// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;

class FilterBooks {
    private final Flux<Book> allBooks;

    FilterBooks(Flux<Book> book) {
        allBooks = book;
    }

    /**
     * Filters out all the Books based off their title.
     *
     * @param title - string of the book's title
     * @return Flux containing all the books with the specified title
     */
    Flux<Book> findTitles(String title) {
        return allBooks.filter(x -> checkTitle(x, title));
    }

    /**
     * Filters out all the books based of the author's name
     *
     * @param firstName - String containing author's first name
     * @param lastName  - String containing author's last name
     * @return Flux<Book> with all the books from specified author
     */
    Flux<Book> findAuthor(String firstName, String lastName) {
        return allBooks.filter(x -> checkAuthor(x, lastName, firstName));
    }

    private boolean checkTitle(Book b, String title) {
        return title.contentEquals(b.getTitle());
    }

    private boolean checkAuthor(Book b, String lastName, String firstName) {
        return b.getAuthor().getLastName().contentEquals(lastName)
            && b.getAuthor().getFirstName().contentEquals(firstName);
    }
}
