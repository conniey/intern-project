package com.azure.app;

import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeleteBookTest {

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

        final BookCollection bookCollection = mock(BookCollection.class);
        when(bookCollection.getBooks()).thenReturn(Flux.just(book1, book2, book3));

        final DeleteBook deleteBook = new DeleteBook(bookCollection);

        // Act & Assert
        StepVerifier.create(deleteBook.lookupTitle(title))
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
}
