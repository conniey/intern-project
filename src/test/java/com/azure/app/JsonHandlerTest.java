// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JsonHandlerTest {
    private JsonHandler jsonHandler = new JsonHandler();
    private String root;

    /**
     * Sets up the root so the Json handler knows where to store/find the JSON files
     */
    @Before
    public void setUp() {
        try {
            URI folder = Objects.requireNonNull(JsonHandlerTest.class.getClassLoader().getResource(".")).toURI();
            root = Paths.get(folder).toString();
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(JsonHandlerTest.class).error("Error in setting up JsonHandlerTest: ", e);
            Assert.fail("");
        }
    }

    /**
     * Tests serialization of a valid book.
     */
    @Test
    public void testSerializingGoodBook() {
        //Arrange
        Book b = new Book("Wonder", new Author("RJ", "Palacio"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI());
        //Act
        boolean result = jsonHandler.writeJSON(b, root);
        //Assert
        assertTrue(result);
        //Cleanup
        deleteJsonFile(new File(Constants.JSON_PATH), b);
    }

    /**
     * Tests serialization of a book with an invalid title.
     */
    @Test
    public void testSerializingBadTitle() {
        //Arrange
        Book b = new Book("", new Author("RJ", "Palacio"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI());
        //Act
        boolean result = jsonHandler.writeJSON(b, root);
        //Assert
        assertFalse(result);
    }

    /**
     * Tests serialization of a book with an invalid author.
     */
    @Test
    public void testSerializingBadAuthor() {
        //Arrange
        Book b = new Book("Wonder", new Author("", null),
            new File(Paths.get(root, "Wonder.png").toString()).toURI());
        //Act
        boolean result = jsonHandler.writeJSON(b, root);
        //Assert
        assertFalse(result);
    }

    /**
     * Tests serialization of a completely invalid book
     */
    @Test
    public void testSerializingBadBook() {
        //Arrange
        Book b = new Book(null, new Author(null, null), //completely invalid #4
            new File("").toURI());
        //Act
        boolean result = jsonHandler.writeJSON(b, root);
        //Assert
        assertFalse(result);
    }

    /**
     * Tests the deserialization of a JSON file to a book
     */
    @Test
    public void testFromJSONtoBookValid() {
        //Arrange and Act
        Book result = jsonHandler.fromJSONtoBook(Paths.get(root, "Kingdom Keepers VIII.json").toFile());
        //Assert
        assertNotNull(result);
    }

    /**
     * Tests the deserialization of a bad JSON file.
     */
    @Test
    public void testFromJSONtoBookInvalid() {
        //Arrange and Act
        Book result = jsonHandler.fromJSONtoBook(new File(Paths.get(root, "asdfasdf").toString()));
        //Assert
        assertNull(result);
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
            LoggerFactory.getLogger(JsonHandlerTest.class).error("Error deleting files: ", e);
        }
    }

    /**
     * Clears out any empty directories that might have been leftover from when the JSON file was deleted.
     */
    private void deleteEmptyDirectories() {
        File[] files = new File(Constants.JSON_PATH).listFiles();
        assert files != null;
        clearFiles(files);
        File[] imageFiles = new File(Constants.IMAGE_PATH).listFiles();
        assert imageFiles != null;
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
                clearFiles(Objects.requireNonNull(file.listFiles()));
            }
            if (file.length() == 0 && !file.getAbsolutePath().endsWith(".json")) {
                file.delete();
            }
        }
    }
}
