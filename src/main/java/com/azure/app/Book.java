package com.azure.app;

import java.io.File;

public class Book {
    private String title;
    private String author;
    private File cover;

    Book() {
    }

    Book(String title, String author, File cover) {
        setTitle(title);
        setAuthor(author);
        setCover(cover);
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }


    private void setTitle(String title) {
        this.title = title;
    }

    private void setAuthor(String author) {
        this.author = author;
    }

    private void setCover(File path) {
        cover = path;
    }
}
