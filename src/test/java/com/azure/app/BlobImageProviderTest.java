// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.data.appconfiguration.credentials.ConfigurationClientCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class BlobImageProviderTest {
    private BlobImageProvider blobCollector;
    private static final URL FOLDER = BlobImageProviderTest.class.getClassLoader().getResource(".");

    /**
     * Set up the App Configuration to grab the information for the Blob Storage
     */
    @Before
    public void setup() {
        ObjectMapper mapper = new ObjectMapper();
        String connectionString = System.getenv("AZURE_APPCONFIG");
        if (connectionString == null || connectionString.isEmpty()) {
            System.err.println("Environment variable AZURE_APPCONFIG is not set. Cannot connect to App Configuration."
                + " Please set it.");
            return;
        }
        ConfigurationAsyncClient client;
        try {
            client = new ConfigurationClientBuilder()
                .credential(new ConfigurationClientCredentials(connectionString))
                .httpLogDetailLevel(HttpLogDetailLevel.HEADERS)
                .buildAsyncClient();
            BlobSettings blobSettings = mapper.readValue(Objects.requireNonNull(client.getSetting("BLOB_INFO").block()).value(), BlobSettings.class);
            blobCollector = new BlobImageProvider(blobSettings);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            Assert.fail("");
            LoggerFactory.getLogger(BlobImageProviderTest.class).error("Error in setting up the BlobImageProvider: ", e);
        }
    }

    /**
     * Checks that saving a book works
     */
    @Test
    public void saveImageTest() {
        //Arrange
        Book newBook = new Book("Valid", new Author("Work", "Hard"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        //Act
        StepVerifier.create(blobCollector.saveImage(newBook))
            //Assert
            .verifyComplete();
        //Cleanup
        blobCollector.deleteImage(newBook).block();
    }

    /**
     * Tests that you can edit an image if the user decides to change the book's title
     */
    @Test
    public void editTitleImage() {
        //Arrange
        Book oldBook = new Book("Valid", new Author("Work", "Hard"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        blobCollector.saveImage(oldBook).block();
        Book newBook = new Book("Starships", oldBook.getAuthor(), oldBook.getCover());
        //Act
        StepVerifier.create(blobCollector.editImage(oldBook, newBook, 1)).
            //Assert
                verifyComplete();
        //Double check & Cleanup
        StepVerifier.create(blobCollector.deleteImage(newBook))
            .verifyComplete();
    }

    /**
     * Tests that you can edit an image if the user decided to change the book's author
     */
    @Test
    public void editAuthorImage() {
        //Arrange
        Book oldBook = new Book("Valid", new Author("Work", "Hard"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        blobCollector.saveImage(oldBook).block();
        Book newBook = new Book(oldBook.getTitle(), new Author("Changed", "Person"), oldBook.getCover());
        //Act
        StepVerifier.create(blobCollector.editImage(oldBook, newBook, 1)).
            //Assert
                verifyComplete();
        //Double check & Cleanup
        StepVerifier.create(blobCollector.deleteImage(newBook))
            .verifyComplete();
    }

    /**
     * Tests that you can edit an image if the user decided to change the book's cover image
     */
    @Test
    public void editCoverImage() {
        //Arrange
        Book oldBook = new Book("Valid", new Author("Work", "Hard"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        blobCollector.saveImage(oldBook).block();
        Book newBook = new Book(oldBook.getTitle(), oldBook.getAuthor(), new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        //Act
        StepVerifier.create(blobCollector.editImage(oldBook, newBook, 0)).
            //Assert
                verifyComplete();
        //Double check & Cleanup
        StepVerifier.create(blobCollector.deleteImage(newBook))
            .verifyComplete();
    }

    /**
     * Checks that deleting an image that exists works
     */
    @Test
    public void deleteImageTest() {
        //Arrange
        Book book = new Book("Valid", new Author("Work", "Harder"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        blobCollector.saveImage(book).block();
        //Act
        StepVerifier.create(blobCollector.deleteImage(book))
            //Assert & Cleanup
            .verifyComplete();
    }

    /**
     * Tests that you cannot delete an image that doesn't exist
     */
    @Test
    public void testDeletingNonexistantImage() {
        //Arrange
        Book book = new Book("Completetly Random", new Author("asdfasdf", "qeryuio"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        //Act & Assert
        StepVerifier.create(blobCollector.deleteImage(book)).verifyError();
    }

    /**
     * Tests overwriting image
     */
    @Test
    public void testOverwritingImage() {
        //Arrange
        Book book = new Book("Valid", new Author("Work", "Harder"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        Book book2 = new Book("Valid", new Author("Work", "Harder"),
            new File(FOLDER.getPath() + "Wonder.png").toURI());
        //Act
        StepVerifier.create(blobCollector.saveImage(book)).verifyComplete();
        StepVerifier.create(blobCollector.saveImage(book2)).verifyComplete();
        //Assert
        StepVerifier.create(blobCollector.deleteImage(book2)).verifyComplete();
        StepVerifier.create(blobCollector.deleteImage(book)).verifyError();
    }

    /**
     * Tests saving same image by different title but same author
     */
    @Test
    public void testSavingSameImageDifferentTitle() {
        //Arrange
        Book book = new Book("Miracle", new Author("Work", "Harder"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        Book book2 = new Book("Worker", new Author("Work", "Harder"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        //Act
        StepVerifier.create(blobCollector.saveImage(book)).verifyComplete();
        StepVerifier.create(blobCollector.saveImage(book2)).verifyComplete();
        //Assert
        StepVerifier.create(blobCollector.deleteImage(book2)).verifyComplete();
        StepVerifier.create(blobCollector.deleteImage(book)).verifyComplete();
    }

    /**
     * Tests saving same image by different authors
     */
    @Test
    public void testSavingSameImageDifferentAuthor() {
        //Arrange
        Book book = new Book("Miracle", new Author("Brother", "Harder"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        Book book2 = new Book("Miracle", new Author("Work", "Harder"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        //Act & Assert
        StepVerifier.create(blobCollector.saveImage(book)).verifyComplete();
        StepVerifier.create(blobCollector.saveImage(book2)).verifyComplete();
        //Cleanup & Check
        StepVerifier.create(blobCollector.deleteImage(book2)).verifyComplete();
        StepVerifier.create(blobCollector.deleteImage(book)).verifyComplete();
    }
}
