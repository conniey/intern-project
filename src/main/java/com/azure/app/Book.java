package com.azure.app;

public class Book {
    private String title;
    private String author;
    private String cover;

    Book() {
    }

    Book(String title, String author, String cover) {
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

    public String getCover() {
        return cover;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    private void setAuthor(String author) {
        this.author = author;
    }

    private void setCover(String path) {
        cover = path;
    }
}
