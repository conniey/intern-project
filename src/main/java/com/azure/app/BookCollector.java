// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;

final class BookCollector {
    private ImageProvider imageProvider;
    private DocumentProvider documentProvider;

    /**
     * Constructor for BookCollector
     *
     * @param documentCollection - the implementation for saving the text files of Books (Local or Cosmos)
     * @param imageProvider      - the implementation for saving the book cover images (Local or Blob Storage)
     */
    BookCollector(DocumentProvider documentCollection, ImageProvider imageProvider) {
        this.documentProvider = documentCollection;
        this.imageProvider = imageProvider;
    }

    /**
     * Saves the book to the storage system.
     *
     * @param book - Book object that's going to be saved
     * @return {@Link Mono} returns a Mono that was successful or has an error
     */
    Mono<Void> saveBook(Book book) {
        return book.isValid() && isFile(book.getCover())
            ? documentProvider.saveBook(book.getTitle(), book.getAuthor(), book.getCover())
                .then(imageProvider.saveImage(book)) : Mono.error(new IllegalArgumentException("Book can't be saved."));
    }

    /**
     * Returns the Flux of Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    Flux<Book> getBooks() {
        return documentProvider.getBooks();
    }

    /**
     * Overwrites the old book with the contents in the new book
     *
     * @param oldBook   - Book object that will be changed
     * @param newBook   - Book object with the new information to change to
     * @param saveCover - determines whether or not the user wants to keep the same cover
     * @return {@Link Mono}
     */
    Mono<Void> editBook(Book oldBook, Book newBook, int saveCover) {
        return documentProvider.editBook(oldBook, newBook, saveCover).
            then(imageProvider.editImage(oldBook, newBook, saveCover));
    }

    /**
     * Deletes the book and the file based off its information.
     *
     * @param book - Book that'll be deleted
     * @return Mono<Boolean> determines whether or not book was successfully deleted </Boolean>
     * true - Book was deleted
     * false - Book wasn't deleted
     */
    Mono<Void> deleteBook(Book book) {
        return book.isValid() ? documentProvider.deleteBook(book).then(imageProvider.deleteImage(book))
            : Mono.error(new IllegalArgumentException("Book wasn't deleted."));
    }

    /**
     * Filters out the book based on the specified title.
     *
     * @param title - String of the book title the user is looking for
     * @return - Flux of Book objects with that title
     */
    Flux<Book> findBook(String title) {
        return documentProvider.findBook(title);
    }

    /**
     * Filters out the books based on the specified author.
     *
     * @param author - Contains the name of the author the user is looking for
     * @return - Flux of Book objects by that author
     */
    Flux<Book> findBook(Author author) {
        return documentProvider.findBook(author);
    }

    /**
     * Determines whether the collection has books or not.
     *
     * @return {@Link Mono<Boolean>} - true if there are books
     * false - if there are no books
     */
    Mono<Boolean> hasBooks() {
        assert getBooks().hasElements().block() != null;
        return getBooks().hasElements();
    }

    /**
     * Converts the path to a file and then returns the URI to that path
     *
     * @param path - String containing the image file the user entered
     * @return URI of the image path
     */
    URI retrieveURI(String path) {
        return new File(path).toURI();
    }

    /**
     * Grab a String containing the absolute path to the book's cover location
     * If it's in Azure Database storage, the cover will be downloaded to the temporary directory.
     *
     * @param book - Book object of whose cover you want to retrieve
     * @return {@Link Mono} holds a String of the absolute path
     */
    Mono<String> grabCoverImage(Book book) {
        return imageProvider.grabCoverImage(book);
    }

    /**
     * Determines if a URI input is a file.
     *
     * @param entry - the URI to be checked
     * @return - boolean that determines if it's a valid file or not
     * true - if it's valid
     * false - if it's not valid
     */
    static boolean isFile(URI entry) {
        return entry != null && new File(entry).isFile();
    }
}
