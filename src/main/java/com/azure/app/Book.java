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
     * @return  string with book's title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the author's name
     *
     * @return  Author object
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
     * Returns a string with the book information
     *
     * @return string containing book info
     */
    public String toString() {
        return author + " -" + " " + getTitle();
    }
}

