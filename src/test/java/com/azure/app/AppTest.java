// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


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

    /**
     * Tests the JsonHandler class
     */
    @Test
    @Ignore("Needs update to use local paths")
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

    /**
     * Tests to make sure the directives are cleared
     */
    @Test
    @Ignore("Needs update to use local paths")
    public void testClearEmptyFiles() {
        BookCollection collection = mock(BookCollection.class);
        new DeleteBook(collection).deleteEmptyDirectories();
    }

    /**
     * Tests to see if books can be registered
     */
    @Test
    public void testRegisterBooks() {
        try {
            new LocalBookCollector().getBooks();
            assertTrue(true);
        } catch (NullPointerException e) {
            Assert.fail("");
        }
    }

    /**
     * Tests to make sure it takes in correct options
     */
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

    /**
     * Makes sure that books can be saved properly.
     */
    @Test
    @Ignore("Needs update to use local paths")
    public void testSaveBooks() {
        try {
            LocalBookCollector fileCollector = new LocalBookCollector();
            assertTrue(fileCollector.saveBook("Title", new Author("Good", "Author"), new File("C:\\Users\\t-katami\\Documents\\Images\\Book.png")).block());
        } catch (NullPointerException e) {
            Assert.fail("");
        }
    }

    /**
     * Makes sure that the correct images are saved.
     */
    @Test
    @Ignore("Needs update to use local paths")
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

    /**
     * Makes sure that the author's name contains at least a first and last name
     */
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
