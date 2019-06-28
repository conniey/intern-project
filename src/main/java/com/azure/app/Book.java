// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import java.io.File;

public class Book {
    private String title;
    private Author author;
    private File cover;

    Book(String title, String author, File cover) {
        setTitle(title);
        setAuthor(author);
        setImage(cover);
    }

    /*
     * Returns the book's title.
     *
     * @return  string with book's title
     */
    public String getTitle() {
        return title;
    }

    /*
     * Returns the author's name
     *
     * @return  Author object
     */
    public Author getAuthor() {
        return author;
    }

    public File getCover() {
        return cover;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    private void setAuthor(String name) {
        String[] authorName = name.split(" ");
        String lastName = authorName[authorName.length - 1];
        String firstName = authorName[0];
        for (int i = 1; i < authorName.length - 1; i++) {
            firstName += " " + authorName[i];
        }
        author = new Author(lastName, firstName);
    }

    private void setImage(File path) {
        cover = path;
    }

    /*
     * Returns a string with the book information
     *
     * @return string containing book info
     */
    public String toString() {
        return author.getLastName() + ", " + author.getFirstName() + " - " + getTitle();
    }
}

