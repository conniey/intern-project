// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CosmosBookCollectorTest {
    private CosmosDocumentProvider cosmosBC;
    private static URL folder;
    private static KeyVaultForTests keyVault = new KeyVaultForTests();


    /**
     * Sets up App Configuration to get the information needed for Cosmos.
     */
    @Before
    public void setup() {
        CosmosSettings cosmosSettings = keyVault.getCosmosInformation().block();
        assertNotNull(cosmosSettings);
        cosmosBC = new CosmosDocumentProvider(cosmosSettings);
        folder = CosmosBookCollectorTest.class.getClassLoader().getResource(".");
        assertNotNull(folder);
    }

    /**
     * Tests to see that a book can be saved to Cosmos as a JSON file.
     */
    @Test
    public void testSaveBook() {
        Book book = new Book("Valid", new Author("Work", "Hard"),
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        StepVerifier.create(cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()))
            .expectComplete()
            .verify();
        cosmosBC.deleteBook(book).block();
    }

    /**
     * Tests deletion.
     */
    @Test
    public void testDeleteBook() {
        Book book = new Book("Once", new Author("Work", "Hard"),
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()).block();
        cosmosBC.deleteBook(book).block();
    }

    /**
     * Tests find
     */
    @Test
    public void testFindTitle() {
        //Arrange
        String title = "ASD0a3FHJKL";
        Book book = new Book(title, new Author("Crazy", "Writer"), new File(folder.getPath(), "GreatGatsby.gif").toURI());
        cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()).block();
        //Act
        Flux<Book> length = cosmosBC.findBook(title);
        //Assert
        StepVerifier.create(length)
            .assertNext(expected -> assertEquals(expected.getTitle(), title))
            .verifyComplete();
        //Cleanup!
        cosmosBC.deleteBook(book).block();
    }

    /**
     * Tests find
     */
    @Test
    public void testFindNoTitle() {
        //Arrange
        Book book = new Book("Utterly Ridicious", new Author("IMPOssibleToHaveYOu", "Yep"),
            new File(folder.getPath(), "GreatGatsby.gif").toURI());
        //Act
        int length = cosmosBC.findBook(book.getTitle()).count().block().intValue();
        //Assert
        assertEquals(0, length);
    }

    /**
     * Test find author when there's at least one
     */
    @Test
    public void testFindAuthor() {
        //Arrange
        Author author = new Author("HAJKSDFAard", "Kadmklasnock");
        Book book = new Book("abdeidoapsd", author, new File("Rip.png").toURI());
        cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()).block();
        //Act
        Flux<Book> foundBook = cosmosBC.findBook(author);
        //Assert
        StepVerifier.create(foundBook)
            .assertNext(bookCopy -> {
                assertEquals(author.getLastName(), bookCopy.getAuthor().getLastName());
                assertEquals(author.getFirstName(), bookCopy.getAuthor().getFirstName());
            }).verifyComplete();
        cosmosBC.deleteBook(book).block();
    }

    /**
     * Tests find
     */
    @Test
    public void testFindNoAuthor() {
        //Arrange
        Book book = new Book("Utterly Ridicious", new Author("IMPOssibleToHaveYOu", "Yep"),
            new File(folder.getPath(), "GreatGatsby.gif").toURI());
        //Act
        int length = cosmosBC.findBook(book.getAuthor()).count().block().intValue();
        //Assert
        Assert.assertTrue(length == 0);
    }

    /**
     * Tests saving two different books by the same author AND with the same cover
     */
    @Test
    public void testSavingDifferentBooksWithSameCover() {
        //Arrange
        boolean result;
        boolean result2;
        Book book1 = new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Wonder.png").toURI());
        Book book2 = new Book("Giant Peach_The Return", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Wonder.png").toURI());
        //Act & Assert
        StepVerifier.create(cosmosBC.saveBook("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Wonder.png").toURI())).expectComplete().verify();
        StepVerifier.create(cosmosBC.saveBook("Giant Peach_The Return", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Wonder.png").toURI())).expectComplete().verify();
        //Cleanup
        cosmosBC.deleteBook(book1).block();
        cosmosBC.deleteBook(book2).block();
    }

    /**
     * Tests overwriting the same book but with a different cover image
     */
    @Test
    public void testOverwritingBook() {
        //Arrange
        Book book1 = new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Wonder.png").toURI());
        Book book2 = new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Gingerbread.jpg").toURI());
        //Act & Assert
        StepVerifier.create(cosmosBC.saveBook("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File("Wonder.png").toURI())).expectComplete().verify();
        StepVerifier.create(cosmosBC.saveBook("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(folder.getPath(), "Gingerbread.jpg").toURI())).expectComplete().verify();
        //Cleanup
        StepVerifier.create(cosmosBC.deleteBook(book1)).verifyComplete();
        StepVerifier.create(cosmosBC.deleteBook(book2)).verifyError();
    }
}
