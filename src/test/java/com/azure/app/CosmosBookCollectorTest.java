// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.ConfigurationClientBuilder;
import com.azure.data.appconfiguration.credentials.ConfigurationClientCredentials;
<<<<<<< HEAD
=======
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
<<<<<<< HEAD
import org.slf4j.LoggerFactory;
=======
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

<<<<<<< HEAD
import static org.junit.Assert.assertEquals;

public class CosmosBookCollectorTest {
    private CosmosDocumentProvider cosmosBC;
    private static final URL FOLDER = CosmosBookCollectorTest.class.getClassLoader().getResource(".");
=======
public class CosmosBookCollectorTest {
    private BookCollector cosmosBC;
    URL folder = CosmosBookCollectorTest.class.getClassLoader().getResource(".");
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6

    /**
     * Sets up App Configuration to get the information needed for Cosmos.
     */
    @Before
    public void setup() {
        ObjectMapper mapper = new ObjectMapper();
        String connectionString = System.getenv("AZURE_APPCONFIG");
        if (connectionString == null || connectionString.isEmpty()) {
            System.err.println("Environment variable AZURE_APPCONFIG is not set. Cannot connect to App Configuration."
                + " Please set it.");
            return;
        }
        ConfigurationAsyncClient client;
        try {
            client = new ConfigurationClientBuilder()
                .credential(new ConfigurationClientCredentials(connectionString))
                .httpLogDetailLevel(HttpLogDetailLevel.HEADERS)
                .buildAsyncClient();
            CosmosSettings cosmosSettings = mapper.readValue(client.getSetting("COSMOS_INFO").block().value(), CosmosSettings.class);
<<<<<<< HEAD
            cosmosBC = new CosmosDocumentProvider(cosmosSettings);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            LoggerFactory.getLogger(BlobImageProviderTest.class).error("Error in setting up the CosmosBookCollector: ", e);
=======
            BlobSettings blobSettings = mapper.readValue(client.getSetting("BLOB_INFO").block().value(), BlobSettings.class);
            cosmosBC = new BookCollector(new CosmosDocumentProvider(cosmosSettings), new BlobImageProvider(blobSettings));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Assert.fail("");
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
        }
    }

    /**
     * Tests to see that a book can be saved to Cosmos as a JSON file.
     */
<<<<<<< HEAD
    @Test
    public void testSaveBook() {
        Book book = new Book("Valid", new Author("Work", "Hard"),
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        StepVerifier.create(cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()))
            .expectComplete()
            .verify();
=======
    @Ignore
    @Test
    public void testSaveBook() {
        Book book = new Book("Valid", new Author("Work", "Hard"),
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        StepVerifier.create(cosmosBC.saveBook(book))
            .expectComplete()
            .verify();
        //Todo: Cleanup when you figure out how to delete
    }

    /**
     * Tests the getBook method
     */
    @Ignore
    @Test
    public void testGetBook() {
        Flux<Book> books = cosmosBC.getBooks();
        books.collectList().map(list -> {
            for (int i = 0; i < list.size(); i++) {
                System.out.println(list.get(i));
            }
            return list;
        }).block();
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
    }

    /**
     * Tests deletion.
     */
    @Ignore
    @Test
    public void testDeleteBook() {
        Book book = new Book("Once", new Author("Work", "Hard"),
<<<<<<< HEAD
            new File(FOLDER.getPath() + "GreatGatsby.gif").toURI());
        cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()).block();
=======
            new File(folder.getPath() + "GreatGatsby.gif").toURI());
        cosmosBC.saveBook(book).block();
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
        cosmosBC.deleteBook(book).block();
    }

    /**
     * Tests find
     */
<<<<<<< HEAD
    @Test
    public void testFindTitle() {
        //Arrange
        String title = "ASD0a3FHJKL";
        Book book = new Book(title, new Author("Crazy", "Writer"), new File(FOLDER.getPath(), "GreatGatsby.gif").toURI());
        cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()).block();
        //Act
        Flux<Book> length = cosmosBC.findBook(title);
        //Assert
        StepVerifier.create(length)
            .assertNext(expected -> assertEquals(expected.getTitle(), title))
            .verifyComplete();
        //Cleanup...eventally....
=======
    @Ignore
    @Test
    public void testFindTitle() {
        //Arrange
        Book book = new Book("ASD0a3FHJKL", new Author("Crazy", "Writer"), new File(folder.getPath(), "GreatGatsby.gif").toURI());
        int formerLength = cosmosBC.findBook(book.getTitle()).count().block().intValue();
        cosmosBC.saveBook(book).block();
        //Act
        int length = cosmosBC.findBook(book.getTitle()).count().block().intValue();
        //Assert
        Assert.assertTrue(formerLength + 1 == length);
        //Cleanup
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
    }

    /**
     * Tests find
     */
    @Test
<<<<<<< HEAD
    public void testFindNoTitle() {
        //Arrange
        Book book = new Book("Utterly Ridicious", new Author("IMPOssibleToHaveYOu", "Yep"),
            new File(FOLDER.getPath(), "GreatGatsby.gif").toURI());
=======
    @Ignore
    public void testFindNoTitle() {
        //Arrange
        Book book = new Book("Utterly Ridicious", new Author("IMPOssibleToHaveYOu", "Yep"),
            new File(folder.getPath(), "GreatGatsby.gif").toURI());
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
        //Act
        int length = cosmosBC.findBook(book.getTitle()).count().block().intValue();
        //Assert
        Assert.assertTrue(length == 0);
    }
<<<<<<< HEAD

    /**
     * Test find
     */
    @Test
    public void testFindAuthor() {
        //Arrange
        Author author = new Author("HAJKSDFAard", "Kadmklasnock");
        Book book = new Book("abdeidoapsd", author, new File("Rip.png").toURI());
        cosmosBC.saveBook(book.getTitle(), book.getAuthor(), book.getCover()).block();
        //Act
        Flux<Book> foundBook = cosmosBC.findBook(author);
        //Assert
        StepVerifier.create(foundBook)
            .assertNext(bookCopy -> {
                assertEquals(author.getLastName(), bookCopy.getAuthor().getLastName());
                assertEquals(author.getFirstName(), bookCopy.getAuthor().getFirstName());
            }).verifyComplete();
        //Since delete doesn't work, you'll have to manually delete the item from your Cosmos storage
    }

    /**
     * Tests find
     */
    @Test
    public void testFindNoAuthor() {
        //Arrange
        Book book = new Book("Utterly Ridicious", new Author("IMPOssibleToHaveYOu", "Yep"),
            new File(FOLDER.getPath(), "GreatGatsby.gif").toURI());
        //Act
        int length = cosmosBC.findBook(book.getAuthor()).count().block().intValue();
        //Assert
        Assert.assertTrue(length == 0);
    }
=======
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
}
