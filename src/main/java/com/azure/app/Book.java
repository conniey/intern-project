// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.UUID;

final class Book {
    @JsonProperty("title")
    private String title;
    @JsonProperty("author")
    private Author author;
    @JsonProperty("cover")
    private URI cover;
    @JsonProperty("id")
    private String id;

    Book() {
    }


    Book(String title, Author author, URI cover) {
        this.title = title;
        this.author = author;
        this.cover = cover;
        id = UUID.randomUUID().toString();
    }

    /**
     * Returns the book's title.
     *
     * @return string with book's title
     */
    String getTitle() {
        return title;
    }

    /**
     * Returns the author's name
     *
     * @return Author object
     */
    Author getAuthor() {
        return author;
    }

    /**
     * Returns the File to print out its path
     *
     * @return File of image
     */
    URI getCover() {
        return cover;
    }

    /**
     * Checks to the book's fields to make sure they're correct
     *
     * @return boolean
     * true - if all the fields are valid
     * false - otherwise
     */
    boolean isValid() {
        if (cover == null) {
            return false;
        }
        if (author.getLastName() == null || author.getLastName().isEmpty()
            || author.getFirstName() == null || author.getFirstName().isEmpty()) {
            return false;
        }
        if (title == null || title.isEmpty()) {
            return false;
        }
        return true;
    }


    /**
     * Returns a string with the book's author and title.
     *
     * @return string containing book info
     */
    @Override
    public String toString() {
        return author + " - " + getTitle();
    }

    /**
     * Returns a string with the book information in a different format.
     * Contains the title, author, and file path.
     *
     * @return - String with the book information
     */
    String displayBookInfo(String coverLoc) {
        return "Title: " + getTitle() + "\n"
            + "Author: " + getAuthor() + "\n"
            + "Cover: " + coverLoc + "\n";
    }
}
