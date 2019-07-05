// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;


import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Unit test for simple App.
 */
public class AppTest {

    @Rule
    public TestName name = new TestName();

    /**
     * Rigourous Test :-)
     */
    public void testApp() {

    }

    @Test
    public void testSerializationAndCheckBook() {
        //Good book
        Book b = new Book("Wonder", new Author("Palacio", "R. J."),
            new File("C:\\Users\\t-katami\\Documents\\Images\\Book.png"));
        JsonHandler bookSerializer = new JsonHandler();
        assertTrue(bookSerializer.writeJSON(b));
        //Bad book (empty title)
        Book b2 = new Book("", new Author("Palacio", "R. J."),
            new File("C:\\Users\\t-katami\\Documents\\Images\\Book.png"
            ));
        assertFalse(bookSerializer.writeJSON(b2));
        //Bad book (Invalid author)
        Book b3 = new Book("Wonder", new Author("", null),
            new File("C:\\Users\\t-katami\\Documents\\Images\\Book.png"));
        assertFalse(bookSerializer.writeJSON(b3));
        //Bad book (Wrong file path)
        Book b4 = new Book("Wonder", new Author("Palacio", "R. J."),
            new File(""));
        assertFalse(bookSerializer.writeJSON(b4));
        //Completely bad book (wrong on all aspects)
        Book b5 = new Book(null, new Author(null, null),
            new File(""));
        assertFalse(bookSerializer.writeJSON(b5));
    }

    @Test
    public void testClearEmptyFiles() {
        new DeleteBook().deleteEmptyDirectories();
    }

    @Test
    public void testRegisterBooks() {
        try {
            new FileCollector().registerBooks();
            assertTrue(true);
        } catch (NullPointerException e) {
            Assert.fail("");
        }
    }

    @Test
    public void testOptions() {
        OptionChecker optionChecker = new OptionChecker();
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

    @Test
    public void testSaveBooks() {
        try {
            FileCollector fileCollector = new FileCollector();
            assertTrue(fileCollector.saveBook("Title", new Author("Good", "Author"), new File("C:\\Users\\t-katami\\Documents\\Images\\Book.png"), "y"));
        } catch (NullPointerException e) {
            Assert.fail("");
        }
    }

    @Test
    public void testCheckImage() {
        OptionChecker optionChecker = new OptionChecker();
        //Good png picture
        File fh = new File("C:\\Users\\t-katami\\Documents\\Images\\Book.png");
        assertTrue(optionChecker.checkImage(fh));
        //Good jpg
        fh = new File("C:\\Users\\t-katami\\Documents\\Images\\bookpic.jpg");
        assertTrue(optionChecker.checkImage(fh));
        //Good gif
        fh = new File("C:\\Users\\t-katami\\Documents\\Images\\GreatGatsby.gif");
        assertTrue(optionChecker.checkImage(fh));
        //Random file
        fh = new File("C:\\Users\\t-katami\\Documents\\Windows PowerShell.docx");
        assertFalse(optionChecker.checkImage(fh));
    }

    @Test
    public void testAuthorCheck() {
        OptionChecker optionChecker = new OptionChecker();
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

}
