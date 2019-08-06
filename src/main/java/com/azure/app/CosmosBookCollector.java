// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import com.azure.data.appconfiguration.models.SettingSelector;
import com.azure.data.cosmos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

interface ImageProvider {

    Mono<String> grabCoverImage(Book book);

    /**
     * Saves the book's cover image to a Blob Storage
     *
     * @param imagePath - the File with the cover image
     * @param author    - Author of the Book
     * @param title     - String with the title of the book
     * @return {@Mono Void}
     */
    Mono<Void> saveImage(File imagePath, Author author, String title);

    Mono<Void> deleteImage(Book book);
}


final class CosmosBookCollector implements BookCollection {
    private static Logger logger = LoggerFactory.getLogger(CosmosBookCollector.class);
    private Mono<CosmosClient> asyncClient;
    private Mono<CosmosDatabaseResponse> databaseCache;
    private Mono<CosmosContainerResponse> bookCollection;
    private ImageProvider imageProvider;

    CosmosBookCollector() {
    }

    CosmosBookCollector(ConfigurationAsyncClient client) {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.connectionMode(ConnectionMode.DIRECT);
        List<ConfigurationSetting> infoList = client.listSettings(new SettingSelector().keys("COSMOS*")).collectList().block();
        String endpoint = null;
        String masterKey = null;
        for (int i = 0; i < infoList.size(); i++) {
            String key = infoList.get(i).key();
            if (key.contentEquals("COSMOS_HOST")) {
                endpoint = infoList.get(i).value();
            } else {
                masterKey = infoList.get(i).value();
            }
        }
        imageProvider = new BlobBookCollector(client, 0);
        CosmosClient cosmosClient = CosmosClient.builder()
            .endpoint(endpoint)
            .key(masterKey)
            .connectionPolicy(policy)
            .build();
        String databaseId = "book-inventory";
        String collectionId = "book-info";
        String collectionLink = "/StoredBooks";
        asyncClient = Mono.just(cosmosClient);
        databaseCache = asyncClient.flatMap(asyncClient -> asyncClient.createDatabaseIfNotExists(databaseId));
        bookCollection = databaseCache.flatMap(databaseClient -> databaseClient.database().createContainerIfNotExists(collectionId, collectionLink));
    }


    @Override
    public Flux<Book> getBooks() {
        Flux<Book> cosmosBooks = bookCollection.flatMapMany(items -> {
            Flux<FeedResponse<CosmosItemProperties>> containerItems = items.container().queryItems("SELECT * FROM all",
                new FeedOptions().enableCrossPartitionQuery(true));
            return queryBooks(containerItems);
        });
        return cosmosBooks.sort(this::compare);
    }

    /**
     * Compares the two books for sorting purposes based of
     * last name then first name then title.
     *
     * @param obj1 - Book object
     * @param obj2 - Book object
     * @return integer with the 'smallest' book
     */
    private int compare(Book obj1, Book obj2) {
        String lastName1 = obj1.getAuthor().getLastName();
        String lastName2 = obj2.getAuthor().getLastName();
        int i = lastName1.compareTo(lastName2);
        if (i != 0) {
            return i;
        }
        String firstName1 = obj1.getAuthor().getFirstName();
        String firstName2 = obj2.getAuthor().getFirstName();
        i = firstName1.compareTo(firstName2);
        if (i != 0) {
            return i;
        }
        String title = obj1.getTitle();
        String title2 = obj2.getTitle();
        return title.compareTo(title2);
    }

    @Override
    public Mono<Void> saveBook(String title, Author author, URI path) {
        String extension = path.getPath().substring(path.getPath().lastIndexOf('.'));
        String titleImage;
        try {
            titleImage = URLEncoder.encode(title + extension, StandardCharsets.US_ASCII.toString());
            String apostrophe = URLEncoder.encode("'", StandardCharsets.US_ASCII.toString());
            if (titleImage.contains(apostrophe)) {
                titleImage = titleImage.replace(apostrophe, "'");
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding names: ", e);
            return Mono.error(e);
        }
        File relativeFile = Paths.get(Constants.IMAGE_PATH, author.getLastName(), author.getFirstName(), titleImage).toFile();
        URI saved = relativeFile.toURI();
        URI relative = new File(System.getProperty("user.dir")).toURI().relativize(saved);
        Book book = new Book(title, author, relative);
        return bookCollection.flatMap(collection ->
            collection.container().createItem(book).then()
        ).then(imageProvider.saveImage(new File(path), author, title));
    }

    @Override
    public Mono<Void> editBook(Book oldBook, Book newBook, int saveCover) {
        return null;
    }

    @Override
    public Mono<Void> deleteBook(Book book) {
        CosmosContainer cosmosContainer = bookCollection.map(CosmosContainerResponse::container).block();
        String title = book.getTitle();
        Author author = book.getAuthor();
        FeedResponse<CosmosItemProperties> block = cosmosContainer.queryItems("SELECT * FROM Book b WHERE b.title = \""
                + title + "\" AND b.author.lastName =\"" + author.getLastName() + "\" AND b.author.firstName = \"" + author.getFirstName() + "\"",
            new FeedOptions().enableCrossPartitionQuery(true)).elementAt(0).block();
        cosmosContainer.getItem(block.results().get(0).id(), new PartitionKey("/StoredBooks")).delete().block();
        return bookCollection.flatMap(items -> {
            Flux<FeedResponse<CosmosItemProperties>> containerItems = items.container().queryItems("SELECT * FROM Book b WHERE b.title = \""
                    + title + "\" AND b.author.lastName =\"" + author.getLastName() + "\" AND b.author.firstName = \"" + author.getFirstName() + "\"",
                new FeedOptions().enableCrossPartitionQuery(true));
            return containerItems.elementAt(0).map(id -> {
                Mono<CosmosItemResponse> delete = items.container().getItem(id.results().get(0).id(), "/StoredBooks").delete();
                return items.container().getItem(id.results().get(0).id(), "/StoredBooks").replace(delete);
            });
        }).then(imageProvider.deleteImage(book));
    }

    @Override
    public Flux<Book> findBook(String title) {
        Flux<Book> cosmosBooks = bookCollection.flatMapMany(items -> {
            Flux<FeedResponse<CosmosItemProperties>> containerItems = items.container().queryItems("SELECT * FROM Book b WHERE b.title = \""
                + title + "\"", new FeedOptions().enableCrossPartitionQuery(true));
            return queryBooks(containerItems);
        });
        return cosmosBooks.sort(this::compare);
    }

    @Override
    public Flux<Book> findBook(Author author) {
        Flux<Book> cosmosBooks = bookCollection.flatMapMany(items -> {
            Flux<FeedResponse<CosmosItemProperties>> containerItems = items.container().queryItems("SELECT * FROM Book b WHERE b.author.lastName =\""
                + author.getLastName() + "\" AND b.author.firstName = \"" + author.getFirstName() + "\"", new FeedOptions().enableCrossPartitionQuery(true));
            return queryBooks(containerItems);
        });
        return cosmosBooks.sort(this::compare);
    }

    /**
     * Converts the items in the Cosmos container into Book objects
     *
     * @param containerItems - the query with the Cosmos items under a specific criteria
     * @return
     */
    private Flux<Book> queryBooks(Flux<FeedResponse<CosmosItemProperties>> containerItems) {
        return containerItems.flatMap(item -> {
            List<CosmosItemProperties> list = item.results();
            return Flux.fromIterable(list).map(book -> {
                try {
                    return book.getObject(Book.class);
                } catch (IOException e) {
                    logger.error("Failed to de-serialize: ", e);
                    return null;
                }
            });
        });
    }

    @Override
    public Mono<Boolean> hasBooks() {
        return getBooks().hasElements();
    }

    @Override
    public URI retrieveURI(String path) {
        return new File(path).toURI();
    }

    @Override
    public Mono<String> grabCoverImage(Book book) {
        return imageProvider.grabCoverImage(book);
    }
}
