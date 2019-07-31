// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.credentials.ConfigurationClientCredentials;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.File;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class BlobBookCollectorTest {
    private BlobBookCollector blobCollector;
    URL folder = LocalBookCollectorTest.class.getClassLoader().getResource(".");

    /**
     * Set up the App Configuration to grab the information for the Blob Storage
     */
    @Before
    public void setup() {
        String connectionString = System.getenv("AZURE_APPCONFIG");
        if (connectionString == null || connectionString.isEmpty()) {
            System.err.println("Environment variable AZURE_APPCONFIG is not set. Cannot connect to App Configuration."
                + " Please set it.");
            return;
        }
        ConfigurationAsyncClient client;
        try {
            client = ConfigurationAsyncClient.builder()
                .credentials(new ConfigurationClientCredentials(connectionString))
                .httpLogDetailLevel(HttpLogDetailLevel.HEADERS)
                .build();
            blobCollector = new BlobBookCollector(client);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Assert.fail("");
        }
    }

    /**
     * Checks that saving a book works
     */
    @Test
    public void saveBookTest() {
        //Arrange
        int length = blobCollector.getBooks().count().block().intValue();
        Book newBook = new Book("Valid", new Author("Work", "Hard"),
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        //Act
        blobCollector.saveBook(newBook.getTitle(), newBook.getAuthor(), newBook.getCover()).block();
        //Assert
        Assert.assertTrue(length < blobCollector.getBooks().count().block());
        //Cleanup
        blobCollector.deleteBook(newBook).block();
    }

    /**
     * Verifies finding a book, where at least one exists
     */
    @Test
    public void findBookTest() {
        //Arrange
        Book book = new Book("ASD0a3FHJKL", new Author("Crazy", "Writer"), new File(folder.getPath(), "GreatGatsby.gif").toURI());
        int formerLength = blobCollector.findBook(book.getTitle()).count().block().intValue();
        blobCollector.saveBook(book.getTitle(), book.getAuthor(), book.getCover()).block();
        //Act
        Flux<Book> booksFound = blobCollector.findBook(book.getTitle());
        //Assert
        Assert.assertTrue(formerLength + 1 == booksFound.count().block());
        //Cleanup
        blobCollector.deleteBook(book).block();
    }

    /**
     * Verifies finding a book that doesn't exist won't appear
     */
    @Test
    public void findNoBookTest() {
        //Arrange
        Book book = new Book("aloeiur128c,1ds", new Author("Kid", "Preacher"), new File(folder.getPath(), "GreatGatsby.gif").toURI());
        //Act
        StepVerifier.create(blobCollector.findBook(book.getTitle()))
            //Assert
            .expectComplete()
            .verify();
    }

    /**
     * Verifies finding a book if there's at least by that author
     */
    @Test
    public void findAuthorTest() {
        //Arrange
        Book book = new Book("ASD0a3FHJKL", new Author("Crazy", "Writer"), new File(folder.getPath(), "GreatGatsby.gif").toURI());
        int formerLength = blobCollector.findBook(book.getTitle()).count().block().intValue();
        blobCollector.saveBook(book.getTitle(), book.getAuthor(), book.getCover()).block();
        //Act
        int length = blobCollector.findBook(book.getTitle()).count().block().intValue();
        //Assert
        Assert.assertTrue(formerLength + 1 == length);
        //Cleanup
        blobCollector.deleteBook(book).block();

    }

    /**
     * Verifies not finding a book when there's none written by that author
     */
    @Test
    public void findNoAuthor() {
        //Arrange
        Book book = new Book("aloeiur128c,1ds", new Author("Kid", "Preacher"), new File(folder.getPath(), "GreatGatsby.gif").toURI());
        //Act
        StepVerifier.create(blobCollector.findBook(book.getAuthor()))
            //Assert
            .expectComplete()
            .verify();
    }

    /**
     * Tests saving a two different books by the same author AND with the same cover
     */
    @Test
    public void testSavingDifferentBooksWithSameCover() {
        //Arrange
        Book book1 = new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Wonder.png").toURI());
        Book book2 = new Book("Giant Peach_The Return", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Wonder.png").toURI());
        //Act
        StepVerifier.create(blobCollector.saveBook("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Wonder.png").toURI())).expectComplete().verify();
        StepVerifier.create(blobCollector.saveBook("Giant Peach_The Return", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Wonder.png").toURI())).expectComplete().verify();
        //Assert and Cleanup
        StepVerifier.create(blobCollector.deleteBook(book1))
            .expectComplete() //if not created, will return error
            .verify();
        StepVerifier.create(blobCollector.deleteBook(book2))
            .expectComplete()
            .verify();
    }

    /**
     * Tests overwriting the same book but with a different cover image
     */
    @Test
    public void testOverwritingBook() {
        //Arrange
        Book bookToDelete = new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Gingerbread.jpg").toURI());
        int formerLength;
        //Act & Assert
        StepVerifier.create(blobCollector.saveBook("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Wonder.png").toURI())).expectComplete().verify();
        formerLength = blobCollector.getBooks().count().block().intValue();
        StepVerifier.create(blobCollector.saveBook("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Gingerbread.jpg").toURI())).expectComplete().verify();
        //Check that the size is the same as before they added the second book
        Assert.assertEquals(formerLength, blobCollector.getBooks().count().block().intValue());
        //Cleanup
        StepVerifier.create(blobCollector.deleteBook(bookToDelete))
            .expectComplete() //should delete the book fine
            .verify();
    }
}
