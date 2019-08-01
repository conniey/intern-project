// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.models.SettingSelector;
import com.microsoft.azure.cosmosdb.ConnectionMode;
import com.microsoft.azure.cosmosdb.ConnectionPolicy;
import com.microsoft.azure.cosmosdb.ConsistencyLevel;
import com.microsoft.azure.cosmosdb.Database;
import com.microsoft.azure.cosmosdb.Document;
import com.microsoft.azure.cosmosdb.DocumentCollection;
import com.microsoft.azure.cosmosdb.FeedResponse;
import com.microsoft.azure.cosmosdb.ResourceResponse;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

public class CosmosBookCollector implements BookCollection {
    private static Logger logger = LoggerFactory.getLogger(CosmosBookCollector.class);
    private Mono<AsyncDocumentClient> asyncClient;
    private Observable<Database> databaseCache;
    private Observable<DocumentCollection> bookCollection;
    private String collectionLink;
    private Flux<Book> cosmosBooks;

    CosmosBookCollector(ConfigurationAsyncClient client) {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setConnectionMode(ConnectionMode.Direct);
        asyncClient = client.listSettings(new SettingSelector().keys("COSMOS*")).collectList().map(list -> {
            String endpoint = null;
            String masterKey = null;
            for (int i = 0; i < list.size(); i++) {
                String key = list.get(i).key();
                if (key.contentEquals("COSMOS_HOST")) {
                    endpoint = list.get(i).value();
                } else {
                    masterKey = list.get(i).value();
                }
            }
            AsyncDocumentClient asyncClient = new AsyncDocumentClient.Builder()
                .withServiceEndpoint(endpoint)
                .withMasterKeyOrResourceToken(masterKey)
                .withConnectionPolicy(policy)
                .withConsistencyLevel(ConsistencyLevel.Eventual)
                .build();
            Database databaseDefinition = new Database();
            String databaseID = "book-inventory";
            databaseDefinition.setId(databaseID);
            DocumentCollection collection = new DocumentCollection();
            String collectionID = "book-info";
            collection.setId(collectionID);
            Observable<List<FeedResponse<Database>>> listObservable = asyncClient.queryDatabases("SELECT * FROM root r WHERE r.id='" + databaseID
                + "'", null).toList();
            databaseCache = listObservable.flatMap(databases -> Observable.just(databases.get(0).getResults().get(0)))
                .onErrorResumeNext(asyncClient.createDatabase(databaseDefinition, null)
                    .map(ResourceResponse::getResource)).cache();
            bookCollection = databaseCache.flatMap(datbaseLink -> {
                Observable<List<FeedResponse<DocumentCollection>>> collections = asyncClient.queryCollections(datbaseLink.getSelfLink(), "SELECT * FROM root r where r.id = '" + collectionID + "'", null).toList();
                return collections.flatMap(colList -> Observable.just(colList.get(0).getResults().get(0)))
                    .onErrorResumeNext(asyncClient.createCollection(datbaseLink.getSelfLink(), collection, null)
                        .map(item -> item.getResource()));
            });
            collectionLink = String.format("/dbs/%s/colls/%s", databaseID, collectionID);
            cosmosBooks = Flux.empty();
            return asyncClient;
        });
    }

    @Override
    public Flux<Book> getBooks() {
        initializeBooks().block();
        return cosmosBooks.sort(this::compare);
    }

    public int compare(Book obj1, Book obj2) {
        String lastName1 = obj1.getAuthor().getLastName();
        String lastName2 = obj2.getAuthor().getLastName();
        int i = lastName1.compareTo(lastName2);
        if (i != 0) return i;
        String firstName1 = obj1.getAuthor().getFirstName();
        String firstName2 = obj2.getAuthor().getFirstName();
        i = firstName1.compareTo(firstName2);
        if (i != 0) return i;
        String title = obj1.getTitle();
        String title2 = obj2.getTitle();
        return title.compareTo(title2);
    }

    public Mono<Void> initializeBooks() {
        return asyncClient.map(client -> {
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
        }).then();
    }

    private Mono<Void> saveDocument(Document book) {
        Mono<Observable<ResourceResponse<Document>>> doc = asyncClient.map(client -> bookCollection.flatMap(x -> client.createDocument(x.getSelfLink(), book, null, false)));
        return doc.map(complete -> {
            complete.toCompletable().await();
            return complete;
        }).then();
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
        Document bookDoc = new Document(Constants.SERIALIZER.toJson(book));
        bookDoc.set("id", generateID(title));
        return saveDocument(bookDoc);
    }

    private String generateID(String info) {
        String id;
        try {
            id = URLEncoder.encode(info, StandardCharsets.US_ASCII.toString());
            String apostrophe = URLEncoder.encode("'", StandardCharsets.US_ASCII.toString());
            if (info.contains(apostrophe)) {
                id = id.replace(apostrophe, "'");
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding names: ", e);
            return null;
        }
        return id;
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
