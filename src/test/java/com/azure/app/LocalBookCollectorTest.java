// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

/*
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore("Until it's corrected") */
public class LocalBookCollectorTest {
    // private URL file = App.class.getClassLoader().getResource(".");
    // private LocalBookCollector lclCollector = mock(LocalBookCollector.class);
    /*
     *//**
     * Tests the filtering of books using titles that don't exist, titles that do exist, and
     * multiple titles that exist.
     *//*
    @Test
    public void testFilterTitle() {
        when (lclCollector.)
        lclCollector.saveBook("Existing", new Author("Mock", "Author"),
            new File(file.getPath() + "\\GreatGatsby1.gif")).block();
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
            new File(file.getPath() + "\\GreatGatsby1.gif")).block();
        books = filterBooks.findTitles("Existing");
        books.collectList().map(list -> {
            assertTrue(list.size() > 1);
            return list;
        });
        deleteJsonFile(new File("\\lib\\jsonFiles\\"),
            new Book("Existing", new Author("Mock", "Author"),
                new File(file.getPath() + "\\GreatGatsby1.gif")));
    }

*
     * Tests the filtering of books using authors that do and do not exist


    @Test
    public void testFilterAuthor() {
        lclCollector.saveBook("Title", new Author("First", "Last"),
            new File(file.getPath() + "\\Wonder1.png")).block();
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
            new File(file.getPath() + "\\KK81.jpg")).block();
        authorSearch = filterBooks.findAuthor("First", "Last");
        authorSearch.collectList().map(list -> {
            assertTrue(list.size() > 1);
            return list;
        });
        deleteJsonFile(new File("\\lib\\jsonFiles"),
            new Book("Title", new Author("First", "Last"),
                new File(file.getPath() + "\\Wonder1.png")));
        deleteJsonFile(new File("\\lib\\jsonFiles"),
            new Book("Title2", new Author("First", "Last"),
                new File(file.getPath() + "\\KK81.jpg")));
    }

*
     * For testing purposes only - To delete the json File but keep the image.


    private void deleteJsonFile(File files, Book book) {
        try (Stream<Path> walk = Files.walk(Paths.get(files.getAbsolutePath()))) {
            List<String> result = walk.map(x -> x.toString()).filter(f -> f.endsWith(".json")).collect(Collectors.toList());
            for (String file : result) {
                File newFile = new File(file);
                if (new OptionChecker().checkFile(newFile, book)) {
                    if (newFile.delete()) {
                        new DeleteBook().deleteEmptyDirectories();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}
