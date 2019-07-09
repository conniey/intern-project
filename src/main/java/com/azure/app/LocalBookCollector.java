// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.apache.commons.io.FilenameUtils;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LocalBookCollector implements BookCollection {
    private final Set<String> supportedImageFormats;

    //Consider passing a string that's the root of the directory you want to add the things in
    LocalBookCollector() {
        supportedImageFormats = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("gif", "png", "jpg")));
        File directory = new File(Constants.IMAGE_PATH);
        directory.mkdir();
        File directoryJSON = new File(Constants.JSON_PATH);
        directoryJSON.mkdir();
    }

    /**
     * Stores all the Book info from the JSON files into a Flux
     * into a Flux object
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    @Override
    public Flux<Book> registerBooks() {
        Flux<Book> savedBook = Flux.empty();
        List<File> result = traverseJsonFiles();
        if (result != null) {
            savedBook = Flux.create(bookFluxSink -> {
                for (int i = 0; i < result.size(); i++) {
                    bookFluxSink.next(new JsonHandler().fromJSONtoBook(result.get(i)));
                }
                bookFluxSink.complete();
            }, FluxSink.OverflowStrategy.BUFFER);
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
        File dir = new File(Constants.IMAGE_PATH + "\\" + author.getLastName());
        dir.mkdir();
        File dir2 = new File(dir + "\\" + author.getFirstName());
        dir2.mkdir();
        File newPath = new File(dir2 + "\\" + path.getName());
        saveImage(dir2, path);
        Book book = new Book(title, author, newPath);
        if (book.checkBook()) {
            return Mono.just(Constants.SERIALIZER.writeJSON(book));
        }
        return Mono.just(false);
    }

    private boolean saveImage(File directory, File imagePath) {
        String extension = FilenameUtils.getExtension(imagePath.getName());
        if (!supportedImageFormats.contains(extension)) {
            return false;
        }
        try {
            BufferedImage bufferedImage = ImageIO.read(imagePath);
            File image = new File(directory + "\\" + imagePath.getName());
            return ImageIO.write(bufferedImage, extension, image);
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public List<File> traverseJsonFiles() {
        List<File> result = null;
        try (Stream<Path> walk = Files.walk(Paths.get(Constants.JSON_PATH))) {
            result = walk.map(x -> x.toFile()).filter(f -> f.getAbsolutePath().endsWith(".json")).collect(Collectors.toList());
            if (result.isEmpty()) {
                return Collections.emptyList();
            }
        } catch (IOException e) {
            System.err.println("Exception traversing through JSON files.");
        }
        return result;
    }
}
