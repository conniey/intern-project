// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Test;
import reactor.core.publisher.Flux;

import java.io.File;
import java.net.URL;

import static junit.framework.TestCase.assertTrue;

public class FilterTester {
    private FilterBooks filterBooks = new FilterBooks(new LocalBookCollector().registerBooks());
    private URL file = App.class.getClassLoader().getResource(".");
    LocalBookCollector lclCollector = new LocalBookCollector();

    /**
     * Tests the filtering of books using titles that don't exist, titles that do exist, and
     * multiple titles that exist.
     */
    @Test
    public void testFilterTitle() {
        lclCollector.saveBook("Existing", new Author("Mock", "Author"),
            new File(file.getPath() + "\\GreatGatsby.gif")).block();
        //Title that doesn't exist
        Flux<Book> books = filterBooks.findTitles("ASDF");
        books.collectList().map(list -> {
            assertTrue(list.size() == 0);
            return list;
        });
        //Only one book has this title
        books = filterBooks.findTitles("Existing");
        books.collectList().map(list -> {
            assertTrue(list.size() == 1);
            return list;
        });
        //Multiple books have this title
        lclCollector.saveBook("Existing", new Author("Mock2", "Author"),
            new File(file.getPath() + "\\GreatGatsby.gif")).block();
        books = filterBooks.findTitles("Existing");
        books.collectList().map(list -> {
            assertTrue(list.size() > 1);
            return list;
        });
    }

    /**
     * Tests the filtering of books using authors that do and do not exist
     */
    @Test
    public void testFilterAuthor() {
        lclCollector.saveBook("Title", new Author("First", "Last"),
            new File(file.getPath() + "\\Wonder.png")).block();
        //No authors
        Flux<Book> authorSearch = filterBooks.findAuthor("Not_asdff", "Available_xcv");
        authorSearch.collectList().map(list -> {
            assertTrue(list.size() == 0);
            return list;
        });
        authorSearch = filterBooks.findAuthor("First", "Last");
        authorSearch.collectList().map(list -> {
            assertTrue(list.size() == 1);
            return list;
        });
        //Create another book from same author
        lclCollector.saveBook("Title2", new Author("First", "Last"),
            new File(file.getPath() + "\\KK8.jpg")).block();
        authorSearch = filterBooks.findAuthor("First", "Last");
        authorSearch.collectList().map(list -> {
            assertTrue(list.size() > 1);
            return list;
        });
    }
}
