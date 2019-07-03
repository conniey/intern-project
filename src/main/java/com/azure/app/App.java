// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A library application that keeps track of books using Azure services.
 */
public class App {

    private static final int INVALID = -1;
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final AtomicReference<List<Book>> arBooks = new AtomicReference<>();
    private static final OptionChecker optCheck = new OptionChecker();

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
        Flux<Book> book = registerBooks();
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
    public static Flux<Book> registerBooks() {
        Flux<Book> savedBook = Flux.empty();
        try (Stream<Path> walk = Files.walk(Paths.get("C:\\Users\\t-katami\\Documents\\intern-project\\lib"))) {
            List<String> result = walk.map(x -> x.toString()).filter(f -> f.endsWith(".json")).collect(Collectors.toList());
            if (result.isEmpty() || result == null) {
                return Flux.empty();
            }
            savedBook = Flux.create(bookFluxSink ->
            {
                for (int i = 0; i < result.size(); i++) {
                    bookFluxSink.next(new BookDeserialize().fromJSONtoBook(new File(result.get(i))));
                }
                bookFluxSink.complete();

            }, FluxSink.OverflowStrategy.BUFFER);
        } catch (
            IOException e) {
            e.printStackTrace();
        }
        return savedBook;
    }

    public static void addBook() {
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
        do {
            System.out.println("3. Cover image?");
            path = new File(SCANNER.nextLine());
        } while (!optCheck.checkImage(path));
        String choice;
        do {
            System.out.println("4. Save? Enter 'Y' or 'N'.");
            choice = SCANNER.nextLine();
        } while (optCheck.checkYesOrNo(choice));
        saveBook(title, author, path, choice);
    }

    private static void saveBook(String title, String author, File path, String choice) {
        if (choice.equalsIgnoreCase("Y")) {
            String[] name = parseAuthorsName(author.split(" "));
            Author savedAuthor = new Author(name[0], name[1]);
            Mono<Book> book = Mono.just(new Book(title, savedAuthor, path));
            BookSerializer serializer = new BookSerializer();
            book.subscribe(x -> {
                if (serializer.writeJSON(x)) {
                    System.out.println("Book was successfully saved!");
                } else {
                    System.out.println("Error. Book wasn't saved.");
                }
            });
        }
    }

    private static void findBook() {
        int choice;
        FindBook findBook = new FindBook(registerBooks());
        System.out.println("How would you like to find the book?");
        do {
            System.out.println("1. Search by book title?");
            System.out.println("2. Search by author?");
            String option = SCANNER.nextLine();
            choice = optCheck.checkOption(option, 2);
        } while (choice == INVALID);
        switch (choice) {
            case 1:
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
                        break;
                }
                break;
            case 2:
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

                        break;
                }
                break;
            default:
                System.out.println("Please enter a number between 1 or 2.");
        }
    }

    public static Mono<Void> deleteBook() {
        DeleteBook deleteBook = new DeleteBook();
        System.out.println("Enter the title of the book to delete: ");
        FindBook findBook = new FindBook(registerBooks());
        Flux<Book> booksToDelete = findBook.findTitles(SCANNER.nextLine());
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
                System.out.println(b);
            }
            String delete;
            int max = 1;
            System.out.println("Here are matching books. Enter the number to delete: ");
            for (int i = 0; i < list.size(); i++) {
                System.out.println(i + 1 + ". " + list.get(i));
                max++;
            }
            int choice;
            do {
                String option = SCANNER.nextLine();
                choice = optCheck.checkOption(option, max);
            } while (choice == INVALID);
            System.out.println("Delete \"" + list.get(choice - 1) + "\"? Enter Y or N.");
            delete = SCANNER.nextLine();
            if (delete.contentEquals("Y") || delete.contentEquals("y")) {
                deleteBook.deleteFile(new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib").listFiles(),
                    list.get(choice - 1));
            }
            list.remove(choice - 1);
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

// Would you decompose App class more? Like break up deleteBook into its own class? --YES, break everything up...
// Should I provide the users with an escape? (Like What if they don't want to find a book, can I add 0 head back?)
// TODO: provide them some form of escape...
// AZURE! Doesn't work. -- Working on it
