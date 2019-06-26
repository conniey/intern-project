// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

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
        System.out.println("Hello World!");
        for (int i = 1; i < 11; i++) {
            System.out.println(i);
        }
        System.out.println("Goodbye");
    }
}
