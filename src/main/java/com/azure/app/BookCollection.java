// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface BookCollection {

    /**
     * Reads all the JSON files and then saves their informations in new Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    Flux<Book> registerBooks();

    /**
     * Saves the book to a JSON file.
     *
     * @param book - Book object that's going to be saved
     */
    Mono<Boolean> saveBook(Book book);
}
