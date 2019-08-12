package com.azure.app;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.data.appconfiguration.credentials.ConfigurationClientCredentials;
import com.azure.data.cosmos.ConnectionMode;
import com.azure.data.cosmos.ConnectionPolicy;
import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.CosmosContainer;
import com.azure.data.cosmos.CosmosContainerProperties;
import com.azure.data.cosmos.CosmosContainerResponse;
import com.azure.data.cosmos.CosmosDatabase;
import com.azure.data.cosmos.CosmosItem;
import com.azure.data.cosmos.CosmosItemRequestOptions;
import com.azure.data.cosmos.CosmosItemResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SampleDelete {

    public static void main(String[] args) throws IOException {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.connectionMode(ConnectionMode.DIRECT);
        ConfigurationAsyncClient client;
        final ObjectMapper mapper2 = new ObjectMapper();
        String connectionString = System.getenv("AZURE_APPCONFIG");
        try {
            client = new ConfigurationClientBuilder()
                .credential(new ConfigurationClientCredentials(connectionString))
                .httpLogDetailLevel(HttpLogDetailLevel.HEADERS)
                .buildAsyncClient();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            return;
        }
        CosmosSettings cosmosSettings = client.getSetting("COSMOS_INFO").map(info -> {
            try {
                return (mapper2.readValue(info.value(), CosmosSettings.class));
            } catch (IOException e) {
                return null;
            }
        }).block();
        CosmosClient cosmosClient = CosmosClient.builder()
            .endpoint(cosmosSettings.host())
            .key(cosmosSettings.key())
            .connectionPolicy(policy)
            .build();
        final Mono<CosmosContainer> containerMono = cosmosClient.createDatabaseIfNotExists("ToDoList")
            .flatMap(response -> {
                final CosmosDatabase database = response.database();
                final CosmosContainerProperties properties = new CosmosContainerProperties("bookRus", "/id");
                return database.createContainerIfNotExists(properties).map(CosmosContainerResponse::container);
            }).cache();
        final Book book = new Book("Happily Ever After", new Author("Sammi", "Smithers"), new File("cover.jpg").toURI());
        final CosmosItemResponse cosmosItem = containerMono.flatMap(container -> {
            System.out.println("Creating book...");
            return container.createItem(book);
        }).flatMap(response -> {
            System.out.println("Reading book...");
            final CosmosItem item = response.item();
            return item.read(new CosmosItemRequestOptions(book.id()));
        }).block();
        if (cosmosItem == null) {
            throw new IllegalStateException("Should have fetched cosmosItem.");
        }
        final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final Book deserialized = mapper.readValue(cosmosItem.properties().toJson(), Book.class);
        System.out.printf("Book Id: %s, Title: %s%n", deserialized.id(), deserialized.title());
        cosmosItem.item().delete().block();
        System.out.println("Finished.");
    }
}
