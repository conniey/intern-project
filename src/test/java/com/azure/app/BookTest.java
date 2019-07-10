package com.azure.app;

import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BookTest {
    @Test
    public void testBookChecker() {
        URL folder = BookTest.class.getClassLoader().getResource(".");
        //Good book
        Book book = new Book("Title", new Author("Good", "Book"), new File(folder.getPath() + "KK8.jpg"));
        assertTrue(book.checkBook());
        // TITLE
        book = new Book("", new Author("Good", "Book"), new File(folder.getPath() + "KK8.jpg"));
        assertFalse(book.checkBook());
        // AUTHOR
        book = new Book("Title", new Author("Good", ""), new File(folder.getPath() + "KK8.jpg"));
        assertFalse(book.checkBook());
        book = new Book("Title", new Author("", "Bad"), new File(folder.getPath() + "KK8.jpg"));
        assertFalse(book.checkBook());
        book = new Book("Title", new Author("", ""), new File(folder.getPath() + "KK8.jpg"));
        assertFalse(book.checkBook());
        //FILE
        book = new Book("Title", new Author("Good", "Author"), new File(""));
        assertFalse(book.checkBook());
        book = new Book("Title", new Author("Good", "Author"), null);
        assertFalse(book.checkBook());
        book = new Book("Title", new Author("Good", "Author"), new File(folder.getPath() + "Test.docx"));
        assertFalse(book.checkBook());
    }
}
