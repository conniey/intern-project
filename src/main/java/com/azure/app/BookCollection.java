// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;

interface BookCollection {
    /**
     * Traverse through the files in the class
     */
    List<File> traverseJsonFiles();

    /**
     * Reads all the JSON files and then saves their information in new Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    Flux<Book> registerBooks();

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
