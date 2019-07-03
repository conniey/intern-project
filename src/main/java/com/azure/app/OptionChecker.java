// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import java.io.File;

public class OptionChecker {

    /**
     * Checks the user's string and makes sure its valid.
     *
     * @param input - String the user enteredd
     * @return -  boolean : true - the String is valid
     * false - the String isn't valid
     */
    public boolean validateString(String input) {
        if (input.isEmpty()) {
            System.out.println("Please enter a value: ");
            return false;
        }
        boolean onlySpaces = true;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) != ' ') {
                onlySpaces = false;
            }
        }
        return !onlySpaces;
    }

    /**
     * Verifies the image path to make sure its valid and right
     * Checks the extension to make sure its png., jpg., gif.
     *
     * @param image - File with the supposed image
     * @return - boolean : true - if image path is correct
     *                     false - otherwise.
     */
    public boolean checkImage(File image) {
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

    /**
     * Verifies the user's input to make sure its either y/n
     * @param choice - String containing a choice
     * @return - boolean : true - the String is either a y/n
     *                     false - the String is neither
     */
    public boolean checkYesOrNo(String choice) {
        if (!choice.contentEquals("Y") && !choice.contentEquals("y")
            && !choice.contentEquals("N") && !choice.contentEquals("n")) {
            System.out.println("Please enter Y or N.");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Verifies the author's name to make sure there's a first and last name.
     * @param author - String with the author's name
     * @return boolean : true - if String is valid
     *                   false - if String isn't valid
     */
    public boolean validateAuthor(String[] author) {
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

    /**
     * Checks the file to see if its contents match the book and that it's an actual json file.
     * @param f - File that needs to be checked
     * @param b - the Book object that it's being compared to
     * @return
     */
    public boolean checkFile(File f, Book b) {
        if (f == null) {
            return false;
        }
        return f.getAbsolutePath().contains(b.getAuthor().getFirstName())
            && f.getAbsolutePath().contains(b.getAuthor().getLastName())
            && f.getAbsolutePath().endsWith(".json");
    }

    /**
     * Checks the String option and parses it to an integer if it fits within the desired range or is actually a numeral
     * @param option - String that the user picked
     * @param max - a long containing the maximum number of options
     * @return - valid integer option, or else -1 signifies that the choice was invalid
     */
    public int checkOption(String option, long max) {
        if (option.equalsIgnoreCase("q")) {
            return 0;
        }
        if (option.isEmpty()) {
            System.out.println("Enter a value.");
        } else {
            try {
                //checks if choice is a number
                int choice = Integer.parseInt(option);
                if (choice > max || choice < 1) {
                    System.out.println("Enter a number from 1-" + max + ".");
                } else {
                    return choice;
                }
            } catch (NumberFormatException ex) {
                System.out.println("Enter a numerical value. ");
            }
        }
        // returns -1 which corresponds as INVALID in the App main
        return -1;
    }

}
