// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.data.cosmos.ConnectionMode;
import com.azure.data.cosmos.ConnectionPolicy;
import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.CosmosContainer;
import com.azure.data.cosmos.CosmosContainerResponse;
import com.azure.data.cosmos.CosmosItemProperties;
import com.azure.data.cosmos.CosmosItemResponse;
import com.azure.data.cosmos.FeedOptions;
import com.azure.data.cosmos.FeedResponse;
import com.azure.data.cosmos.PartitionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
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

final class CosmosDocumentProvider implements DocumentProvider {
    private static Logger logger = LoggerFactory.getLogger(CosmosDocumentProvider.class);
    private Mono<CosmosContainerResponse> bookCollection;

    CosmosDocumentProvider(CosmosSettings cosmosSettings) {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.connectionMode(ConnectionMode.DIRECT);
        CosmosClient cosmosClient = CosmosClient.builder()
            .endpoint(cosmosSettings.host())
            .key(cosmosSettings.key())
            .connectionPolicy(policy)
            .build();
        String databaseId = "book-inventory";
        String collectionId = "book-info";
        String collectionLink = "/StoredBooks";
        bookCollection = cosmosClient.createDatabaseIfNotExists(databaseId).flatMap(databaseClient -> databaseClient.database().createContainerIfNotExists(collectionId, collectionLink));
    }

    /**
     * Returns the Flux of Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
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

    /**
     * Saves the book as a JSON file
     *
     * @param title  - String containing the title of the book
     * @param author - Author object of the book
     * @param path   - File containing the cover image of the book
     * @return Mono<Boolean> that determines whether the book got saved or not
     * true - book was successfully saved
     * false - book wasn't saved </Boolean>
     */
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
        ).then();
    }

    /**
     * Overwrites the old book with the contents in the new book
     *
     * @param oldBook   - Book object that will be changed
     * @param newBook   - Book object with the new information to change to
     * @param saveCover - determines whether or not the user wants to keep the same cover
     * @return {@Link Mono}
     */
    @Override
    public Mono<Void> editBook(Book oldBook, Book newBook, int saveCover) {
        if (saveCover == 1) {
            return deleteBook(oldBook).then(saveBook(newBook.getTitle(), newBook.getAuthor(),
                newBook.getCover()));
        } else {
            return deleteBook(oldBook).then(saveBook(newBook.getTitle(), newBook.getAuthor(),
                newBook.getCover()));
        }
    }

    /**
     * Deletes the book and the file based off its information.
     *
     * @param book - Book that'll be deleted
     * @return Mono<Boolean> determines whether or not book was successfully deleted </Boolean>
     * true - Book was deleted
     * false - Book wasn't deleted
     */
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
        }).then();
    }

    /**
     * Filters out the book based on the specified title.
     *
     * @param title - String of the book title the user is looking for
     * @return - Flux of Book objects with that title
     */
    @Override
    public Flux<Book> findBook(String title) {
        Flux<Book> cosmosBooks = bookCollection.flatMapMany(items -> {
            Flux<FeedResponse<CosmosItemProperties>> containerItems = items.container().queryItems("SELECT * FROM Book b WHERE b.title = \""
                + title + "\"", new FeedOptions().enableCrossPartitionQuery(true));
            return queryBooks(containerItems);
        });
        return cosmosBooks.sort(this::compare);
    }

    /**
     * Filters out the books based on the specified author.
     *
     * @param author - Contains the name of the author the user is looking for
     * @return - Flux of Book objects by that author
     */
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
                    throw Exceptions.propagate(e);
                }
            });
        });
    }
}
