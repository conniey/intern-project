// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.credentials.ConfigurationClientCredentials;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.io.File;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class BlobImageProviderTest {
    private BlobImageProvider blobCollector;
    URL folder = BlobImageProviderTest.class.getClassLoader().getResource(".");

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
            blobCollector = new BlobImageProvider(client);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Assert.fail("");
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
}
