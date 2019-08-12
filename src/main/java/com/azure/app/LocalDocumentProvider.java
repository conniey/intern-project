// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class LocalDocumentProvider implements DocumentProvider {
    private Flux<Book> jsonBooks;
    private List<File> jsonFiles;
    private String root;
    private final OptionChecker optionChecker = new OptionChecker();
    private static Logger logger = LoggerFactory.getLogger(LocalDocumentProvider.class);

    LocalDocumentProvider(String root) {
        this.root = root;
        File directoryJSON = new File(Paths.get(root, Constants.JSON_PATH).toString());
        if (!directoryJSON.exists() && !directoryJSON.mkdirs()) {
            logger.error("Couldn't create non-existent JSON directory: " + directoryJSON.getAbsolutePath());
        }
        jsonBooks = initializeBooks().cache();
        jsonFiles = retrieveJsonFiles();
    }

    /**
     * Returns the Flux of Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
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
            return Flux.fromStream(Files.walk(Paths.get(root, Constants.JSON_PATH)))
                .filter(f -> f.toFile().getName().endsWith(".json"))
                .map(path -> Constants.SERIALIZER.fromJSONtoBook(new File(path.toString())));
        } catch (IOException e) {
            logger.error("Error making Flux: ", e);
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
     * true -  book was successfully saved
     * false - book wasn't saved </Boolean>
     */
    @Override
    public Mono<Void> saveBook(String title, Author author, URI path) {
        File imagePath = new File(path);
        String extension = FilenameUtils.getExtension(imagePath.getAbsolutePath());
        final Path fullImagePath = Paths.get(root, Constants.IMAGE_PATH, author.getLastName(),
            author.getFirstName());
        File imageFile = fullImagePath.toFile();
        if (!imageFile.exists() && !imageFile.mkdirs()) {
            logger.error("Couldn't create directories for: " + imageFile.getAbsolutePath());
        }
        String blobTitle;
        try {
            blobTitle = URLEncoder.encode(title.replace(' ', '-'), StandardCharsets.US_ASCII.toString());
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding: ", e);
            return Mono.error(e);
        }
        File relativePath = Paths.get(Constants.IMAGE_PATH, author.getLastName(), author.getFirstName(),
            blobTitle + "." + extension).toFile();
        URI saved = relativePath.toURI();
        URI relative = new File(System.getProperty("user.dir")).toURI().relativize(saved);
        Book book = new Book(title, author, relative);
        duplicateBook(book);
        if (book.isValid()) {
            boolean bookSaved = Constants.SERIALIZER.writeJSON(book, root);
            jsonBooks = initializeBooks().cache();
            jsonFiles = retrieveJsonFiles();
            if (bookSaved) {
                return Mono.empty();
            } else {
                return Mono.error(new IllegalStateException("Unsuccessful save"));
            }
        }
        return Mono.error(new IllegalStateException("Unsuccessful save"));
    }

    /**
     * Overwrites the old book with the contents in the new book
     *
     * @param oldBook   - Book object that will be changed
     * @param newBook   - Book object with the new information to change to
     * @param saveCover - determines whether or not the user wants to keep the same cover
     * @return {@Link Mono}
     */
    @Override
    public Mono<Void> editBook(Book oldBook, Book newBook, int saveCover) {
        if (saveCover == 1) { // Overwriting/changing cover
            return saveBook(newBook.getTitle(), newBook.getAuthor(), newBook.getCover());
        } else {
            File image = Paths.get(System.getProperty("user.dir"), oldBook.getCover().getPath()).toFile();
            return saveBook(newBook.getTitle(), newBook.getAuthor(), image.toURI()).then(
                deleteBook(oldBook));
        }
    }

    /**
     * Deletes the old book if the new book has a the same title and author
     *
     * @param bookToCompare - Book object that's going to be checked
     */
    private void duplicateBook(Book bookToCompare) {
        //Checks to see if the book has a duplicate, if so it'll delete it so it can be overwritten
        jsonFiles.removeIf(x -> {
            boolean result = optionChecker.checkFile(x, bookToCompare);
            if (result) {
                Book imageToDelete = Constants.SERIALIZER.fromJSONtoBook(x);
                Paths.get(System.getProperty("user.dir"),
                    imageToDelete.getCover().getPath()).toFile().delete();
                return true;
            }
            return false;
        });
    }

    /**
     * Deletes the book and the file based off its information.
     *
     * @param bookToCompare - book that will be deleted
     * @return Mono<Boolean> determines whether or not book was successfully deleted </Boolean>
     * true - Book was deleted
     * false - Book wasn't deleted
     */
    @Override
    public Mono<Void> deleteBook(Book bookToCompare) {
        boolean delete = jsonFiles.removeIf(x -> {
            boolean result = optionChecker.checkFile(x, bookToCompare);
            if (result) {
                x.delete();
            }
            return result;
        });
        if (delete) {
            deleteEmptyDirectories();
            jsonBooks = initializeBooks().cache();
            return Mono.empty();
        }
        return Mono.error(new IllegalStateException(""));
    }

    /**
     * Retrieves the files on book information from the local directory
     *
     * @return a List of Files
     */
    private List<File> retrieveJsonFiles() {
        try (Stream<Path> walk = Files.walk(Paths.get(Constants.JSON_PATH))) {
            return walk.map(Path::toFile).filter(f -> f.getName().endsWith(".json"))
                .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Exception deleting book file.", e);
            return Collections.emptyList();
        }
    }

    /**
     * Clears out any empty directories that might have been leftover from when the JSON file was deleted.
     */
    private void deleteEmptyDirectories() {
        File[] files = new File(Constants.JSON_PATH).listFiles();
        assert files != null;
        clearFiles(files);
    }

    /**
     * Assists the emptyDirectory method by traversing through the files. When it finds an empty directory without a
     * JSON file, it deletes that file.
     *
     * @param files - the folder containing the other files in the library.
     */
    private void clearFiles(File[] files) {
        for (File file : files) {
            if (file == null) {
                return;
            } else {
                if (file.isDirectory()) {
                    clearFiles(Objects.requireNonNull(file.listFiles()));
                }
                if (file.length() == 0 && !file.getAbsolutePath().endsWith(".json")) {
                    file.delete();
                }
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
    public Flux<Book> findBook(String title) {
        return jsonBooks.filter(book -> title.contentEquals(book.getTitle()));
    }

    /**
     * Filters out the books based on the specified author.
     *
     * @param author - Contains the name of the author the user is looking for
     * @return - {@link Flux} of {@link Book} by specified author in the collection
     */
    @Override
    public Flux<Book> findBook(Author author) {
        return jsonBooks.filter(book -> author.getFirstName().contentEquals(book.getAuthor().getFirstName())
            && book.getAuthor().getLastName().contentEquals(author.getLastName()));
    }
}
