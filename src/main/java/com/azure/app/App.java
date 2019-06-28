// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import java.io.File;
import java.io.IOException;
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

    private static void listBooks() {
        System.out.println("Here are your list of books:");
        BookDeserialize bd = new BookDeserialize();
        bd.iterateThroughLibrary();
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
        } while (!validateString(author));
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
            Book book = new Book(title, savedAuthor, path);
            BookSerializer serializer = new BookSerializer();
            System.out.println("---------------------------");
            try {
                serializer.toJsonString(book);
            } catch (IOException e) {
                System.out.println("Uh -oh");
            }
            if (serializer.writeJSON(book)) {
                System.out.println("Book was successfully saved!");
            } else {
                System.out.println("Error. Book wasn't saved.");
            }
        }
    }

    private static void findBook() {
        System.out.println("How would you like to find the book?");
    }

    private static void deleteBook() {
        System.out.println("Enter the title of the book to delete: ");
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
        // returns an arbitrary number otherwise
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
}

