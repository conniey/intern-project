// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class LocalBookCollectorTests {

    private LocalBookCollector bookCollector = new LocalBookCollector();

    /**
     * Makes sure that books can be saved properly.
     */
    @Test
    @Ignore("Needs update to use local paths")
    public void testSaveBooks() {
        URL folder = AppTest.class.getClassLoader().getResource(".");
        try {
            assertTrue(bookCollector.saveBook("Title", new Author("Good", "Author"),
                new File(folder.getPath() + "\\Book.png")).block());
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
