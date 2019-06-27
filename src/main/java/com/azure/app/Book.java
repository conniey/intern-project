package com.azure.app;

import java.io.File;

public class Book {
    private String title;
    private String[] author;
    private String url;
    private File cover;

    Book() {
    }

    Book(String title, String author, File cover) {
        setTitle(title);
        setAuthor(author);
        setImage(cover);
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return author[0];
    }

    public String getLastName() {
        /*String lastName = "";
        //Under the assumption that people have multple last names
        for (int i = 1; i < author.length; i++)
            lastName += author[i];
        return lastName;*/
        return author[1];
    }


    public String getUrl() {
        return url;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    private void setAuthor(String author) {
        this.author = author.split(" ");
    }

    private void setImage(File path) {
        cover = path;
        url = path.getAbsolutePath();
    }
}
