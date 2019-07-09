// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;

interface BookCollection {

    /**
     * Gets all the books in the collection.
     *
     * @return {@link Flux} of {@link Book books} in this collection.
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
    Mono<Boolean> saveBook(String title, Author author, File path);
}
