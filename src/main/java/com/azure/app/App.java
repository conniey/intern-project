// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Scanner;

/**
 * A library application that keeps track of books using Azure services.
 */
public class App {
    /**
     * Starting point for the library application.
     *
     * @param args Arguments to the library program.
     */
    public static void main(String[] args) {
        System.out.print("Welcome! ");
        Scanner sc = new Scanner(System.in);
        int choice;
        do {
            showMenu();
            String option = sc.nextLine();
            choice = checkOption(option);
            switch (choice) {
                case 1:
                    listBooks();
                    break;
                case 2:
                    addBook();
                    break;
                case 3:
                    findBook();
                    break;
                case 4:
                    deleteBook();
                    break;
                case 5:
                    System.out.println("Goodbye.");
                    break;
                default:
                    System.out.println("Please try again.");
            }
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

    //static variable for creating lists in lamda expressions
    private static int numberIndex = 1;

    private static void listBooks() {
        Flux<Book> book = registerBooks();
        System.out.println("Here are all the books you have: ");
        try {
            book.subscribe(x -> System.out.println(increment() + ". " + x));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        numberIndex = 1;
    }

    private static String increment() {
        return "" + numberIndex++;
    }

    /*
     * Goes through all the JSON files and registers their information as Book objects
     *
     */
    private static Flux<Book> registerBooks() {
        File[] files = new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib").listFiles();
        Flux<Book> savedBook = Flux.empty();
        for (int i = 0; i < files.length; i++) {
            File[] file = files[i].listFiles();
            File[] exploreFiles = files[i].listFiles();
            for (File innerFile : file) {
                if (innerFile.isDirectory()) {
                    exploreFiles = innerFile.listFiles();
                }
                for (File readFile : exploreFiles) {
                    savedBook = savedBook.concatWithValues(new BookDeserialize().fromJSONtoBook(readFile));
                }
            }
        }
        return savedBook;
    }

    private static void addBook() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Please enter the following information:");
        String title;
        String author;
        File path;
        do {
            System.out.println("1. Title?");
            title = sc.nextLine();
        } while (!validateString(title));
        do {
            System.out.println("2. Author?");
            author = sc.nextLine();
        } while (!validateAuthor(author.split(" ")));
        do {
            System.out.println("3. Cover image?");
            path = new File(sc.nextLine());
        } while (!checkImage(path));
        String choice;
        do {
            System.out.println("4. Save? Enter 'Y' or 'N'.");
            choice = sc.nextLine();
        } while (choice.contentEquals("Y") && choice.contentEquals("y")
            && choice.contentEquals("N") && choice.contentEquals("n"));
        saveBook(title, author, path, choice);
    }

    private static void saveBook(String title, String author, File path, String choice) {
        if (choice.contentEquals("Y") || choice.contentEquals("y")) {
            String[] authorName = author.split(" ");
            String lastName = authorName[authorName.length - 1];
            String firstName = authorName[0];
            for (int i = 1; i < authorName.length - 1; i++) {
                firstName += " " + authorName[i];
            }
            Author savedAuthor = new Author(lastName, firstName);
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

    public static boolean validateAuthor(String[] author) {
        if (author.length == 0) {
            System.out.println("Please enter their name.");
            return false;
        }
        if (author.length < 2) {
            System.out.println("Please enter their first and last name");
            return false;
        }
        return true;
    }

    private static void findBook() {
        Scanner sc = new Scanner(System.in);
        int choice;
        FindBook findBook = new FindBook(registerBooks());
        System.out.println("How would you like to find the book?");
        do {
            System.out.println("1. Search by book title?");
            System.out.println("2. Search by author?");
            String option = sc.nextLine();
            choice = checkOption(option);
        } while (choice == -1);
        switch (choice) {
            case 1:
                findBook.searchByTitle();
                break;
            case 2:
                findBook.searchByAuthor();
                break;
            default:
                System.out.println("Please enter a number between 1 or 2.");
        }
        numberIndex = 1;
    }

    private static void deleteBook() {
        System.out.println("Enter the title of the book to delete: ");
        Scanner sc = new Scanner(System.in);
        Flux<Book> booksToDelete = new FindBook(registerBooks()).findTitles(sc.nextLine());
        String delete;
        Mono<Book> deletedBook;
        do {
            System.out.println("Here are matching books. Enter the number to delete: ");
            booksToDelete.subscribe(x -> System.out.println(increment() + ". " + x));
            numberIndex = 1;
            int choice;
            do {
                String option = sc.nextLine();
                choice = checkOption(option);
            } while (choice != 1);
            deletedBook = booksToDelete.elementAt(choice - 1);
            deletedBook.subscribe(x ->
                System.out.println("Delete \"" + x + "\"? Enter Y or N"));
            delete = sc.nextLine();
        } while (delete.contentEquals("Y") && delete.contentEquals("y")
            && delete.contentEquals("N") && delete.contentEquals("n"));
        if (delete.contentEquals("Y") || delete.contentEquals("y"))
            deleteFile(new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib").listFiles(),
                deletedBook);
    }

    private static void deleteBookFile(String choice, Mono<Book> book) {

    }

    private static void deleteFile(File[] files, Mono<Book> book) {
        for (File file : files) {
            if (file.isDirectory()) {
                deleteFile(file.listFiles(), book);
            } else {
                book.subscribe(x -> {
                    if (checkFile(file, x)) {
                        if (file.delete()) {
                            System.out.println("Book is deleted.");
                        }
                    }
                });
            }
        }
    }

    private static boolean checkFile(File f, Book b) {
        return f.getAbsolutePath().contains(b.getAuthor().getFirstName())
            && f.getAbsolutePath().contains(b.getAuthor().getLastName())
            && f.getAbsolutePath().contains(b.getTitle() + ".json");
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

    private static boolean validateString(String input) {
        if (input.isEmpty()) {
            System.out.println("Please enter a value: ");
            return false;
        }
        return true;
    }

    private static boolean checkImage(File image) {
        if (!image.isFile()) {
            System.out.println("Please write out a valid image file path.");
            return false;
        }
        //Parses the File path to examine its extension
        String extension = image.getAbsolutePath();
        extension = extension.substring(extension.lastIndexOf('.'));
        //only returns true if it has the required extension
        if (extension.contentEquals(".jpg") || (extension.contentEquals(".png"))
            || (extension.contentEquals(".gif"))) {
            return true;
        }
        System.out.println("Please pick an image file.");
        return false;
    }

    private void getBookInfo(Book b) {

    }
}

