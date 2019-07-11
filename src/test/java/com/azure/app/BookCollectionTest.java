// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BookCollectionTest {
    private BookCollection collector = mock(BookCollection.class);

    /**
     * Verifies how a book should be saved.
     */
    @Test
    public void testSaveBook() {
        URL folder = BookCollectionTest.class.getClassLoader().getResource(".");
        File picFile = new File(folder.getPath() + "Gingerbread.jpg");
        Book expectedBook = new Book("Santa's Helper", new Author("Gingerbread", "Man"), picFile);
        boolean saved = when(collector.saveBook("Santa's Helper", new Author("Gingerbread", "Man"), picFile))
            .thenReturn(Mono.just(true)).equals(false);
        if (saved) {
            when(collector.getBooks()).thenReturn(Flux.just(expectedBook));
            StepVerifier.create(collector.getBooks())
                .expectNext(expectedBook)
                //Since the Flux was empty, the only book at this point should be expectedBook
                .expectComplete()
                .verify();
        }
    }

    /**
     * Verifies when we find by title with multiple matching books, they are returned.
     */
    @Test
    public void findMatchingTitles() {
        // Arrange
        String title = "John's Book";
        Book book1 = new Book(title, new Author("John", "Smith"), new File("image.jpg"));
        Book book2 = new Book(title, new Author("Suzie", "Smith"), new File("image2.jpg"));
        Book book3 = new Book("Sara's Book", new Author("Sara", "Summers"), new File("image3.jpg"));
        final Set<Book> expected = new HashSet<>();
        expected.add(book1);
        expected.add(book2);
        when(collector.getBooks()).thenReturn(Flux.just(book1, book2, book3));
        // Verify that all the books are in the collector before filtering
        StepVerifier.create(collector.getBooks())
            .expectNext(book1, book2, book3)
            .expectComplete()
            .verify();
        when(collector.findBook(title)).thenReturn(Flux.just(book1, book2));
        // Act & Assert
        StepVerifier.create(collector.findBook(title))
            .assertNext(book -> {
                // Verify that one of the books we got from that Flux is in our "expected" set.
                Assert.assertTrue(expected.remove(book));
            })
            .assertNext(book -> {
                // Verify that one of the books we got from that Flux is in our "expected" set.
                Assert.assertTrue(expected.remove(book));
            })
            // There should only be 2 matching books, so this Flux should complete after the two books are received.
            .expectComplete()
            .verify();
        // Assert that the set is empty because in the `assertNext` steps, we removed those books.
        Assert.assertTrue(expected.isEmpty());
    }

    /**
     * Verifies that we found titles by the same author
     */
    @Test
    public void findAuthorBooks() {
        Author author = new Author("Tom", "Jerry");
        Book book1 = new Book("Cheese", author, new File("image.gif"));
        Book book2 = new Book("Seeds", new Author("Tweety", "Bird"), new File("image.png"));
        Book book3 = new Book("Milk", author, new File("image.jpg"));
        final Set<Book> expected = new HashSet<>();
        expected.add(book1);
        expected.add(book3);
        when(collector.getBooks()).thenReturn(Flux.just(book1, book2, book3));
        //Verify that all the books are in the collector before filtering
        StepVerifier.create(collector.getBooks())
            .expectNext(book1, book2, book3)
            .expectComplete()
            .verify();
        when(collector.findBook(author)).thenReturn(Flux.just(book1, book3));
        StepVerifier.create(collector.findBook(author))
            .assertNext(book -> {
                Assert.assertTrue(expected.remove(book));
            })
            .assertNext(book -> {
                Assert.assertTrue(expected.remove(book));
            })
            .expectComplete()
            .verify();
        Assert.assertTrue(expected.isEmpty());
    }

    /**
     * Verifies that the book was deleted.
     */
    @Test
    public void testDeletion() {
        Book book = new Book("Deletion", new Author("In", "Progress"), new File("bye.jpg"));
        when(collector.getBooks()).thenReturn(Flux.just(book));
        StepVerifier.create(collector.getBooks())
            .expectNext(book)
            .expectComplete()
            .verify();
        boolean deleted = when(collector.deleteBook(book))
            .thenReturn(true).equals(false);
        if (deleted) {
            when(collector.getBooks()).thenReturn(Flux.empty());
            if (collector.deleteBook(book)) {
                StepVerifier.create(collector.getBooks())
                    .expectComplete()
                    .verify();
            }
        }
    }
}
