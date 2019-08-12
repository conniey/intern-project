// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class BookCollectorTest {
    BookCollector bookCollector;

    /**
     * Sets up the BookCollector object
     */
    @Before
    public void setUp() {
        ImageProvider mockedImage = Mockito.mock(ImageProvider.class);
        DocumentProvider mockedDocument = Mockito.mock(DocumentProvider.class);
        bookCollector = new BookCollector(mockedDocument, mockedImage);
    }

    /**
     * Gathers the data for the Book object that'll be tested
     *
     * @return Collection of Book objects
     */
    @Parameterized.Parameters
    public static Collection<Book[]> data() {
        //Arrange
        URL folder = BookTest.class.getClassLoader().getResource(".");
        return Arrays.asList(new Book[][]{
            {new Book("", new Author("Good", "Book"), new File(folder.getPath() + "KK8.jpg").toURI())}, //No title
            {new Book("Title", new Author("Good", ""), new File(folder.getPath() + "KK8.jpg").toURI())}, // No last name
            {new Book("Title", new Author("", "Bad"), new File(folder.getPath() + "KK8.jpg").toURI())}, // No first name
            {new Book("Title", new Author("", ""), new File(folder.getPath() + "KK8.jpg").toURI())}, // Empty author
            {new Book("Title", new Author("Good", "Author"), null)}, // null File
        });
    }
    private Book bInput;
    /**
     * Constructor for the BookCollectorTest to pass the parameters
     *
     * @param b - one of the faulty Book objects
     */
    public BookCollectorTest(Book b) {
        this.bInput = b;
    }

    /**
     * Verifies that none of the parameters pass the checkBook test
     */
    @Test
    public void testSavingInvalidBooks() {
        StepVerifier.create(bookCollector.saveBook(bInput))
            .verifyError();
    }

    /**
     * Verifies that none of the parameters will pass to deletion.
     */
    @Test
    public void testDeletingBooks() {
        StepVerifier.create(bookCollector.deleteBook(bInput))
            .verifyError();
    }
}
