package com.azure.app;

import java.io.File;

public class OptionChecker {

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

    public boolean checkYesOrNo(String choice) {
        if (!choice.contentEquals("Y") && !choice.contentEquals("y")
            && !choice.contentEquals("N") && !choice.contentEquals("n")) {
            System.out.println("Please enter Y or N.");
            return true;
        } else {
            return false;
        }
    }

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

    public boolean checkFile(File f, Book b) {
        if (f == null) {
            return false;
        }
        return f.getAbsolutePath().contains(b.getAuthor().getFirstName())
            && f.getAbsolutePath().contains(b.getAuthor().getLastName())
            && f.getAbsolutePath().endsWith(".json");
    }


    public int checkOption(String option, int max) {
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
