// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

class LocalBookCollector implements BookCollection {
    private final JsonHandler serializer;
    private final String jsonPath = "\\lib\\jsonFiles\\";
    private final String imagePath = "\\lib\\images\\";

    // A flux containing all the books in the collection.
    private final Flux<Book> books;

    LocalBookCollector() {
        serializer = new JsonHandler();

        final File directory = new File(imagePath);
        if (!directory.exists() && !directory.mkdir()) {
            throw new IllegalStateException("Could not create non-existent image directory: "
                + directory.getAbsolutePath());
        }

        final File directoryJSON = new File(jsonPath);
        if (!directoryJSON.exists() && !directoryJSON.mkdir()) {
            throw new IllegalStateException("Could not create non-existent JSON directory: "
                + directoryJSON.getAbsolutePath());
        }

        // Caching this Flux so we don't read the repository multiple times.
        books = Flux.defer(this::initializeBooks).cache();
    }

    /**
     * Reads all the JSON files and then saves their information in new Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    @Override
    public Flux<Book> getBooks() {
        return books;
    }

    /**
     * Reads all the JSON files and saves their information into new {@link Book} objects.
     *
     * @return A {@link Flux<Book>} of all the books in this collection.
     */
    private Flux<Book> initializeBooks() {
        try {
            return Flux.fromStream(Files.walk(Paths.get(jsonPath)))
                .filter(f -> f.getFileName().endsWith(".json"))
                .map(path -> serializer.fromJSONtoBook(new File(path.toString())));
        } catch (IOException e) {
            return Flux.error(e);
        }
    }

    /**
     * Saves the book as a JSON file
     *
     * @param title - String containing the title of the book
     * @param author - Author object of the book
     * @param path - File containing the cover image of the book
     * @return Mono<Boolean> that determines whether the book got saved or not true - book was successfully saved false
     *         - book wasn't saved </Boolean>
     */
    @Override
    public Mono<Boolean> saveBook(String title, Author author, File path) {
        final Path fullImagePath = Paths.get(imagePath, author.getLastName(), author.getFirstName(), path.getName());
        final File imageFile = fullImagePath.toFile();

        if (!imageFile.mkdirs()) {
            System.out.printf(Locale.US, "Could not create directories for: %s%n", fullImagePath);
        }

        File parentDirectory = imageFile.getParentFile();
        Book book = new Book(title, author, imageFile);

        if (saveImage(parentDirectory, path) && book.checkBook()) {
            return Mono.just(serializer.writeJSON(book));
        } else {
            return Mono.just(false);
        }
    }

    private boolean saveImage(File directory, File imagePath) {
        try {
            BufferedImage bufferedImage = ImageIO.read(imagePath);
            String extension = imagePath.getName();
            File image = new File(directory + "\\" + extension);
            if (extension.endsWith(".png")) {
                return ImageIO.write(bufferedImage, "png", image);
            } else if (extension.endsWith(".jpg")) {
                return ImageIO.write(bufferedImage, "jpg", image);
            } else if (extension.endsWith(".gif")) {
                return ImageIO.write(bufferedImage, "gif", image);
            }
        } catch (IOException ex) {
            return false;
        }
        return false;
    }
}
