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
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class CosmosBookCollectorTest {
    private CosmosDocumentProvider cosmosBC;
    private static final URL FOLDER = CosmosBookCollectorTest.class.getClassLoader().getResource(".");

    /**
     * Sets up App Configuration to get the information needed for Cosmos.
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
            CosmosSettings cosmosSettings = mapper.readValue(client.getSetting("COSMOS_INFO").block().value(), CosmosSettings.class);
            cosmosBC = new CosmosDocumentProvider(cosmosSettings);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            LoggerFactory.getLogger(BlobImageProviderTest.class).error("Error in setting up the CosmosBookCollector: ", e);
        }
    }

    /**
     * Tests to see that a book can be saved to Cosmos as a JSON file.
     */
    @Test
    public void testSaveBook() {
        Book book = new Book("Valid", new Author("Work", "Hard"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        StepVerifier.create(cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()))
            .expectComplete()
            .verify();
    }

    /**
     * Tests deletion.
     */
    @Ignore
    @Test
    public void testDeleteBook() {
        Book book = new Book("Once", new Author("Work", "Hard"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
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
        Book book = new Book(title, new Author("Crazy", "Writer"), new File(FOLDER.getPath(), "GreatGatsby.gif").toURI());
        cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()).block();
        //Act
        Flux<Book> length = cosmosBC.findBook(title);
        //Assert
        StepVerifier.create(length)
            .assertNext(expected -> assertEquals(expected.getTitle(), title))
            .verifyComplete();
        //Cleanup...eventally....
    }

    /**
     * Tests find
     */
    @Test
    public void testFindNoTitle() {
        //Arrange
        Book book = new Book("Utterly Ridicious", new Author("IMPOssibleToHaveYOu", "Yep"),
            new File(FOLDER.getPath(), "GreatGatsby.gif").toURI());
        //Act
        int length = cosmosBC.findBook(book.getTitle()).count().block().intValue();
        //Assert
        Assert.assertTrue(length == 0);
    }

    /**
     * Test find
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
        //Since delete doesn't work, you'll have to manually delete the item from your Cosmos storage
    }

    /**
     * Tests find
     */
    @Test
    public void testFindNoAuthor() {
        //Arrange
        Book book = new Book("Utterly Ridicious", new Author("IMPOssibleToHaveYOu", "Yep"),
            new File(FOLDER.getPath(), "GreatGatsby.gif").toURI());
        //Act
        int length = cosmosBC.findBook(book.getAuthor()).count().block().intValue();
        //Assert
        Assert.assertTrue(length == 0);
    }
}
