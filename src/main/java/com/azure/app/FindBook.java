// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FindBook {

    private Flux<Book> allBooks;
    private final AtomicReference<List<Book>> arBooks = new AtomicReference<>();
    private Flux<Book> results;

    private OptionChecker optionChecker = new OptionChecker();
    //Whether the search had 0 results, 1 result, or many results
    private int caseScenario;
    private long size;

    FindBook(Flux<Book> book) {
        allBooks = book;
    }

    /**
     * Filters out all the Books based off their title.
     *
     * @param title - string of the book's title
     * @return Flux containing all the books with the specified title
     */
    public Flux<Book> findTitles(String title) {
        return allBooks.filter(x -> checkTitle(x, title));
    }

    /**
     * Searches for the specified book by its title and relays that information to the user.
     *
     * @param title - String containing the title the user is searching for
     */
    public int searchByTitle(String title) {
        results = findTitles(title);
        results.count().subscribe(size -> {
            if (size == 0 || size == null) {
                setCaseScenario(0);
            } else if (size == 1) {
                setCaseScenario(1);
            } else {
                setCaseScenario(2);
            }
            setSize(size);
        });
        return caseScenario;
    }

    public long getSize() {
        return size;
    }

    /**
     * Searches for a specific book by its author and relays the information to the user.
     */
    public int searchByAuthor(String firstName, String lastName) {
        results = allBooks.filter(x -> checkAuthor(x, lastName, firstName));
        results.count().subscribe(size -> {
            if (size == 0 || size == null) {
                setCaseScenario(0);
            } else if (size == 1) {
                setCaseScenario(1);
            } else {
                setCaseScenario(2);
            }
        });
        return caseScenario;
    }

    /**
     * Under the condition where only one book is in the list. the method will show the result to the user
     * and then ask them if they wish to take a closer look at the book.
     *
     * @return boolean - true if they want to look into the book
     * - false otherwise
     */
    public Mono<Void> onlyOneResult(int viewOption) {
        return results.collectList().map(list -> {
            if (viewOption == 1) {
                System.out.println(list.get(0));
            } else if (viewOption == 2) {
                System.out.println(list.get(0).displayBookInfo());
            }
            return list;
        }).then();
    }

    /**
     * Under the condition where there are mutliple results, the user can select which one it wants to view.
     *
     * @param viewOption - 0 : prints out the list of results to the user.
     *                   - any other integer: the selected item they wished to view from the list.
     * @return A {@link Mono} that completes when the list has been read from.
     */
    public Mono<Void> manyResults(int viewOption) {
        return results.collectList().map(list -> {
            if (viewOption == 0) {
                for (int i = 0; i < list.size(); i++) {
                    System.out.println(i + 1 + ". " + list.get(i));
                }
            } else {
                System.out.println(list.get(viewOption - 1).displayBookInfo());
            }
            return list;
        }).then();
    }

    private boolean checkTitle(Book b, String title) {
        return title.contentEquals(b.getTitle());
    }

    private boolean checkAuthor(Book b, String lastName, String firstName) {
        return b.getAuthor().getLastName().contentEquals(lastName)
            && b.getAuthor().getFirstName().contentEquals(firstName);
    }

    private void setCaseScenario(int caseScenario) {
        this.caseScenario = caseScenario;
    }

    private void setSize(long size) {
        this.size = size;
    }

}
