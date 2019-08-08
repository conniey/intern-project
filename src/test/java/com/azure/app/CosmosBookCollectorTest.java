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

public class CosmosBookCollectorTest {
    private BookCollector cosmosBC;
    URL folder = CosmosBookCollectorTest.class.getClassLoader().getResource(".");

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
            BlobSettings blobSettings = mapper.readValue(client.getSetting("BLOB_INFO").block().value(), BlobSettings.class);
            cosmosBC = new BookCollector(new CosmosDocumentProvider(cosmosSettings), new BlobImageProvider(blobSettings));
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            LoggerFactory.getLogger(BlobImageProviderTest.class).error("Error in setting up the CosmosBookCollector: ", e);
        }
    }

    /**
     * Tests to see that a book can be saved to Cosmos as a JSON file.
     */
    @Ignore
    @Test
    public void testSaveBook() {
        Book book = new Book("Valid", new Author("Work", "Hard"),
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        StepVerifier.create(cosmosBC.saveBook(book))
            .expectComplete()
            .verify();
        //Todo: Cleanup when you figure out how to delete
    }

    /**
     * Tests the getBook method
     */
    @Ignore
    @Test
    public void testGetBook() {
        Flux<Book> books = cosmosBC.getBooks();
        books.collectList().map(list -> {
            for (int i = 0; i < list.size(); i++) {
                System.out.println(list.get(i));
            }
            return list;
        }).block();
    }

    /**
     * Tests deletion.
     */
    @Ignore
    @Test
    public void testDeleteBook() {
        Book book = new Book("Once", new Author("Work", "Hard"),
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        cosmosBC.saveBook(book).block();
        cosmosBC.deleteBook(book).block();
    }

    /**
     * Tests find
     */
    @Ignore
    @Test
    public void testFindTitle() {
        //Arrange
        Book book = new Book("ASD0a3FHJKL", new Author("Crazy", "Writer"), new File(folder.getPath(), "GreatGatsby.gif").toURI());
        int formerLength = cosmosBC.findBook(book.getTitle()).count().block().intValue();
        cosmosBC.saveBook(book).block();
        //Act
        int length = cosmosBC.findBook(book.getTitle()).count().block().intValue();
        //Assert
        Assert.assertTrue(formerLength + 1 == length);
        //Cleanup
    }

    /**
     * Tests find
     */
    @Test
    @Ignore
    public void testFindNoTitle() {
        //Arrange
        Book book = new Book("Utterly Ridicious", new Author("IMPOssibleToHaveYOu", "Yep"),
            new File(folder.getPath(), "GreatGatsby.gif").toURI());
        //Act
        int length = cosmosBC.findBook(book.getTitle()).count().block().intValue();
        //Assert
        Assert.assertTrue(length == 0);
    }
}
