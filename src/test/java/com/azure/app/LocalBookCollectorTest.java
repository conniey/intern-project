// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalBookCollectorTest {
    private LocalBookCollector localCollector;
    private String root;

    /**
     * Initializes the LocalBookCollector and determines the
     * root path to store all the files.
     */
    @Before
    public void setUp() {
        try {
            URI folder = LocalBookCollector.class.getClassLoader().getResource(".").toURI();
            root = Paths.get(folder).toString();
        } catch (URISyntaxException e) {
            Assert.fail("");
        }
        localCollector = new LocalBookCollector(root);
    }

    /**
     * Verifies the implementation of findBook when the title doesn't exist.
     */
    @Test
    public void testFindBookNoTitles() {
        //Arrange
        String expected = "AsOKalsdjfkal";
        //Act
        localCollector.saveBook("Existing", new Author("Mock", "Author"),
            new File(Paths.get(root, "GreatGatsby.json").toString()).toURI()).block();
        Flux<Book> noTitles = localCollector.findBook(expected);
        //Assert
        StepVerifier.create(noTitles)
            //Assert that it's empty
            .expectComplete()
            .verify();
        //Cleanup
        deleteJsonFile(new Book("Existing", new Author("Mock", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI()));
    }

    /**
     * Verifies the implementation of the findBook(String title)
     * in the LocalBookCollector object for one object.
     */
    @Test
    public void testFindBookOneTitle() {
        //Arrange
        String expected = "Existing";
        //Act
        localCollector.saveBook(expected, new Author("Mock", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI()).block();
        Flux<Book> oneBook = localCollector.findBook(expected);
        //Assert
        StepVerifier.create(oneBook)
            .assertNext(book -> Assert.assertEquals(expected, book.getTitle()))
            .expectComplete()
            .verify();
        //Cleanup
        deleteJsonFile(new Book("Existing", new Author("Mock", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI()));
    }

    /**
     * Verifies the implementation of
     * the findBook when there's multiple results
     */
    @Test
    public void testFindBookManyTitles() {
        //Arrange
        String expected = "Existing";
        //Act
        localCollector.saveBook(expected, new Author("Mock", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI()).block();
        localCollector.saveBook(expected, new Author("Mock2", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI()).block();
        Flux<Book> manyBooks = localCollector.findBook(expected);
        //Assert
        StepVerifier.create(manyBooks)
            .assertNext(book -> Assert.assertEquals(expected, book.getTitle()))
            .assertNext(book -> Assert.assertEquals(expected, book.getTitle()))
            .expectComplete()
            .verify();
        //Cleanup
        deleteJsonFile(new Book(expected, new Author("Mock", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI()));
        deleteJsonFile(new Book(expected, new Author("Mock2", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI()));
    }

    /**
     * Tests the implementation of the finding the books by a specified author.
     * This test checks the condition where there are no books by that author.
     */
    @Test
    public void testFindAuthorsNoResult() {
        //Arrange
        Author author = new Author("First", "Last");
        //Act
        localCollector.saveBook("Title", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI()).block();
        Flux<Book> noAuthors = localCollector.findBook(new Author("Not_asdff", "Available_xcv"));
        //Assert
        StepVerifier.create(noAuthors)
            .expectComplete()
            .verify();
        //Cleanup
        deleteJsonFile(new Book("Existing", new Author("Mock", "Author"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI()));
    }

    /**
     * Tests the implementation of finding the books by a specified author.
     * This checks the condition where there's only one book by that author.
     */
    @Test
    public void testFindAuthorsOneResult() {
        //Arrange
        Author author = new Author("First", "Last");
        //Act
        localCollector.saveBook("Title", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI()).block();
        Flux<Book> oneAuthor = localCollector.findBook(author);
        //Assert
        StepVerifier.create(oneAuthor)
            .assertNext(book -> {
                Assert.assertEquals(author.getLastName(), book.getAuthor().getLastName());
                Assert.assertEquals(author.getFirstName(), book.getAuthor().getFirstName());
            })
            .expectComplete()
            .verify();
        //Cleanup
        deleteJsonFile(new Book("Title", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI()));
    }

    /**
     * Tests the implementation of the finding the books by a specified author.
     * This checks the condition where there are multiple books by that author.
     */
    @Test
    public void testFindAuthorsMultipleResults() {
        //Arrange
        Author author = new Author("First", "Last");
        //Act
        localCollector.saveBook("Title", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI()).block();
        localCollector.saveBook("Wishful", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI()).block();
        localCollector.saveBook("Winter", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI()).block();
        Flux<Book> manyAuthors = localCollector.findBook(author);
        //Assert
        StepVerifier.create(manyAuthors)
            .assertNext(book -> {
                Assert.assertEquals(author.getLastName(), book.getAuthor().getLastName());
                Assert.assertEquals(author.getFirstName(), book.getAuthor().getFirstName());
            })
            .assertNext(book -> {
                Assert.assertEquals(author.getLastName(), book.getAuthor().getLastName());
                Assert.assertEquals(author.getFirstName(), book.getAuthor().getFirstName());
            })
            .assertNext(book -> {
                Assert.assertEquals(author.getLastName(), book.getAuthor().getLastName());
                Assert.assertEquals(author.getFirstName(), book.getAuthor().getFirstName());
            })
            .expectComplete()
            .verify();
        //Cleanup
        deleteJsonFile(new Book("Title", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI()));
        deleteJsonFile(new Book("Wishful", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI()));
        deleteJsonFile(new Book("Winter", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI()));
    }

    /**
     * For testing purposes only -
     * To delete the json File but keep the image.
     */
    private void deleteJsonFile(Book book) {
        try (Stream<Path> walk = Files.walk(Paths.get(root, Constants.JSON_PATH))) {
            List<String> result = walk.map(Path::toString).filter(f -> f.endsWith(".json")).collect(Collectors.toList());
            for (String file : result) {
                File newFile = new File(file);
                if (new OptionChecker().checkFile(newFile, book)) {
                    if (newFile.delete()) {
                        new File(Paths.get(root, Constants.IMAGE_PATH, book.getAuthor().getLastName(),
                            book.getAuthor().getFirstName(),
                            new File(book.getCover()).getName()).toString()).delete();
                        deleteEmptyDirectories();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clears out any empty directories that might have been leftover
     * from when the JSON file was deleted.
     */
    private void deleteEmptyDirectories() {
        File[] files = new File(Paths.get(root, Constants.JSON_PATH).toString()).listFiles();
        clearFiles(files);
        File[] imageFiles = new File(Paths.get(root, Constants.IMAGE_PATH).toString()).listFiles();
        clearFiles(imageFiles);
    }

    /**
     * Assists the emptyDirectory method by traversing through the
     * files. When it finds an empty directory without a JSON file, it deletes that file.
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
