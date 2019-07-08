// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class FileCollector implements BookCollection {
    private final JsonHandler serializer;
    private final String jsonPath = "\\lib\\jsonFiles\\";
    private final String imagePath = "\\lib\\images\\";

    FileCollector() {
        serializer = new JsonHandler();
        File directory = new File(imagePath);
        directory.mkdir();
        File directoryJSON = new File(jsonPath);
        directoryJSON.mkdir();
    }

    /**
     * Reads all the JSON files and then saves their informations in new Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    @Override
    public Flux<Book> registerBooks() {
        Flux<Book> savedBook = Flux.empty();
        try (Stream<Path> walk = Files.walk(Paths.get("\\lib\\jsonFiles\\"))) {
            List<String> result = walk.map(x -> x.toString()).filter(f -> f.endsWith(".json")).collect(Collectors.toList());
            if (result.isEmpty() || result == null) {
                return Flux.empty();
            }
            savedBook = Flux.create(bookFluxSink -> {
                for (int i = 0; i < result.size(); i++) {
                    bookFluxSink.next(new JsonHandler().fromJSONtoBook(new File(result.get(i))));
                }
                bookFluxSink.complete();
            }, FluxSink.OverflowStrategy.BUFFER);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return savedBook;
    }

    /**
     * Saves the book as a JSON file
     *
     * @param title  - String containing the title of the book
     * @param author - Author object of the book
     * @param path   - File containing the cover image of the book
     * @return Mono<Boolean> that determines whether the book got saved or not
     * true - book was successfully saved
     * false - book wasn't saved </Boolean>
     */
    @Override
    public Mono<Boolean> saveBook(String title, Author author, File path) {
        File dir = new File(imagePath + "\\" + author.getLastName());
        dir.mkdir();
        File dir2 = new File(dir + "\\" + author.getFirstName());
        dir2.mkdir();
        File newPath = new File(dir2 + "\\" + path.getName());
        saveImage(dir2, path);
        Book book = new Book(title, author, newPath);
        if (book.checkBook()) {
            Mono<Book> newBook = Mono.just(book);
            return newBook.map(x -> {
                return serializer.writeJSON(x);
            });
        }
        return Mono.just(false);
    }

    private boolean saveImage(File directory, File imagePath) {
        try {
            BufferedImage bufferedImage = ImageIO.read(imagePath);
            String extension = imagePath.getName();
            File image = new File(directory + "\\" + extension);
            if (extension.endsWith(".png")) {
                ImageIO.write(bufferedImage, "png", image);
                return true;
            } else if (extension.endsWith(".jpg")) {
                ImageIO.write(bufferedImage, "jpg", new File(directory + "\\" + imagePath.getName()));
                return true;
            } else if (extension.endsWith(".gif")) {
                ImageIO.write(bufferedImage, "gif", new File(directory + "\\" + imagePath.getName()));
                return true;
            }
        } catch (IOException ex) {
            return false;
        }
        return false;
    }
}
