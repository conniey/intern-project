// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.apache.commons.io.FilenameUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LocalBookCollector implements BookCollection {
    private final Set<String> supportedImageFormats;
    private Flux<Book> jsonBooks;
    private final OptionChecker OPTION_CHECKER = new OptionChecker();
    private List<File> jsonFiles;

    LocalBookCollector() {
        supportedImageFormats = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("gif", "png", "jpg")));
        File directory = new File(Constants.IMAGE_PATH);
        if (!directory.exists() && !directory.mkdir()) {
            throw new IllegalStateException("Couldn't create non-existent JSON directory: "
                + directory.getAbsolutePath());
        }
        File directoryJSON = new File(Constants.JSON_PATH);
        if (!directoryJSON.exists() && !directoryJSON.mkdir()) {
            throw new IllegalStateException("Couldn't create non-existent JSON directory: "
                + directoryJSON.getAbsolutePath());
        }
        jsonBooks = initializeBooks().cache();
        jsonFiles = retrieveJsonFiles();
    }

    @Override
    public Flux<Book> getBooks() {
        return jsonBooks;
    }

    /**
     * Stores all the Book info from the JSON files into a Flux
     * into a Flux object
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    private Flux<Book> initializeBooks() {
        try {
            return Flux.fromStream(Files.walk(Paths.get(Constants.JSON_PATH)))
                .filter(f -> f.toFile().getName().endsWith(".json"))
                .map(path -> Constants.SERIALIZER.fromJSONtoBook(new File(path.toString())));
        } catch (IOException e) {
            return Flux.error(e);
        }
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
        final Path fullImagePath = Paths.get(Constants.IMAGE_PATH, author.getLastName(), author.getFirstName(), path.getName());
        File imageFile = fullImagePath.toFile();
        if (!imageFile.exists() && !imageFile.mkdirs()) {
            System.err.println("Could not create directories for: " + fullImagePath.toString());
        }
        Book book = new Book(title, author, imageFile);
        if (saveImage(imageFile.getParentFile(), path) && book.checkBook()) {
            boolean bookSaved = Constants.SERIALIZER.writeJSON(book);
            jsonBooks = initializeBooks().cache();
            jsonFiles = retrieveJsonFiles();
            return Mono.just(bookSaved);
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
            File image = Paths.get(directory.getPath(), imagePath.getName()).toFile();
            return ImageIO.write(bufferedImage, extension, image);
        } catch (IOException ex) {
            return false;
        }
    }


    @Override
    public boolean deleteBook(Book bookToCompare) {
        boolean delete = jsonFiles.removeIf(x -> OPTION_CHECKER.checkFile(x, bookToCompare));
        if (delete) {
            deleteEmptyDirectories();
            jsonBooks = initializeBooks().cache();
            return true;
        }
        return false;
    }

    private List<File> retrieveJsonFiles() {
        try (Stream<Path> walk = Files.walk(Paths.get(Constants.JSON_PATH))) {
            return walk.map(Path::toFile).filter(f -> f.getName().endsWith(".json"))
                .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Exception deleting book file.");
            return Collections.emptyList();
        }
    }

    /**
     * Clears out any empty directories that might have been leftover from when the JSON file was deleted.
     */
    private void deleteEmptyDirectories() {
        File[] files = new File(Constants.JSON_PATH).listFiles();
        clearFiles(files);
        File[] imageFiles = new File(Constants.IMAGE_PATH).listFiles();
        clearFiles(imageFiles);
    }

    /**
     * Assists the emptyDirectory method by traversing through the files. When it finds an empty directory without a
     * JSON file, it deletes that file.
     *
     * @param files - the folder containing the other files in the library.
     */
    private void clearFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                clearFiles(file.listFiles());
            }
            if (file.length() == 0 && !file.getAbsolutePath().endsWith(".json")) {
                file.delete();
            }
        }
    }

    /**
     * Filters out the book based on the specified title.
     *
     * @param title - String of the book title the user is looking for
     * @return - {@link Flux} of {@link Book} with specified title in the collection
     */
    @Override
    public Flux<Book> findBookTitle(String title) {
        return jsonBooks.filter(book -> title.contentEquals(book.getTitle()));
    }

    /**
     * Filters out the books based on the specified author.
     *
     * @param author - Contains the name of the author the user is looking for
     * @return - {@link Flux} of {@link Book} by specified author in the collection
     */
    @Override
    public Flux<Book> findBookAuthor(Author author) {
        return jsonBooks.filter(book -> author.getFirstName().contentEquals(book.getAuthor().getFirstName())
            && book.getAuthor().getLastName().contentEquals(author.getLastName()));
    }

}
