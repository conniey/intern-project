// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OptionCheckerTest {
    private OptionChecker optionChecker = new OptionChecker();

    /**
     * Makes sure that the correct images are saved.
     */
    @Test
    public void testCheckImage() {
        //Good png picture
        URL folder = App.class.getClassLoader().getResource(".");
        File path = new File(folder.getPath() + "Wonder.png");
        assertTrue(optionChecker.checkImage(path));
        //Good jpg
        path = new File(folder.getPath() + "KK8.jpg");
        assertTrue(optionChecker.checkImage(path));
        //Good gif
        path = new File(folder.getPath() + "GreatGatsby.gif");
        assertTrue(optionChecker.checkImage(path));
        //Random file
        path = new File(folder.getPath() + "Test.docx");
        assertFalse(optionChecker.checkImage(path));
    }

    /**
     * Makes sure that the author's name contains at least a first and last name
     */
    @Test
    public void testAuthorCheck() {
        //Author with only one name;
        String name = "OnlyFirstName";
        assertFalse(optionChecker.validateAuthor(name.split(" ")));
        //Author with two name
        name = "First Last";
        assertTrue(optionChecker.validateAuthor(name.split(" ")));
        //Author with multiple name
        name = "First And Last";
        assertTrue(optionChecker.validateAuthor(name.split(" ")));
    }

    /**
     * Tests to make sure it takes in correct options
     */
    @Test
    public void testOptions() {
        int result;
        String option = "2";
        //Good option
        result = optionChecker.checkOption(option, 5);
        assertTrue(result != -1);
        //Bad option - out of range (too big)
        option = "6";
        result = optionChecker.checkOption(option, 5);
        assertTrue(result == -1);
        //Bad optoin - out of range (too small)
        option = "-100";
        assertTrue(optionChecker.checkOption(option, 5) == -1);
        //Flat out bad option
        option = "asdfasdfasdf";
        result = optionChecker.checkOption(option, 5);
        assertTrue(result == -1);
        //Quit option
        option = "q";
        result = optionChecker.checkOption(option, 5);
        assertTrue(result == 0);
    }

    /**
     * Tests to make sure the user enters y/n
     */
    @Test
    public void testCheckYesOrNo() {
        //Enter a valid yes (uppercase)
        assertFalse(optionChecker.checkYesOrNo("Y"));
        //Enter a valid yes (lowercase)
        assertFalse(optionChecker.checkYesOrNo("y"));
        //Enter a valid no (uppercase)
        assertFalse(optionChecker.checkYesOrNo("N"));
        //Enter a valid no (lowercase)
        assertFalse(optionChecker.checkYesOrNo("n"));
        //Enter invalid characters
        assertTrue(optionChecker.checkYesOrNo("asdfasdfa"));
        assertTrue(optionChecker.checkYesOrNo("No"));
        assertTrue(optionChecker.checkYesOrNo("1"));
    }

    /**
     * Tests to make sure a file and a book can be compared.
     */
    @Test
    public void testCheckFile() {
        URL folder = OptionCheckerTest.class.getClassLoader().getResource(".");
        //Corresponding book and title
        Book book = new Book("Wonder", new Author("RJ", "Palacio"), new File("image.png"));
        File file = new File(folder.getPath() + Paths.get("Palacio", "RJ", "Wonder.json"));
        assertTrue(optionChecker.checkFile(file, book));
        //Title doesn't correspond with book
        file = new File(folder.getPath() + Paths.get("Fitzgerald", "Scott", "The Great Gatsby.json"));
        assertFalse(optionChecker.checkFile(file, book));
        //Book doesn't correspond with title
        book = new Book("ABC", new Author("CDE", "FGH"), new File("image.gif"));
        assertFalse(optionChecker.checkFile(file, book));
        //Book and title match again
        book = new Book("The Great Gatsby", new Author("Scott", "Fitzgerald"), new File("GreatGatsby.png"));
        assertTrue(optionChecker.checkFile(file, book));
    }
}
