// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Before;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertNotNull;

public class BlobImageProviderTest {
    private BlobImageProvider blobCollector;
    private static URL folder;

    /**
     * Set up the App Configuration to grab the information for the Blob Storage
     */
    @Before
    public void setup() {
        KeyVaultStorage keyVaultStorage = new KeyVaultStorage();
        BlobSettings data = keyVaultStorage.getBlobInformation().block();
        assertNotNull(data);
        blobCollector = new BlobImageProvider(data);
        folder = BlobImageProviderTest.class.getClassLoader().getResource(".");
        assertNotNull(folder);
    }

    /**
     * Verifies that you can save an image.
     */
    @Test
    public void saveImageTest() {
        //Arrange
        Book newBook = new Book("Valid", new Author("Work", "Hard"),
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
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
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        blobCollector.saveImage(oldBook).block();
        Book newBook = new Book("Starships", oldBook.getAuthor(), oldBook.getCover());
        //Act
        StepVerifier.create(blobCollector.editImage(oldBook, newBook, true)).
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
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        blobCollector.saveImage(oldBook).block();
        Book newBook = new Book(oldBook.getTitle(), new Author("Changed", "Person"), oldBook.getCover());
        //Act
        StepVerifier.create(blobCollector.editImage(oldBook, newBook, true)).
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
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        blobCollector.saveImage(oldBook).block();
        Book newBook = new Book(oldBook.getTitle(), oldBook.getAuthor(), new File(folder.getPath() + "GreatGatsby.gif").toURI());
        //Act
        StepVerifier.create(blobCollector.editImage(oldBook, newBook, false)).
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
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
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
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
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
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        Book book2 = new Book("Valid", new Author("Work", "Harder"),
            new File(folder.getPath() + "Wonder.png").toURI());
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
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        Book book2 = new Book("Worker", new Author("Work", "Harder"),
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
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
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        Book book2 = new Book("Miracle", new Author("Work", "Harder"),
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        //Act & Assert
        StepVerifier.create(blobCollector.saveImage(book)).verifyComplete();
        StepVerifier.create(blobCollector.saveImage(book2)).verifyComplete();
        //Cleanup & Check
        StepVerifier.create(blobCollector.deleteImage(book2)).verifyComplete();
        StepVerifier.create(blobCollector.deleteImage(book)).verifyComplete();
    }
}
