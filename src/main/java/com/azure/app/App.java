// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.credentials.ConfigurationClientCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
    private static final AtomicReference<List<Book>> AR_REFERENCE = new AtomicReference<>();
    private static final OptionChecker OPTION_CHECKER = new OptionChecker();
    private static BookCollection bookCollector;
    private static Logger logger = LoggerFactory.getLogger(JsonHandler.class);

    /**
     * Starting point for the library application.
     *
     * @param args Arguments to the library program.
     */
    public static void main(String[] args) {
        String connectionString = System.getenv("AZURE_APPCONFIG");
        if (connectionString == null || connectionString.isEmpty()) {
            System.err.println("Environment variable AZURE_APPCONFIG is not set. Cannot connect to App Configuration."
                + " Please set it.");
            return;
        }
        ConfigurationAsyncClient client;
        try {
            client = ConfigurationAsyncClient.builder()
                .credentials(new ConfigurationClientCredentials(connectionString))
                .build();
            Mono<BookCollection> bookCollectionMono = client.getSetting("IMAGE_STORAGE_TYPE").flatMap(input -> {
                String storageType = input.value().value();
                if (storageType.equalsIgnoreCase("Local")) {
                    return Mono.just(new LocalBookCollector(System.getProperty("user.dir")));
                } else if (storageType.equalsIgnoreCase("BlobStorage")) {
                    return Mono.just(new BlobBookCollector());
                } else {
                    return Mono.error(new IllegalArgumentException("Image storage type '" + storageType + "' is not recognised."));
                }
            });
            bookCollector = bookCollectionMono.block();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            logger.error("Exception with App Configuration: ", e);
        }
        System.out.print("Welcome! ");
        int choice;
        do {
            showMenu();
            String option = SCANNER.nextLine();
            choice = OPTION_CHECKER.checkOption(option, 5);
            switch (choice) {
                case 1:
                    listBooks().block();
                    break;
                case 2:
                    final Mono<String> savedBookMono = addBook()
                        .then(Mono.just("Book was successfully saved!"))
                        .onErrorResume(error -> Mono.just("Book wasn't saved. Error:" + error.toString()));
                    final String description = savedBookMono.block();
                    System.out.println("Status: " + description);
                    break;
                case 3:
                    boolean notEmpty = bookCollector.hasBooks().block();
                    if (notEmpty) {
                        System.out.print(findBook().block());
                    } else {
                        System.out.println("There are no books to find.");
                    }
                    break;
                case 4:
                    notEmpty = bookCollector.hasBooks().block();
                    if (notEmpty) {
                        deleteBook().block();
                    } else {
                        System.out.println("There are no books to delete");
                    }
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
        System.out.println("Select one of the options below (1 - 5).");
        System.out.println("1. List books");
        System.out.println("2. Add a book");
        System.out.println("3. Find a book");
        System.out.println("4. Delete book");
        System.out.println("5. Quit");
    }

    private static Mono<Void> listBooks() {
        Flux<Book> book = bookCollector.getBooks();
        return book.collectList().map(list -> {
            if (list.isEmpty()) {
                AR_REFERENCE.set(Collections.emptyList());
                System.out.println("There are no books.");
                return list;
            }
            System.out.println("Here are all the books you have: ");
            AR_REFERENCE.set(list);
            for (int i = 0; i < list.size(); i++) {
                Book book1 = list.get(i);
                System.out.println(i + 1 + ". " + book1);
            }
            return list;
        }).then();
    }

    private static Mono<Void> addBook() {
        System.out.println("Please enter the following information:");
        String title;
        String author;
        URI path;
        String choice;
        do {
            System.out.println("1. Title?");
            title = SCANNER.nextLine();
        } while (!OPTION_CHECKER.validateString(title));
        do {
            System.out.println("2. Author?");
            author = SCANNER.nextLine();
        } while (!OPTION_CHECKER.validateAuthor(author.split(" ")));
        String[] authorName = parseAuthorsName(author.split(" "));
        Author newAuthor = new Author(authorName[0], authorName[1]);
        do {
            System.out.println("3. Cover image? (Enter \"Q\" to return to menu.)");
            String filePath = SCANNER.nextLine();
            if (filePath.equalsIgnoreCase("Q")) {
                return null;
            }
            path = bookCollector.retrieveURI(filePath);
        } while (!OPTION_CHECKER.checkImage(System.getProperty("user.dir"), path));
        System.out.print("4. Save? ");
        choice = getYesOrNo();
        if (choice.equalsIgnoreCase("y")) {
            return bookCollector.saveBook(title, newAuthor, path);
        }
        return Mono.empty();
    }

    private static Mono<String> findBook() {
        int choice;
        System.out.println("How would you like to find the book? (Enter \"Q\" to return to menu.)");
        do {
            System.out.println("1. Search by book title?");
            System.out.println("2. Search by author?");
            String option = SCANNER.nextLine();
            choice = OPTION_CHECKER.checkOption(option, 2);
        } while (choice == INVALID);
        switch (choice) {
            case 0:
                break;
            case 1:
                System.out.println("What is the book title?");
                String title = SCANNER.nextLine();
                return find("title", title);
            case 2:
                System.out.println("What is the author's full name?");
                String author = SCANNER.nextLine();
                return find("author", author);
            default:
                System.out.println("Please enter a number between 1 or 2.");
        }
        return Mono.just("");
    }

    private static Mono<String> find(String option, String input) {
        Flux<Book> booksToFind;
        if (option.contentEquals("author")) {
            String[] name = parseAuthorsName(input.split(" "));
            booksToFind = bookCollector.findBook(new Author(name[0], name[1]));
        } else {
            booksToFind = bookCollector.findBook(input);
        }
        return booksToFind.collectList().flatMap(list -> {
            if (list.isEmpty()) {
                System.out.printf("There are no books %s.\n", option.contentEquals("title") ? "with that title" :
                    "by that author");
            } else if (list.size() == 1) {
                System.out.printf("Here is a book %s %s.\n", option.contentEquals("title") ? "titled" :
                    "by", input);
                System.out.println(" * " + list.get(0));
                System.out.println("Would you like to view it?");
                String choice = getYesOrNo();
                if (choice.equalsIgnoreCase("y")) {
                    return bookCollector.grabCoverImage(list.get(0)).map(cover ->
                        list.get(0).displayBookInfo(cover)
                    );
                }
            } else {
                System.out.printf("Here are books %s %s. Please enter the number you wish to view." +
                    " (Enter \"Q\" to return to menu.)\n", option.contentEquals("title") ? "titled" :
                    "by", input);
                for (int i = 0; i < list.size(); i++) {
                    Book book1 = list.get(i);
                    System.out.println(i + 1 + ". " + book1);
                }
                int choice;
                do {
                    String observe = SCANNER.nextLine();
                    choice = OPTION_CHECKER.checkOption(observe, list.size());
                } while (choice == INVALID);
                int bookNum = choice - 1;
                if (choice != 0) {
                    return bookCollector.grabCoverImage(list.get(bookNum)).map(cover ->
                        list.get(bookNum).displayBookInfo(cover)
                    );
                }
            }
            return Mono.just("");
        });
    }

    private static Mono<Void> deleteBook() {
        System.out.println("Enter the title of the book to delete: ");
        Flux<Book> booksToDelete = bookCollector.findBook(SCANNER.nextLine());
        return booksToDelete.collectList().map(list -> {
            if (list.isEmpty()) {
                AR_REFERENCE.set(Collections.emptyList());
                System.out.println("There are no books with that title.");
                return list;
            }
            AR_REFERENCE.set(list);
            if (list.size() == 1) {
                System.out.println("Here is a matching book.");
                System.out.println(" * " + list.get(0));
                System.out.print("Would you like to delete it? ");
                String choice = getYesOrNo();
                if (choice.equalsIgnoreCase("Y")) {
                    deleteBookHelper(list.get(0));
                }
            } else {
                System.out.println("Here are matching books. Enter the number to delete :  (Enter \"Q\" to return to menu.) ");
                for (int i = 0; i < list.size(); i++) {
                    System.out.println(i + 1 + ". " + list.get(i));
                }
                int choice;
                do {
                    String option = SCANNER.nextLine();
                    choice = OPTION_CHECKER.checkOption(option, list.size());
                } while (choice == INVALID);
                if (choice != 0) {
                    System.out.println("Delete \"" + list.get(choice - 1) + "\"? ");
                    String delete = getYesOrNo();
                    if (delete.equalsIgnoreCase("y")) {
                        deleteBookHelper(list.get(choice - 1));
                    }
                }
            }
            return list;
        }).then();
    }

    private static void deleteBookHelper(Book b) {
        if (b.checkBook()) {
            bookCollector.deleteBook(b).subscribe(bookDeleted -> {
                if (bookDeleted) {
                    System.out.println("Book is deleted.");
                } else {
                    System.out.println("Error. Book wasn't deleted.");
                }
            });
        }
    }

    private static String getYesOrNo() {
        System.out.println("Enter 'Y' or 'N'.");
        String yesOrNo;
        do {
            yesOrNo = SCANNER.nextLine();
        } while (OPTION_CHECKER.checkYesOrNo(yesOrNo));
        return yesOrNo;
    }

    private static String[] parseAuthorsName(String[] author) {
        String lastName = author[author.length - 1];
        StringBuilder firstName = new StringBuilder(author[0]);
        for (int i = 1; i < author.length - 1; i++) {
            firstName.append(" ").append(author[i]);
        }
        return new String[]{firstName.toString(), lastName};
    }
}
