// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OptionCheckerTest {
    private OptionChecker optionChecker = new OptionChecker();
    private URL folder = OptionCheckerTest.class.getClassLoader().getResource(".");
    private String root;

    @Before
    public void setUp() {
        try {
            URI folder = LocalBookCollector.class.getClassLoader().getResource(".").toURI();
            root = Paths.get(folder).toString();
        } catch (URISyntaxException e) {
            Assert.fail("");
        }
    }

    /**
     * Verifies that a png image can be read
     */
    @Test
    public void testPngImage() {

        //Arrange
        File pngPath = new File(folder.getPath() + "Wonder.png");
        //Act
        boolean pngCheck = optionChecker.checkImage(root, pngPath.toURI());
        //Assert
        assertTrue(pngCheck);
    }

    /**
     * Verifies that a jpg image can be read
     */
    @Test
    public void testJpgImage() {
        //Arrange
        File jpgPath = new File(folder.getPath() + "KK8.jpg");
        //Act
        boolean jpgCheck = optionChecker.checkImage(root, jpgPath.toURI());
        //Assert
        assertTrue(jpgCheck);
    }

    /**
     * Verifies that a gif image can be read
     */
    @Test
    public void testGifImage() {
        //Arrange
        File gifPath = new File(folder.getPath() + "GreatGatsby.gif");
        //Act
        boolean gifCheck = optionChecker.checkImage(root, gifPath.toURI());
        //Assert
        assertTrue(gifCheck);
    }

    /*
     * Verifies that any file that's not in the *.gif,.png,.jpg is not allowed
     */
    @Test
    public void testNonImageFile() {
        //Arrange
        File randomPath = new File(folder.getPath() + "Test.docx");
        //Act
        boolean check = optionChecker.checkImage(root, randomPath.toURI());
        //Assert
        assertFalse(check);
    }

    /**
     * Verifies that an author without a last name or only one name can't be entered
     */
    @Test
    public void testValidateAuthorInvalid() {
        //Arrange
        String name = "OnlyFirstName";
        //Act
        boolean check = optionChecker.validateAuthor(name.split(" "));
        //Assert
        assertFalse(check);
    }


    /**
     * Verifies that an author with at least a first and last name can be entered
     */
    @Test
    public void testValidateAuthorValid() {
        //Arrange
        String name = "First Last";
        //Act
        boolean check = optionChecker.validateAuthor(name.split(" "));
        //Assert
        assertTrue(check);
    }


    /**
     * Verifies that an author with a middle name/two last names, etc. can be entered
     */

    @Test
    public void testValidateAuthorValidMultiName() {
        //Arrange
        String name = "First and Last";
        //Act
        boolean check = optionChecker.validateAuthor(name.split(" "));
        //Assert
        assertTrue(check);
    }

    /**
     * Verifies that a valid option will register. In this test, the conditions will have a max of 5.
     * checkOption should not return a negative value, thus -1 is the indicator that the test failed.
     */

    @Test
    public void testOptionsValid() {
        //Arrange
        int result;
        //Act
        result = optionChecker.checkOption("2", 5);
        //Assert
        assertTrue(result != -1);
    }

    /**
     * Verifies that an option that's too big/out of range will not register
     * In this test, the conditions will have a max of 5.
     * checkOption should not return a negative value, thus -1 is the indicator that the test failed.
     */
    @Test
    public void testOptionsOutOfRangeBig() {
        //Arrange
        int result;
        //Act
        result = optionChecker.checkOption("6", 5);
        //Assert
        assertTrue(result == -1);
    }

    /**
     * Verifies that an option that's too small/out of range will not register
     * In this test, the conditions will have a max of 5. checkOption should
     * not return a negative value,thus -1
     * is the indicator that the test failed .
     */
    @Test
    public void testOptionsOutOfRangeSmall() {
        //Arrange
        int result;
        //Act
        result = optionChecker.checkOption("-100", 5);
        //Assert
        assertTrue(result == -1);
    }

    /**
     * Verifies that an option that's not a number won't be accepted.
     * In this test, the conditions will have a max of 5.
     * checkOption should not return a negative value,thus -1 is the
     * indicator that the test failed .
     */
    @Test
    public void testOptionsInvalidEntry() {
        //Arrange
        int result;
        //Act
        result = optionChecker.checkOption("asdf asdf", 5);
        //Assert
        assertTrue(result == -1);
    }

    /**
     * Verifies that if the user enter 'q'or "Q",
     * their option will be accepted and they'll return to the menu
     * In this test, the conditions will have a max of 5.
     * checkOption should not return
     * a negative value,thus -1
     * is the indicator that the test failed.
     */
    @Test
    public void testOptionsQuit() {
        //Arrange
        int result;
        int result2;
        //Act
        result = optionChecker.checkOption("q", 5);
        result2 = optionChecker.checkOption("Q", 5);
        //Assert
        assertTrue(result == 0); //Since 0 is also not an option that the user can otherwise pick, it indicates return to menu
        assertTrue(result2 == 0);
    }

    /**
     * Verifies that if the user enters "y"or 'Y", their option will be accepted
     * and thus they won't be prompted again. Which is why the return value is false if
     * their value is correct to alert the system not to ask them continuously .
     */
    @Test
    public void testYesOption() {
        //Arrange
        String entryY = "Y";
        String entryLittleY = "y";
        //Act
        boolean acceptY = optionChecker.checkYesOrNo(entryY);
        boolean acceptLittleY = optionChecker.checkYesOrNo(entryLittleY);
        //Assert
        assertFalse(acceptY);
        assertFalse(acceptLittleY);
    }

    /**
     * Verifies that if the user enters "y"or 'Y", their option will be accepted
     * and thus they won't be prompted again, which is
     * why a false value is returned if their value is correct to alert
     * the system not to ask them continuously.
     */
    @Test
    public void testNoOption() {
        //Arrange
        String entryN = "N";
        String entryLittleN = "n";
        //Act
        boolean acceptN = optionChecker.checkYesOrNo(entryN);
        boolean acceptLittleN = optionChecker.checkYesOrNo(entryLittleN);
        //Assert
        assertFalse(acceptN);
        assertFalse(acceptLittleN);
    }

    /**
     * Tests various invalid entries
     * that shouldn't be accepted if user is prompted to enter Y/N,
     * thus the value returns is true to alert the system
     * to keep asking
     */
    @Test
    public void testCheckYesOrNoInvalid() {
        //Arrange
        String badEntry1 = "asdfasdfa";
        String badEntry2 = "No";
        String badEntry3 = "1";
        //Act
        boolean badResult1 = optionChecker.checkYesOrNo(badEntry1);
        boolean badResult2 = optionChecker.checkYesOrNo(badEntry2);
        boolean badResult3 = optionChecker.checkYesOrNo(badEntry3);
        //Assert
        assertTrue(badResult1);
        assertTrue(badResult2);
        assertTrue(badResult3);
    }

    /**
     * Tests to make sure a file and a book can be compared.
     * In this test, the file and book aren't similar thus verifying a false value
     */
    @Test
    public void testCheckFileNotMatching() {
        //Arrange
        Book book = new Book("Wonder", new Author("RJ", "Palacio"), new File("image.png").toURI());
        File file = new File(folder.getPath() + Paths.get("Fitzgerald", "Scott", "The Great Gatsby.json"));
        assertFalse(optionChecker.checkFile(file, book));
        //Act
        boolean result = optionChecker.checkFile(file, book);
        //Assert
        assertFalse(result);
    }

    /**
     * Tests to make sure a file and a book can be compared.
     * In this test, the file and book are corresponding
     * thus returning a true value
     */
    @Test
    public void testCheckFile() {
        //Arrange - corresponding book and file
        Book book = new Book("Wonder", new Author("RJ", "Palacio"), new File("image.png").toURI());
        File file = new File(folder.getPath() + Paths.get("Palacio", "RJ", "Wonder.json"));
        //Act
        boolean result = optionChecker.checkFile(file, book);
        //Assert
        assertTrue(result);
    }
}
