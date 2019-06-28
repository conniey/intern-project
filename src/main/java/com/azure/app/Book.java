// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import java.io.File;

public class Book {
    private String title;
    private String[] author;
    private String cover;
    private File coverLoc;

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
     * Returns the author's first name.
     *
     * @return  string with author's first name
     */
    public String getFirstName() {
        return author[0];
    }

    /*
     * Returns the book's author's last name.
     *
     * @return string with author's last name.
     */
    public String getLastName() {
        return author[1];
    }

    /*
     * Returns the book's image file path.
     *
     * @return string with file path.
     */
    public String getCover() {
        return cover;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    private void setAuthor(String author) {
        this.author = author.split(" ");
    }

    private void setImage(File path) {
        coverLoc = path;
        cover = path.getAbsolutePath();
    }

    /*
     * Returns a string with the book information
     *
     * @return string containing book info
     */
    public String toString() {
        return getLastName() + ", " + getFirstName() + " - " + getTitle();
    }
}
