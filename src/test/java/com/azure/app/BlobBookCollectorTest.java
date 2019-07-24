package com.azure.app;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class BlobBookCollectorTest {
    private BlobBookCollector blobCollector = new BlobBookCollector();
    String root;

    @Before
    public void setUp() {
        try {
            URI folder = LocalBookCollectorTest.class.getClassLoader().getResource(".").toURI();
            root = Paths.get(folder).toString();
        } catch (URISyntaxException e) {
            Assert.fail("");
        }
    }


    @Test
    public void overwriteBook() {
        Flux<Book> inventory = blobCollector.getBooks();
        List<Book> check = inventory.collectList().map(list -> {
            if (!list.isEmpty()) {
                Book copy = list.get(0);
                blobCollector.saveBook(copy.getTitle(), copy.getAuthor(),
                    Paths.get(root, "GreatGatsby.gif").toFile().toURI()).onErrorResume(error -> {
                    System.out.println("Nope: " + error.toString());
                    return Mono.empty();
                });
            }
            return list;
        }).block();
        assertTrue(check.size() == 1);
    }
}
