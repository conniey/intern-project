// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;


/**
 * A library application that keeps track of books using Azure services.
 */
public class App {

    private static final int INVALID = -1;
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final AtomicReference<List<Book>> arBooks = new AtomicReference<>();
    private static final OptionChecker optCheck = new OptionChecker();
    private static final FileCollector fileC = new FileCollector();

    /**
     * Starting point for the library application.
     *
     * @param args Arguments to the library program.
     */
    public static void main(String[] args) {
        System.out.print("Welcome! ");
        int choice;
        do {
            showMenu();
            String option = SCANNER.nextLine();
            choice = optCheck.checkOption(option, 5);
            switch (choice) {
                case 1:
                    listBooks().block();
                    break;
                case 2:
                    addBook();
                    break;
                case 3:
                    findBook();
                    break;
                case 4:
                    deleteBook().block();
                    break;
                case 5:
                    System.out.println("Goodbye.");
                    break;
                default:
                    System.out.println("Please try again.");
                    break;
            }
            System.out.println("------------------------------------------------");
        } while (choice != 5);
    }

    private static void showMenu() {
        System.out.println("Select one of the options  below (1 - 5).");
        System.out.println("1. List books");
        System.out.println("2. Add a book");
        System.out.println("3. Find a book");
        System.out.println("4. Delete book");
        System.out.println("5. Quit");
    }

    public static Mono<Void> listBooks() {
        Flux<Book> book = fileC.registerBooks();
        return book.collectList().map(list -> {
            if (list == null) {
                arBooks.set(Collections.emptyList());
                System.out.println("There are no books.");
                return list;
            }
            System.out.println("Here are all the books you have: ");
            arBooks.set(list);
            for (int i = 0; i < list.size(); i++) {
                Book book1 = list.get(i);
                System.out.println(i + 1 + ". " + book1);
            }
            return list;
        }).then();
    }

    /**
     * Goes through all the JSON files and registers their information as Book objects before storing it in a Flux.
     *
     * @return Flux filled with all the books in the JSON files
     */
    private static void addBook() {
        System.out.println("Please enter the following information:");
        String title;
        String author;
        File path;
        do {
            System.out.println("1. Title?");
            title = SCANNER.nextLine();
        } while (!optCheck.validateString(title));
        do {
            System.out.println("2. Author?");
            author = SCANNER.nextLine();
        } while (!optCheck.validateAuthor(author.split(" ")));
        String[] authorName = parseAuthorsName(author.split(" "));
        Author newAuthor = new Author(authorName[0], authorName[1]);
        do {
            System.out.println("3. Cover image?");
            path = new File(SCANNER.nextLine());
        } while (!optCheck.checkImage(path));
        String choice;
        do {
            System.out.println("4. Save? Enter 'Y' or 'N'.");
            choice = SCANNER.nextLine();
        } while (optCheck.checkYesOrNo(choice));
        fileC.saveBook(title, newAuthor, path, choice);
    }

    private static void findBook() {
        int choice;
        System.out.println("How would you like to find the book?");
        do {
            System.out.println("1. Search by book title?");
            System.out.println("2. Search by author?");
            String option = SCANNER.nextLine();
            choice = optCheck.checkOption(option, 2);
        } while (choice == INVALID);
        switch (choice) {
            case 1:
                findTitle();
                break;
            case 2:
                findAuthor();
                break;
            default:
                System.out.println("Please enter a number between 1 or 2.");
        }
    }

    private static void findTitle() {
        FindBook findBook = new FindBook(fileC.registerBooks());
        System.out.println("What is the book title?");
        String title = SCANNER.nextLine();
        int results = findBook.searchByTitle(title);
        switch (results) {
            case 0:
                System.out.println("There are no books with that title.");
                break;
            case 1:
                System.out.println("Here is a book titled " + title);
                findBook.onlyOneResult(1).block();
                System.out.print("Would you like to view it? ");
                String yesOrNo = getYesOrNo();
                if (yesOrNo.equalsIgnoreCase("y")) {
                    findBook.onlyOneResult(2).block();
                }
                break;
            case 2:
                System.out.println("Here are the books titled " + title + ". Please enter the number you wish to view.");
                findBook.manyResults(0).block();
                int choice;
                do {
                    String option = SCANNER.nextLine();
                    choice = optCheck.checkOption(option, (int) findBook.getSize());
                } while (choice == INVALID);
                findBook.manyResults(choice).block();
                break;
        }
    }

    private static void findAuthor() {
        FindBook findBook = new FindBook(fileC.registerBooks());
        System.out.println("What is the author's full name?");
        String author = SCANNER.nextLine();
        String[] name = parseAuthorsName(author.split(" "));
        int outcome = findBook.searchByAuthor(name[0], name[1]);
        switch (outcome) {
            case 0:
                System.out.println("There are no books with that title.");
                break;
            case 1:
                System.out.println("Here is a book by " + author);
                findBook.onlyOneResult(1).block();
                System.out.print("Would you like to view it? ");
                String yesOrNo = getYesOrNo();
                if (yesOrNo.equalsIgnoreCase("y")) {
                    findBook.onlyOneResult(2).block();
                }
                break;
            case 2:
                System.out.println("Here are the books by " + author + ". Please enter the number you wish to view.");
                findBook.manyResults(0);
                int choice;
                do {
                    String option = SCANNER.nextLine();
                    choice = optCheck.checkOption(option, (int) findBook.getSize() - 1);
                } while (choice == INVALID);
                findBook.manyResults(choice);
                break;
        }
    }

    private static Mono<Void> deleteBook() {
        DeleteBook deleteBook = new DeleteBook();
        System.out.println("Enter the title of the book to delete: ");
        Flux<Book> booksToDelete = deleteBook.lookupTitle(SCANNER.nextLine());
        return booksToDelete.collectList().map(list -> {
            if (list == null || list.isEmpty()) {
                arBooks.set(Collections.emptyList());
                System.out.println("There are no books with that title.");
                return list;
            }
            arBooks.set(list);
            if (list.size() == 1) {
                System.out.println("Here is a matching book. Would you like to delete it? Enter Y or N.");
                Book b = list.get(0);
                System.out.println("- " + b);
            } else {
                System.out.println("Here are matching books. Enter the number to delete: ");
                deleteBook.displayResults().block();
            }
            int choice;
            do {
                String option = SCANNER.nextLine();
                choice = optCheck.checkOption(option, (int) deleteBook.getSize());
            } while (choice == INVALID);
            System.out.println("Delete \"" + list.get(choice - 1) + "\"? Enter Y or N.");
            String delete = SCANNER.nextLine();
            if (delete.equalsIgnoreCase("y")) {
                deleteBook.deleteFile(new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib").listFiles(),
                    list.get(choice - 1));
            }
            deleteBook.deleteEmptyDirectories();
            return list;
        }).then();
    }

    private static String getYesOrNo() {
        System.out.println("Enter Y or N.");
        String yesOrNo;
        do {
            yesOrNo = SCANNER.nextLine();
        } while (optCheck.checkYesOrNo(yesOrNo));
        return yesOrNo;
    }

    private static String[] parseAuthorsName(String author[]) {
        String lastName = author[author.length - 1];
        String firstName = author[0];
        for (int i = 1; i < author.length - 1; i++) {
            firstName += " " + author[i];
        }
        return new String[]{firstName, lastName};
    }
}

// TODO: provide them some form of escape...
// AZURE! Doesn't work. -- Working on it
