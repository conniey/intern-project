package com.azure.app;

import com.azure.data.cosmos.CosmosClient;
import com.azure.data.cosmos.CosmosContainer;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class DeleteWork {

    public static void main(String[] args) {
        CosmosClient client = CosmosClient.builder()
            .endpoint("https://book221.documents.azure.com:443/")
            .key("QkO8k11euctLAYJAhoD1pvvjgSj61ACumvROOXcKGyTFAl8b0kkZI4AZYazSkJYoCdf38AJfRoHZ6hOIJOhvwg==")
            .build();
        CosmosContainer container = client.createDatabaseIfNotExists("book-inventory")
            .flatMap(response -> response.database()
                .createContainerIfNotExists("book-infoz", "/id"))
            .map(response -> response.container())
            .block();
        Book book = new Book("Once", new Author("Work", "Hard"),
            "GreatGatsby.gif");
        // Create an item
        assert container != null;
        container.createItem(book)
            .flatMap(response -> {
                System.out.println("Created item: " + response.properties().toJson());
                // Read that item
                return response.item().read();
                // Delete that item
            }).flatMap(response -> {
            System.out.println(response.item().id()); //FAILS HERE
            return response.item().delete();
        }).block(); //FAILS TO DELETE
    }

    static class Book {
        @JsonProperty("title")
        private String title;
        @JsonProperty("author")
        private Author author;
        @JsonProperty("cover")
        private String cover;
        @JsonProperty("id")
        private String id;

        Book() {
        }

        Book(String title, Author author, String cover) {
            this.title = title;
            this.author = author;
            this.cover = cover;
            id = UUID.randomUUID().toString();
        }
    }

    static class Author {
        @JsonProperty("lastName")
        private String lastName;
        @JsonProperty("firstName")
        private String firstName;

        Author() {

        }

        Author(String firstName, String lastName) {
            this.lastName = lastName;
            this.firstName = firstName;
        }
    }
}
