// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

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
    }

    private static void addBook() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Please enter the following information:");
        String title;
        String author;
        System.out.println("1. Title?");
        title = sc.nextLine();
        System.out.println("2. Author?");
        author = sc.nextLine();
        System.out.println("3. Cover image?");
        System.out.println("4. Save? Enter 'Y' or 'N'.");
    }

    private static void findBook() {
        System.out.println("How would you like to find the book?");
    }

    private static void deleteBook() {
        System.out.println("Enter the title of the book to delete: ");
    }

    private static int checkOption(String option) {
        int choice = -1;
        if (option.isEmpty()) {
            System.out.println("Please enter a value.");
        }
        try {
            choice = Integer.parseInt(option);
        } catch (NumberFormatException ex) {
            System.out.print("Please enter a numerical value. ");
        }
        return choice;
    }
}



