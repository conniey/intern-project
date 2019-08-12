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
        final URL folder = BookTest.class.getClassLoader().getResource(".");
        Book book = new Book("Title", new Author("Good", "Book"), new File(folder.getPath() + "KK8.jpg").toURI());
        //Act and Assert
        assertTrue(book.isValid());
    }

    //TEST INVALID BOOKS
    /**
     * Gathers the data for the Book object that'll be tested
     *
     * @return Collection of Book objects
     */
    @Parameterized.Parameters
    public static Collection<Book[]> data() {
        //Arrange
        final URL folder = BookTest.class.getClassLoader().getResource(".");
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

    /**
     * Constructor for the BookTest to pass the parameters and set the
     * expected result
     *
     * @param b - one of the faulty Book objects
     */
    public BookTest(Book b) {
        this.bInput = b;
        this.expected = false;
    }

    /**
     * Verifies that none of the parameters pass the checkBook test
     */
    @Test
    public void testInvalidBooks() {
        assertEquals(expected, bInput.isValid());
    }
}
