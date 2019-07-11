// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

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

public class LocalBookCollectorTest {
    private URL file = App.class.getClassLoader().getResource(".");
    private LocalBookCollector lclCollector = new LocalBookCollector();

    /**
     * Verifies the implementation of the findBook(String title) in the
     * LocalBookCollector object
     */
    @Test
    public void findTitlesTest() {
        lclCollector.saveBook("Existing", new Author("Mock", "Author"),
            new File(file.getPath() + "GreatGatsby.gif"));
        //Title that doesn't exist
        Flux<Book> books = lclCollector.findBook("ASDF");
        books.collectList().map(list -> {
            assertTrue(list.size() == 0);
            return list;
        });
        //Only one book has this title
        books = lclCollector.findBook("Existing");
        books.collectList().map(list -> {
            assertTrue(list.size() == 1);
            return list;
        });
        //Multiple books have this title
        lclCollector.saveBook("Existing", new Author("Mock2", "Author"),
            new File(file.getPath() + "GreatGatsby.gif")).block();
        books = lclCollector.findBook("Existing");
        books.collectList().map(list -> {
            assertTrue(list.size() > 1);
            return list;
        });
        deleteJsonFile(new File(Constants.JSON_PATH),
            new Book("Existing", new Author("Mock", "Author"),
                new File(file.getPath() + "GreatGatsby1.gif")));
    }

    /**
     * Verifies the implementation of the findBook(Author author) in the
     * LocalBookCollector object
     */
    @Test
    public void findAuthorsTest() {
        lclCollector.saveBook("Title", new Author("First", "Last"),
            new File(file.getPath() + "Wonder1.png")).block();
        //No authors
        Flux<Book> authorSearch = lclCollector.findBook(new Author("Not_asdff", "Available_xcv"));
        authorSearch.collectList().map(list -> {
            assertTrue(list.size() == 0);
            return list;
        });
        authorSearch = lclCollector.findBook(new Author("First", "Last"));
        authorSearch.collectList().map(list -> {
            assertTrue(list.size() == 1);
            return list;
        });
        //Create another book from same author
        lclCollector.saveBook("Title2", new Author("First", "Last"),
            new File(file.getPath() + "KK81.jpg"));
        authorSearch = lclCollector.findBook(new Author("First", "Last"));
        authorSearch.collectList().map(list -> {
            assertTrue(list.size() > 1);
            return list;
        });
        deleteJsonFile(new File(Constants.JSON_PATH),
            new Book("Title", new Author("First", "Last"),
                new File(file.getPath() + "Wonder1.png")));
        deleteJsonFile(new File(Constants.JSON_PATH),
            new Book("Title2", new Author("First", "Last"),
                new File(file.getPath() + "KK81.jpg")));
    }

    /**
     * For testing purposes only - To delete the json File but keep the image.
     */
    private void deleteJsonFile(File files, Book book) {
        try (Stream<Path> walk = Files.walk(Paths.get(files.getAbsolutePath()))) {
            List<String> result = walk.map(Path::toString).filter(f -> f.endsWith(".json")).collect(Collectors.toList());
            for (String file : result) {
                File newFile = new File(file);
                if (new OptionChecker().checkFile(newFile, book)) {
                    if (newFile.delete()) {
                        deleteEmptyDirectories();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clears out any empty directories that might have been leftover from when the JSON file was deleted.
     */
    private void deleteEmptyDirectories() {
        File[] files = new File(Constants.JSON_PATH).listFiles();
        clearFiles(files);
        File[] imageFiles = new File(Constants.IMAGE_PATH).listFiles();
        clearFiles(imageFiles);
    }

    /**
     * Assists the emptyDirectory method by traversing through the files. When it finds an empty directory without a
     * JSON file, it deletes that file.
     *
     * @param files - the folder containing the other files in the library.
     */
    private void clearFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                clearFiles(file.listFiles());
            }
            if (file.length() == 0 && !file.getAbsolutePath().endsWith(".json")) {
                file.delete();
            }
        }
    }

}
