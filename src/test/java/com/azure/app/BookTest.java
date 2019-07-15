// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BookTest {

    /**
     * Verifies that the correct information was passed to the Book object
     */
    @Test
    public void testBookChecker() {
        //Arrange
        URL folder = LocalBookCollectorTest.class.getClassLoader().getResource(".");
        String root = null;
        try {
            URI rootFolder = LocalBookCollector.class.getClassLoader().getResource(".").toURI();
            root = Paths.get(rootFolder).toString();
        } catch (URISyntaxException e) {
            Assert.fail("");
        }
        Book book = new Book("Title", new Author("Good", "Book"), new File(folder.getPath() + "KK8.jpg").toURI());
        Book badTitle = new Book("", new Author("Good", "Book"), new File(folder.getPath() + "KK8.jpg").toURI());
        Book badAuthorLastName = new Book("Title", new Author("Good", ""), new File(folder.getPath() + "KK8.jpg").toURI());
        Book badAuthorFirstName = new Book("Title", new Author("", "Bad"), new File(folder.getPath() + "KK8.jpg").toURI());
        Book badAuthorCompletely = new Book("Title", new Author("", ""), new File(folder.getPath() + "KK8.jpg").toURI());
        Book emptyFile = new Book("Title", new Author("Good", "Author"), new File("").toURI());
        Book badFile = new Book("Title", new Author("Good", "Author"), null);
        Book notPicture = new Book("Title", new Author("Good", "Author"), new File(folder.getPath() + "Test.docx").toURI());

        //Act and Assert
        assertTrue(book.checkBook(root));
        assertFalse(badTitle.checkBook(root));
        assertFalse(badAuthorLastName.checkBook(root));
        assertFalse(badAuthorFirstName.checkBook(root));
        assertFalse(badAuthorCompletely.checkBook(root));
        assertFalse(emptyFile.checkBook(root));
        assertFalse(badFile.checkBook(root));
        assertFalse(notPicture.checkBook(root));
    }
}
