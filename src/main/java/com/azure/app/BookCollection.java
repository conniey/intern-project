// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

interface BookCollection {
    /**
     * Returns the Flux of Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    Flux<Book> getBooks();

    /**
     * Saves the book as a JSON file
     *
     * @param title  - String containing the title of the book
     * @param author - Author object of the book
     * @param path   - File containing the cover image of the book
     * @return Mono<Boolean> that determines whether the book got saved or not
     * true - book was successfully saved
     * false - book wasn't saved </Boolean>
     */
    Mono<Void> saveBook(String title, Author author, URI path);

    Mono<Void> editBook(Book oldBook, Book newBook, int saveCover);

    /**
     * Deletes the book and the file based off its information.
     *
     * @param book - Book that'll be deleted
     * @return Mono<Boolean> determines whether or not book was successfully deleted </Boolean>
     * true - Book was deleted
     * false - Book wasn't deleted
     */
    Mono<Void> deleteBook(Book book);

    /**
     * Filters out the book based on the specified title.
     *
     * @param title - String of the book title the user is looking for
     * @return - Flux of Book objects with that title
     */
    Flux<Book> findBook(String title);

    /**
     * Filters out the books based on the specified author.
     *
     * @param author - Contains the name of the author the user is looking for
     * @return - Flux of Book objects by that author
     */
    Flux<Book> findBook(Author author);

    /**
     * Determines whether the collection has books or not.
     *
     * @return {@Link Mono<Boolean>} - true if there are books
     * false - if there are no books
     */
    Mono<Boolean> hasBooks();

    /**
     * Converts the path to a file and then returns the URI to that path
     *
     * @param path - String containing the image file the user entered
     * @return URI of the image path
     */
    URI retrieveURI(String path);

    /**
     * Grab a String containing the absolute path to the book's cover location
     * If it's in Azure Database storage, the cover will be downloaded to the temporary directory.
     *
     * @param book - Book object of whose cover you want to retrieve
     * @return {@Link Mono} holds a String of the absolute path
     */
    Mono<String> grabCoverImage(Book book);
}
