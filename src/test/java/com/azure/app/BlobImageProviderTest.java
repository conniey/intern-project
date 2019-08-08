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
    URL folder = BlobImageProviderTest.class.getClassLoader().getResource(".");

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
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        //Act
        StepVerifier.create(blobCollector.saveImage(newBook))
            //Assert
            .verifyComplete();
        //Cleanup
        blobCollector.deleteImage(newBook).block();
    }

    /**
     * Checks that deleting a book works
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
}

