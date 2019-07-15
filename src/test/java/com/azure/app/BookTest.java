/*
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BookTest {

    */
/**
 * Verifies that the correct information was passed to the Book object
 *//*

    @Test
    public void testBookChecker() {
        //Arrange
        URL folder = BookTest.class.getClassLoader().getResource(".");
        Book book = new Book("Title", new Author("Good", "Book"), new File(folder.getPath() + "KK8.jpg"));
        Book badTitle = new Book("", new Author("Good", "Book"), new File(folder.getPath() + "KK8.jpg"));
        Book badAuthorLastName = new Book("Title", new Author("Good", ""), new File(folder.getPath() + "KK8.jpg"));
        Book badAuthorFirstName = new Book("Title", new Author("", "Bad"), new File(folder.getPath() + "KK8.jpg"));
        Book badAuthorCompletely = new Book("Title", new Author("", ""), new File(folder.getPath() + "KK8.jpg"));
        Book emptyFile = new Book("Title", new Author("Good", "Author"), new File(""));
        Book badFile = new Book("Title", new Author("Good", "Author"), null);
        Book notPicture = new Book("Title", new Author("Good", "Author"), new File(folder.getPath() + "Test.docx"));

        //Act and Assert
        assertTrue(book.checkBook());
        assertFalse(badTitle.checkBook());
        assertFalse(badAuthorLastName.checkBook());
        assertFalse(badAuthorFirstName.checkBook());
        assertFalse(badAuthorCompletely.checkBook());
        assertFalse(emptyFile.checkBook());
        assertFalse(badFile.checkBook());
        assertFalse(notPicture.checkBook());
    }
}
*/
