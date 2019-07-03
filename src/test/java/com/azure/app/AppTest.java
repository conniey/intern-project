// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;


import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.ByteArrayInputStream;
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
    public void testSerialization() {
        //Good book
        Book b = new Book("Wonder", new Author("Palacio", "R. J."),
            new File("C:\\Users\\t-katami\\Documents\\Images\\Book.png"));
        BookSerializer bookSerializer = new BookSerializer();
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

    public void testDeserialization() {
        BookDeserialize bd = new BookDeserialize();
        Book wonderBook = bd.fromJSONtoBook(
            new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib\\Palacio\\R. J.\\Wonder.json"));
        assertTrue(wonderBook.checkBook());
    }

    @Test
    public void testClearEmptyFiles() {
        App.deleteEmptyDirectories();
    }

    @Test
    public void testRegisterBooks() {
        try {
            App.registerBooks();
            assertTrue(true);
        } catch (NullPointerException e) {
            Assert.fail("");
            assertTrue(false);
        }
    }

    public void testOptions() {

    }

    public void testListBooks() {
        try {
            App.listBooks();
            assertTrue(true);
        } catch (NullPointerException e) {
            assertTrue(false);
        }
    }

    @Test
    public void testAddBook() {
        //Test with good infromation.
        String goodInformation = "Good Book" + "\nDecent Author" +
            "\nC:\\Users\\t-katami\\Documents\\Images\\Book.png" + "\nn";
        System.setIn(new ByteArrayInputStream(goodInformation.getBytes()));
        App.addBook();
        //Test with bad author;
        String goodInformation2 = "Good Book" + "\nDecent Author" +
            "\nC:\\Users\\t-katami\\Documents\\Images\\Book.png" + "\nn";
        System.setIn(new ByteArrayInputStream(goodInformation2.getBytes()));
        App.addBook();
    }

}
/*
System.setIn(new ByteArrayInputStream(data.getBytes()));
        Scanner sc = new Scanner(System.in);
        System.out.println("Do you work? " + sc.next());
 */
