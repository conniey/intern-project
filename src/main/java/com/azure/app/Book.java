// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;

public class Book {
    @JsonProperty("title")
    private String title;
    @JsonProperty("author")
    private Author author;
    @JsonProperty("cover")
    private File cover;

    Book() {
    }

    Book(String title, Author author, File cover) {
        this.title = title;
        this.author = author;
        this.cover = cover;
    }

    /**
     * Returns the book's title.
     *
     * @return string with book's title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the author's name
     *
     * @return Author object
     */
    public Author getAuthor() {
        return author;
    }

    /**
     * Returns the File to print out its path
     *
     * @return File of image
     */
    public File getCover() {
        return cover;
    }

    /**
     * Checks to the book's fields to make sure they're correct
     *
     * @return boolean - true if all the fields are valid
     * - false otherwise
     */
    public boolean checkBook() {
        if (cover == null || !new OptionChecker().checkImage(cover)) {
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

    private void setTitle(String title) {
        this.title = title;
    }

    private void setAuthor(Author author) {
        this.author = author;
    }

    private void setImage(File path) {
        cover = path;
    }

    /**
     * Returns a string with the book's author and title.
     *
     * @return string containing book info
     */
    @Override
    public String toString() {
        return author + " -" + " " + getTitle();
    }

    /**
     * Returns a string with the book information in a different format.
     * Contains the title, author, and file path.
     *
     * @return - String with the book information
     */
    public String displayBookInfo() {
        return "Title: " + getTitle() + "\n"
            + "Author: " + getAuthor() + "\n"
            + "Cover: " + getCover();
    }
}
