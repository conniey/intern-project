// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OptionCheckerTest {
    private OptionChecker optionChecker = new OptionChecker();

    /**
     * Makes sure that the correct images are saved.
     */
    @Test
    @Ignore("Needs update to use local paths")
    public void testCheckImage() {
        //Good png picture
        URL folder = AppTest.class.getClassLoader().getResource(".");
        File path = new File(folder.getPath() + "\\Wonder.png");
        assertTrue(optionChecker.checkImage(path));
        //Good jpg
        path = new File(folder.getPath() + "\\KK8.jpg");
        assertTrue(optionChecker.checkImage(path));
        //Good gif
        path = new File(folder.getPath() + "\\GreatGatsby.gif");
        assertTrue(optionChecker.checkImage(path));
        //Random file
        path = new File(folder.getPath() + "\\Test.docx");
        assertFalse(optionChecker.checkImage(path));
    }
    //C:\Users\t-katami\Documents\intern-project\target\test-classes\Book.png

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
        //Bad option - out of range
        option = "6";
        result = optionChecker.checkOption(option, 5);
        assertTrue(result == -1);
        //Flat out bad option
        option = "asdfasdfasdf";
        result = optionChecker.checkOption(option, 5);
        assertTrue(result == -1);
        //Quit option
        option = "q";
        result = optionChecker.checkOption(option, 5);
        assertTrue(result == 0);
    }
}
