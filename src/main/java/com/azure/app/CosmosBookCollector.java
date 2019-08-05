// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import com.azure.data.appconfiguration.models.SettingSelector;
import com.azure.data.cosmos.ConnectionMode;
import com.azure.data.cosmos.ConnectionPolicy;
import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.CosmosContainerProperties;
import com.azure.data.cosmos.CosmosContainerResponse;
import com.azure.data.cosmos.CosmosDatabaseResponse;
import com.azure.data.cosmos.FeedResponse;
import com.azure.data.cosmos.internal.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

public class CosmosBookCollector implements BookCollection {
    private static Logger logger = LoggerFactory.getLogger(CosmosBookCollector.class);
    private Mono<CosmosClient> asyncClient;
    private Mono<CosmosDatabaseResponse> databaseCache;
    private Mono<CosmosContainerResponse> bookCollection;
    private Flux<Book> cosmosBooks;

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
        bookCollection = databaseCache.flatMap(databaseResponse -> {
            Flux<FeedResponse<CosmosContainerProperties>> feedResponseFlux = databaseResponse.database().queryContainers("SELECT * FROM all");
            return feedResponseFlux.count().flatMap(size -> {
                if (size == 0) {
                    return databaseResponse.database().
                        createContainer(collectionId, collectionLink);
                } else {
                    return databaseResponse.database().getContainer(collectionId).read();
                }
            });
        });
        cosmosBooks = Flux.empty();
    }

    @Override
    public Flux<Book> getBooks() {
        initializeBooks().block();
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
     * Initializes the Books by querying every item in the document collection.
     *
     * @return
     */
    private Mono<Void> initializeBooks() {
        // RxJava2Adapter.observableToFlux()
       /* return asyncClient.map(client -> {
            Observable<List<FeedResponse<Document>>> documentList
                = client.queryDocuments(collectionLink, "Select * FROM all", null).toList();
            documentList.toCompletable().await();
            documentList.map(docs -> {
                List<Document> results = docs.get(0).getResults();
                cosmosBooks = Flux.fromIterable(results).map(doc -> Constants.SERIALIZER.fromJSONtoBook(doc.toJson().substring(0,
                    (doc.toJson().indexOf(",\"id\""))) + "}")
                );
                return docs;
            }).toCompletable().await();
            return client;
        }).then();*/
        return null;
    }

    private Mono<Void> saveDocument(Document book) {
        return null;
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
            return null;
        }
        File relativeFile = Paths.get(Constants.IMAGE_PATH, author.getLastName(), author.getFirstName(), titleImage).toFile();
        URI saved = relativeFile.toURI();
        URI relative = new File(System.getProperty("user.dir")).toURI().relativize(saved);
        Book book = new Book(title, author, relative);
        return bookCollection.map(collection ->
            collection.container().createItem(book)
        ).then();
    }

    @Override
    public Mono<Void> editBook(Book oldBook, Book newBook, int saveCover) {
        return null;
    }

    @Override
    public Mono<Void> deleteBook(Book book) {
        return null;
    }

    @Override
    public Flux<Book> findBook(String title) {
        return null;
    }

    @Override
    public Flux<Book> findBook(Author author) {
        return null;
    }

    @Override
    public Mono<Boolean> hasBooks() {
        return null;
    }

    @Override
    public URI retrieveURI(String path) {
        return new File(path).toURI();
    }

    @Override
    public Mono<String> grabCoverImage(Book book) {
        return null;
    }
}
