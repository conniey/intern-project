// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
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

import static com.azure.app.Constants.IMAGE_PATH;
import static com.azure.app.Constants.JSON_PATH;
import static org.junit.Assert.assertNotNull;

public class LocalBookCollectorTest {
    private BookCollector localCollector;
    private String root;

    /**
     * Initializes the LocalDocumentProvider and determines the
     * root path to store all the files.
     */
    @Before
    public void setUp() {
        try {
            URI folder = LocalBookCollectorTest.class.getClassLoader().getResource(".").toURI();
            assertNotNull(folder);
            root = Paths.get(folder).toString();
        } catch (URISyntaxException e) {
            LoggerFactory.getLogger(LocalBookCollectorTest.class).error("Error in setting up the LocalBookCollectorTest: ", e);
            Assert.fail("Failed to set up the local book collector.");
        }
        localCollector = new BookCollector(new LocalDocumentProvider(root),
            new LocalImageProvider(root));
    }

    /**
     * Verifies the implementation of findBook when the title doesn't exist.
     */
    @Test
    public void testFindBookNoTitles() {
        //Arrange
        String expected = "AsOKalsdjfkal";
        //Act
        localCollector.saveBook(new Book("Existing", new Author("Mock", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI())).block();
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
     * in the LocalDocumentProvider object for one object.
     */
    @Test
    public void testFindBookOneTitle() {
        //Arrange
        String expected = "Existing";
        //Act
        localCollector.saveBook(new Book(expected, new Author("Mock", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI())).block();
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
        localCollector.saveBook(new Book(expected, new Author("Mock", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI())).block();
        localCollector.saveBook(new Book(expected, new Author("Mock2", "Author"),
            new File(Paths.get(root, "GreatGatsby.gif").toString()).toURI())).block();
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
        Book book = new Book("Title", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI());
        //Act
        localCollector.saveBook(book).block();
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
        Book newBook = new Book("Title", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI());
        //Act
        localCollector.saveBook(newBook).block();
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
        localCollector.saveBook(new Book("Title", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI())).block();
        localCollector.saveBook(new Book("Wishful", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI())).block();
        localCollector.saveBook(new Book("Winter", author,
            new File(Paths.get(root, "Wonder.png").toString()).toURI())).block();
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
    private boolean deleteJsonFile(Book book) {
        boolean delete = false;
        try (Stream<Path> walk = Files.walk(Paths.get(root, JSON_PATH))) {
            List<String> result = walk.map(Path::toString).filter(f -> f.endsWith(".json")).collect(Collectors.toList());
            for (String file : result) {
                File newFile = new File(file);
                if (new OptionChecker().checkFile(newFile, book)) {
                    if (newFile.delete()) {
                        new File(Paths.get(root, IMAGE_PATH, book.getAuthor().getLastName(),
                            book.getAuthor().getFirstName(),
                            new File(book.getCover()).getName()).toString()).delete();
                        deleteEmptyDirectories();
                        delete = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(LocalBookCollectorTest.class).error("Error deleting json file: ", e);
        }
        return delete;
    }

    /**
     * Clears out any empty directories that might have been leftover
     * from when the JSON file was deleted.
     */
    private void deleteEmptyDirectories() {
        File[] files = new File(Paths.get(root, JSON_PATH).toString()).listFiles();
        clearFiles(files);
        File[] imageFiles = new File(Paths.get(root, IMAGE_PATH).toString()).listFiles();
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

    /**
     * Tests saving a new book
     */
    @Test
    public void testSavingNewBook() {
        //Act
        StepVerifier.create(localCollector.saveBook(new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI())))
            //Assert
            .expectComplete()
            .verify();
        //Cleanup
        deleteJsonFile(new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI()));
    }

    /**
     * Tests saving two different books by the same author AND with the same cover
     */
    @Test
    public void testSavingDifferentBooksWithSameCover() {
        //Arrange
        boolean result;
        boolean result2;
        Book book1 = new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI());
        Book book2 = new Book("Giant Peach_The Return", new Author("Ronald", "Dahl"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI());
        //Act & Assert
        StepVerifier.create(localCollector.saveBook(new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI()))).expectComplete().verify();
        StepVerifier.create(localCollector.saveBook(new Book("Giant Peach_The Return", new Author("Ronald", "Dahl"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI()))).expectComplete().verify();
        //Should delete fine, because the same image would have been saved but in a file with a different name
        result = deleteJsonFile(book1);
        result2 = deleteJsonFile(book2);
        //Assert
        Assert.assertTrue(result);
        Assert.assertTrue(result2);
        //Cleanup
        deleteJsonFile(book1);
        deleteJsonFile(book2);
    }

    /**
     * Tests overwriting the same book but with a different cover image
     */
    @Test
    public void testOverwritingBook() {
        //Arrange
        File[] files = Paths.get(root, "lib", "images").toFile().listFiles();
        assertNotNull(files);
        int formerLength = files.length;
        boolean result;
        Book book1 = new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI());
        Book book2 = new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(Paths.get(root, "Gingerbread.jpg").toString()).toURI());
        //Act & Assert
        StepVerifier.create(localCollector.saveBook(new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(Paths.get(root, "Wonder.png").toString()).toURI()))).expectComplete().verify();
        StepVerifier.create(localCollector.saveBook(new Book("James and the Giant Peach", new Author("Ronald", "Dahl"),
            new File(Paths.get(root, "Gingerbread.jpg").toString()).toURI()))).expectComplete().verify();
        files = Paths.get(root, "lib", "images").toFile().listFiles();
        //Double check size
        Assert.assertEquals(formerLength, files.length);
    }
}

