package com.azure.app;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.credentials.ConfigurationClientCredentials;
import org.junit.Test;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class CosmosTest {

    @Test
    public void test() {
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
            CosmosBookCollector cosmosBookCollector = new CosmosBookCollector(client);
            cosmosBookCollector.saveBook("Wonder", new Author("RJ", "Palacio"),
                new File("C:\\Users\\t-katami\\Documents\\Images\\Book.png").toURI()).block();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}
