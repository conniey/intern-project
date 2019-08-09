// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.data.appconfiguration.credentials.ConfigurationClientCredentials;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Scanner;

/**
 * A library application that keeps track of books using Azure services.
 */
public class App {
    private static final int INVALID = -1;
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final OptionChecker OPTION_CHECKER = new OptionChecker();
    private static BookCollector bookCollector;
    private static final Logger logger = LoggerFactory.getLogger(App.class);

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
        if (!setBookCollector(connectionString) || bookCollector == null) {
            return;
        }
        System.out.print("Welcome! ");
        int choice;
        do {
            showMenu();
            String option = SCANNER.nextLine();
            choice = OPTION_CHECKER.checkOption(option, 6);
            switch (choice) {
                case 1:
                    listBooks().block();
                    break;
                case 2:
                    final Mono<String> savedBookMono = addBook()
                        .onErrorResume(error -> Mono.just("Book wasn't saved. Error:" + error.toString()));
                    final String description = savedBookMono.block();
                    assert description != null;
                    if (!description.isEmpty()) {
                        System.out.println("Status: " + description);
                    }
                    break;
                case 3:
                    System.out.println(edit().block());
                    break;
                case 4:
                    if (bookCollector.hasBooks().block()) {
                        System.out.print(findBook().block());
                    } else {
                        System.out.println("There are no books to find.");
                    }
                    break;
                case 5:
                    if (bookCollector.hasBooks().block()) {
                        System.out.println(deleteBook().block());
                    } else {
                        System.out.println("There are no books to delete");
                    }
                    break;
                case 6:
                    System.out.println("Goodbye.");
                    break;
                default:
                    System.out.println("Please try again.");
                    break;
            }
            System.out.println("------------------------------------------------");
        } while (choice != 6);
    }

    /**
     * Sets up the BookCollector with the document and image storage.
     *
     * @param connectionString - the value from App Configuration to determine where to store the text and image files
     * @return - boolean that determines whether the setup was successful
     * true - successful
     * false - unsuccessful
     */
    private static boolean setBookCollector(String connectionString) {
        ConfigurationAsyncClient client;
        try {
            client = new ConfigurationClientBuilder()
                .credential(new ConfigurationClientCredentials(connectionString))
                .httpLogDetailLevel(HttpLogDetailLevel.HEADERS)
                .buildAsyncClient();
            DocumentProvider document = selectDocumentProvider(client);
            if (document == null) {
                return false;
            }
            ImageProvider imageProvider;
            try {
                imageProvider = selectImageProvider(client);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.err.println("Could not set up image storage provider. Please check your settings: " + e.getMessage());
                logger.error("Error couldn't set up Image Provider: ", e);
                return false;
            }
            bookCollector = new BookCollector(document, imageProvider);
            return true;
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            logger.error("Exception with App Configuration: ", e);
            return false;
        }
    }

    private static DocumentProvider selectDocumentProvider(ConfigurationAsyncClient client) {
        final ObjectMapper mapper = new ObjectMapper();
        String documentProvider = client.getSetting("DOCUMENT_STORAGE_TYPE").map(ConfigurationSetting::value).block();
        assert documentProvider != null;
        if (documentProvider.equalsIgnoreCase("Cosmos")) {
            CosmosSettings cosmosInfo = client.getSetting("COSMOS_INFO").map(info -> {
                try {
                    return (mapper.readValue(info.value(), CosmosSettings.class));
                } catch (IOException e) {
                    logger.error("Invalid information for document storage: ", e);
                    return null;
                }
            }).block();
            if (cosmosInfo == null) {
                return null;
            }
            return new CosmosDocumentProvider(cosmosInfo);
        } else {
            return new LocalDocumentProvider(System.getProperty("user.dir"));
        }
    }

    private static ImageProvider selectImageProvider(ConfigurationAsyncClient client) {
        final ObjectMapper mapper = new ObjectMapper();
        return client.getSetting("IMAGE_STORAGE_TYPE").flatMap(input -> {
            String storageType = input.value();
            if (storageType.equalsIgnoreCase("Local")) {
                return Mono.just(new LocalImageProvider(System.getProperty("user.dir")));
            } else if (storageType.equalsIgnoreCase("BlobStorage")) {
                return client.getSetting("BLOB_INFO").flatMap(info -> {
                    try {
                        return Mono.just(new BlobImageProvider(mapper.readValue(info.value(), BlobSettings.class)));
                    } catch (IOException e) {
                        logger.error("Invalid information: ", e);
                        return Mono.error(new IllegalStateException("Environment variable COSMOS_INFO is not set properly."));
                    }
                });
            } else {
                return Mono.error(new IllegalArgumentException("Image storage type '" + storageType + "' is not recognized."));
            }
        }).block();
    }

    private static Mono<String> edit() {
        Mono<Book> editBook = grabBook("edit");
        return editBook.flatMap(oldBook -> {
            if (oldBook.getTitle() == null) {
                return Mono.just("");
            }
            System.out.println("What would you like to change?");
            System.out.println("1. Title?");
            System.out.println("2. Author?");
            System.out.println("3. Cover?");
            Book newBook;
            int editAspect;
            do {
                String option = SCANNER.nextLine();
                editAspect = OPTION_CHECKER.checkOption(option, 3);
            } while (editAspect == INVALID);
            switch (editAspect) {
                case 1:
                    String newTitle;
                    do {
                        System.out.println("New Title?");
                        newTitle = SCANNER.nextLine();
                    } while (!OPTION_CHECKER.validateString(newTitle));
                    newBook = new Book(newTitle, oldBook.getAuthor(), oldBook.getCover());
                    return bookCollector.editBook(oldBook, newBook, 0)
                        .then(Mono.just("Book was changed."))
                        .onErrorResume(error -> Mono.just("Book wasn't changed. Error:" + error.toString()));
                case 2:
                    String author;
                    do {
                        System.out.println("New Author?");
                        author = SCANNER.nextLine();
                    } while (!OPTION_CHECKER.validateAuthor(author.split(" ")));
                    String[] authorName = parseAuthorsName(author.split(" "));
                    Author newAuthor = new Author(authorName[0], authorName[1]);
                    newBook = new Book(oldBook.getTitle(), newAuthor, oldBook.getCover());
                    return bookCollector.editBook(oldBook, newBook, 0).then(Mono.just(
                        "Book was changed."));
                case 3:
                    URI newPath;
                    do {
                        System.out.println("New Cover (.gif, .jpg, or .png format)?");
                        String filePath = SCANNER.nextLine();
                        newPath = bookCollector.retrieveURI(filePath);
                    } while (!OPTION_CHECKER.checkImage(newPath));
                    newBook = new Book(oldBook.getTitle(), oldBook.getAuthor(), newPath);
                    return bookCollector.editBook(oldBook, newBook, 1).then(Mono.just("Book was changed"));
                default:
                    return Mono.just("");
            }
        });
    }

    private static void showMenu() {
        System.out.println("Select one of the options below (1 - 6).");
        System.out.println("1. List books");
        System.out.println("2. Add a book");
        System.out.println("3. Edit a book");
        System.out.println("4. Find a book");
        System.out.println("5. Delete book");
        System.out.println("6. Quit");
    }

    private static Mono<Void> listBooks() {
        Flux<Book> book = bookCollector.getBooks();
        return book.collectList().map(list -> {
            if (list.isEmpty()) {
                System.out.println("There are no books.");
                return list;
            }
            System.out.println("Here are all the books you have: ");
            for (int i = 0; i < list.size(); i++) {
                Book book1 = list.get(i);
                System.out.println(i + 1 + ". " + book1);
            }
            return list;
        }).then();
    }

    private static Mono<String> addBook() {
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
            System.out.println("3. Cover image (.gif, .jpg, or .png format)? (Enter \"Q\" to return to menu.)");
            String filePath = SCANNER.nextLine();
            if (filePath.equalsIgnoreCase("Q")) {
                return Mono.just("");
            }
            path = bookCollector.retrieveURI(filePath);
        } while (!OPTION_CHECKER.checkImage(path));
        System.out.print("4. Save? ");
        choice = getYesOrNo();
        if (choice.equalsIgnoreCase("y")) {
            Book newBook = new Book(title, newAuthor, path);
            return bookCollector.saveBook(newBook).then(Mono.just("Book was successfully saved."));
        }
        return Mono.just("");
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
                System.out.printf("There are no books %s.\n", option.contentEquals("title") ? "with that title"
                    : "by that author");
            } else if (list.size() == 1) {
                System.out.printf("Here is a book %s %s.\n", option.contentEquals("title") ? "titled"
                    : "by", input);
                System.out.println(" * " + list.get(0));
                System.out.println("Would you like to view it?");
                String choice = getYesOrNo();
                if (choice.equalsIgnoreCase("y")) {
                    return bookCollector.grabCoverImage(list.get(0)).map(cover ->
                        list.get(0).displayBookInfo(cover)
                    );
                }
            } else {
                System.out.printf("Here are books %s %s. Please enter the number you wish to view."
                    + " (Enter \"Q\" to return to menu.)\n", option.contentEquals("title") ? "titled"
                    : "by", input);
                int choice = getBook(list);
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

    private static Mono<String> deleteBook() {
        return grabBook("delete").flatMap(book -> {
            if (book.getTitle() == null) {
                return Mono.just("");
            }
            return bookCollector.deleteBook(book)
                .then(Mono.just("Book was deleted."))
                .onErrorResume(error -> Mono.just("Error. Book wasn't deleted."));
        });
    }

    private static Mono<Book> grabBook(String modifier) {
        System.out.printf("Please enter the title of the book %s: ", modifier.contentEquals("delete")
            ? "to delete" : "to edit");
        Flux<Book> booksToDelete = bookCollector.findBook(SCANNER.nextLine());
        return booksToDelete.collectList().flatMap(list -> {
            if (list.isEmpty()) {
                System.out.println("There are no books with that title.");
                return Mono.empty();
            }
            if (list.size() == 1) {
                System.out.println("Here is a matching book.");
                System.out.println(" * " + list.get(0));
                System.out.printf("Would you like to %s it? ", modifier.contentEquals("delete")
                    ? "delete" : "edit");
                String choice = getYesOrNo();
                if (choice.equalsIgnoreCase("Y")) {
                    return Mono.just(list.get(0));
                }
            } else {
                System.out.printf("Here are matching books. Enter the number to %s :  (Enter \"Q\" to return to menu.) ",
                    modifier.contentEquals("delete") ? "to delete" : "to edit");
                int choice = getBook(list);
                if (choice != 0 && modifier.contentEquals("delete")) {
                    System.out.println("Delete \"" + list.get(choice - 1) + "\"? Enter Y or N.");
                    String delete = SCANNER.nextLine();
                    if (delete.equalsIgnoreCase("y")) {
                        return Mono.just(list.get(choice - 1));
                    }
                } else if (modifier.contentEquals("edit")) {
                    return Mono.just(list.get(choice - 1));
                }
            }
            return Mono.empty();
        });
    }

    private static int getBook(List<Book> allBooks) {
        for (int i = 0; i < allBooks.size(); i++) {
            System.out.println(i + 1 + ". " + allBooks.get(i));
        }
        int choice;
        do {
            String option = SCANNER.nextLine();
            choice = OPTION_CHECKER.checkOption(option, allBooks.size());
        } while (choice == INVALID);
        return choice;
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
