// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonHandlerTest {
    private JsonHandler jsonHandler = new JsonHandler();
    private URL folder = FilterTester.class.getClassLoader().getResource(".");

    /**
     * Tests the JsonHandler class
     */
    @Test
    public void testSerializationAndCheckBook() {
        //Good book
        Book b = new Book("Wonder", new Author("RJ", "Palacio"),
            new File(folder.getPath() + "\\Wonder.png"));
        assertTrue(jsonHandler.writeJSON(b));
        //Bad book (empty title)
        Book b2 = new Book("", new Author("RJ", "Palacio"),
            new File(folder.getPath() + "\\Wonder1.png")
        );
        assertFalse(jsonHandler.writeJSON(b2));
        //Bad book (Invalid author)
        Book b3 = new Book("Wonder", new Author("", null),
            new File(folder.getPath() + "\\Book.png"));
        assertFalse(jsonHandler.writeJSON(b3));
        //Bad book (Wrong file path)
        Book b4 = new Book("Wonder", new Author("Palacio", "R. J."),
            new File(""));
        assertFalse(jsonHandler.writeJSON(b4));
        //Completely bad book (wrong on all aspects)
        Book b5 = new Book(null, new Author(null, null),
            new File(""));
        assertFalse(jsonHandler.writeJSON(b5));
        //Delete test book
        deleteJsonFile(new File("\\lib\\jsonFiles\\"), b);
    }

    /**
     * Tests the deserialization of a JSON file to a book
     */
    @Test
    public void testFromJSONtoBook() {
        //Test with valid data
        Book result = jsonHandler.fromJSONtoBook(new File(folder.getPath()
            + "\\Kingdom Keepers VIII.json"));
        assertTrue(result != null);
        //Test with invalid data
        result = jsonHandler.fromJSONtoBook(new File(folder.getPath()
            + "//asdfasdf"));
        assertTrue(result == null);
    }


    /**
     * For testing purposes only - To delete the json File but keep the image.
     */
    private void deleteJsonFile(File files, Book book) {
        try (Stream<Path> walk = Files.walk(Paths.get(files.getAbsolutePath()))) {
            List<String> result = walk.map(x -> x.toString()).filter(f -> f.endsWith(".json")).collect(Collectors.toList());
            for (String file : result) {
                File newFile = new File(file);
                if (new OptionChecker().checkFile(newFile, book)) {
                    if (newFile.delete()) {
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
