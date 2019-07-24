// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BookTest {

    /**
     * Verifies that the correct information was passed to the Book object
     */
    @Test
    public void testGoodBookChecker() {
        //Arrange
        URL folder = LocalBookCollectorTest.class.getClassLoader().getResource(".");
        Book book = new Book("Title", new Author("Good", "Book"), new File(folder.getPath() + "KK8.jpg").toURI());
        //Act and Assert
        assertTrue(book.checkBook());
    }

    //TEST INVALID BOOKS
    @Parameterized.Parameters
    public static Collection<Book[]> data() {
        //Arrange
        URL folder = LocalBookCollectorTest.class.getClassLoader().getResource(".");
        return Arrays.asList(new Book[][]{
            {new Book("", new Author("Good", "Book"), new File(folder.getPath() + "KK8.jpg").toURI())},
            {new Book("Title", new Author("Good", ""), new File(folder.getPath() + "KK8.jpg").toURI())},
            {new Book("Title", new Author("", "Bad"), new File(folder.getPath() + "KK8.jpg").toURI())},
            {new Book("Title", new Author("", ""), new File(folder.getPath() + "KK8.jpg").toURI())},
            {new Book("Title", new Author("Good", "Author"), null)}
        });
    }

    private Book bInput;
    private boolean expected;

    public BookTest(Book b) {
        this.bInput = b;
        this.expected = false;
    }

    @Test
    public void testInvalidBooks() {
        assertEquals(expected, bInput.checkBook());
    }
}
