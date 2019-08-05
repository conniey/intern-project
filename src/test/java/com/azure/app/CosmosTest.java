// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.credentials.ConfigurationClientCredentials;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.io.File;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class CosmosTest {
    private CosmosBookCollector cosmosBC;
    URL folder = LocalBookCollectorTest.class.getClassLoader().getResource(".");

    /**
     * Sets up App Configuration to get the information needed for Cosmos.
     */
    @Before
    public void setup() {
        String connectionString = System.getenv("AZURE_APPCONFIG");
        if (connectionString == null || connectionString.isEmpty()) {
            System.err.println("Environment variable AZURE_APPCONFIG is not set. Cannot connect to App Configuration."
                + " Please set it.");
            return;
        }
        UUID.randomUUID().toString();
        ConfigurationAsyncClient client;
        try {
            client = ConfigurationAsyncClient.builder()
                .credentials(new ConfigurationClientCredentials(connectionString))
                .httpLogDetailLevel(HttpLogDetailLevel.HEADERS)
                .build();
            cosmosBC = new CosmosBookCollector(client);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Assert.fail("");
        }
    }

    /**
     * Tests to see that a book can be saved to Cosmos as a JSON file.
     */
    @Test
    public void testSaveBook() {
        Book book = new Book("Once", new Author("Work", "Hard"),
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()).block();
        //Todo: Cleanup when you figure out how to delete
    }

    /**
     * Tests the getBook method
     */
    @Ignore
    @Test
    public void test() {
        Flux<Book> books = cosmosBC.getBooks();
        books.collectList().map(list -> {
            for (int i = 0; i < list.size(); i++) {
                System.out.println(list.get(i));
            }
            return list;
        }).block();
    }
}
