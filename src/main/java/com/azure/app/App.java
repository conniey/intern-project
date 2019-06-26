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
        showMenu();
        Scanner sc = new Scanner(System.in);
        int option = sc.nextInt();
        while (option != 5) {
            switch (option) {
                case 1: {
                    listBooks();
                    break;
                }
                case 2: {
                    addBook();
                    break;
                }
                case 3: {
                    findBook();
                    break;
                }
                case 4: {
                    deleteBook();
                    break;
                }
            }
            showMenu();
            option = sc.nextInt();
        }
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
        System.out.println("Please enter the following information:");
    }

    private static void findBook() {
        System.out.println("How would you like to find the book?");
    }

    private static void deleteBook() {
        System.out.println("Enter the title of the book to delete: ");
    }
}
