// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlockBlobClient;
import com.azure.storage.blob.ContainerClient;
import com.azure.storage.blob.StorageClient;
import com.azure.storage.common.credentials.SASTokenCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;

public class BlobBookCollector implements BookCollection {
    private StorageClient storageClient;
    private ContainerClient containerClient;
    private BlobClient blobClient;
    private static Logger logger = LoggerFactory.getLogger(JsonHandler.class);


    BlobBookCollector() {
        storageClient = StorageClient.storageClientBuilder()
            .endpoint(System.getenv("BLOB_URL"))
            .credential(SASTokenCredential.fromQuery(System.getenv("SAS_TOKEN")))
            .buildClient();
        containerClient = storageClient.getContainerClient("books");
        blobClient = containerClient.getBlobClient("blob");
    }

    /**
     * Returns the Flux of Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    @Override
    public Flux<Book> getBooks() {
        return Flux.empty();
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
    public Mono<Boolean> saveBook(String title, Author author, URI path) {
        BlockBlobClient blockBlobClient = containerClient.getBlockBlobClient("blob");
        try {
            blockBlobClient.uploadFromFile("C:\\Users\\t-katami\\Documents\\intern-project\\lib\\jsonFiles\\Pearson\\Ridley\\Kingdom Keeper.json");
        } catch (IOException e) {
            logger.error("Exception uploading file to blob storage: ", e);
        }
        return Mono.just(false);
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
    public Mono<Boolean> deleteBook(Book book) {
        return null;
    }

    /**
     * Filters out the book based on the specified title.
     *
     * @param title - String of the book title the user is looking for
     * @return - Flux of Book objects with that title
     */
    @Override
    public Flux<Book> findBook(String title) {
        return null;
    }

    /**
     * Filters out the books based on the specified author.
     *
     * @param author - Contains the name of the author the user is looking for
     * @return - Flux of Book objects by that author
     */
    @Override
    public Flux<Book> findBook(Author author) {
        return null;
    }

    /**
     * Determines whether the collection has books or not.
     *
     * @return {@Link Mono<Boolean>} - true if there are books
     * false - if there are no books
     */
    @Override
    public Mono<Boolean> hasBooks() {
        return null;
    }

    /**
     * Converts the path to a file and then returns the URI to that path
     *
     * @param path - String containing the image file the user entered
     * @return URI of the image path
     */
    @Override
    public URI retrieveURI(String path) {
        return null;
    }
}
