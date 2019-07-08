// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalBookCollectorTests {

    private LocalBookCollector bookCollector = new LocalBookCollector();

    /**
     * Makes sure that books can be saved properly.
     */
    @Test
    public void testSaveBooks() {
        URL folder = AppTest.class.getClassLoader().getResource(".");
        try {
            //Good book data
            assertTrue(bookCollector.saveBook("Title", new Author("Good", "Author"),
                new File(folder.getPath() + "\\Book.png")).block());
            //Bad book data concerning the title and author.
            assertFalse(bookCollector.saveBook("", new Author("Bad", ""),
                new File(folder.getPath() + "\\Book.png")).block());
            //Bad data concerning the file name
            assertFalse(bookCollector.saveBook("Title", new Author("The Good", "The Bad"), new File
                ("The Ugly.png")).block());
        } catch (NullPointerException e) {
            Assert.fail("");
        }
    }

    /**
     * Tests to see if books can be registered
     */
    @Test
    public void testRegisterBooks() {
        try {
            bookCollector.registerBooks();
            assertTrue(true);
        } catch (NullPointerException e) {
            Assert.fail("");
        }
    }

}
