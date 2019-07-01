// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;

import java.util.Scanner;

public class FindBook {

    private Flux<Book> allBooks;
    private static int numIndex = 1;
    private Scanner sc = new Scanner(System.in);


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

        Flux<Book> sameTitle = allBooks.filter(x -> checkTitle(x, title));
        return sameTitle;
    }

    /**
     * Searches for a specific book by the title to relay information to user.
     */
    public void searchByTitle() {
        System.out.println("What is the book title?");
        String title = sc.nextLine();
        Flux<Book> sameTitle = findTitles(title);
        sameTitle.hasElements().subscribe(notEmpty -> {
            if (notEmpty) {
                System.out.println("Here are the matching books. Please enter the number you wish to view.");
                sameTitle.subscribe(x -> System.out.println(increment() + ". " + x));
                int choice;
                do {
                    String option = sc.nextLine();
                    choice = checkOption(option);
                } while (choice == -1);
                sameTitle.elementAt(choice - 1).subscribe(x -> displayBookInfo(x));

            } else {
                System.out.println("There are no books with that title.");
            }
        });
        numIndex = 1;
    }

    /**
     * Searches for a specific book by its author to relay the information to the user.
     */
    public void searchByAuthor() {
        System.out.println("What is the author's name?");
        String author = sc.nextLine();
        String[] authorName = author.split(" ");
        String lastName = authorName[authorName.length - 1];
        String first = authorName[0];
        for (int i = 1; i < authorName.length - 1; i++) {
            first += " " + authorName[i];
        }
        String firstName = first;
        Flux<Book> sameAuthor = allBooks.filter(x -> checkAuthor(x, lastName, firstName));
        sameAuthor.hasElements().subscribe(notEmpty -> {
            if (notEmpty) {
                System.out.println("Here are books by " + author + ". Please enter the number you wish to view.");
                sameAuthor.subscribe(x -> System.out.println(increment() + ". " + x));
                int choice;
                do {
                    String option = sc.nextLine();
                    choice = checkOption(option);
                } while (choice == -1);
                sameAuthor.elementAt(choice - 1).subscribe(x -> displayBookInfo(x));
            } else {
                System.out.println("There are no authors with that name.");
            }
        });
        numIndex = 1;
    }

    private String increment() {
        return "" + numIndex++;
    }

    private boolean checkTitle(Book b, String title) {
        return title.contentEquals(b.getTitle());
    }

    private boolean checkAuthor(Book b, String lastName, String firstName) {
        return b.getAuthor().getLastName().contentEquals(lastName)
            && b.getAuthor().getFirstName().contentEquals(firstName);
    }

    private static int checkOption(String option) {
        if (option.isEmpty()) {
            System.out.println("Please enter a value.");
        }
        try {
            //checks if choice is a number
            return Integer.parseInt(option);
        } catch (NumberFormatException ex) {
            System.out.print("Please enter a numerical value. ");
        }
        // returns -1 otherwise
        return -1;
    }

    private void displayBookInfo(Book b) {
        System.out.println("Title: " + b.getTitle());
        System.out.println("Author: " + b.getAuthor());
        System.out.println("Cover: " + b.getCover());
    }


}
