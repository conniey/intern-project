// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
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
    private List<File> jsonFiles;
    private String root;
    private final OptionChecker optionChecker = new OptionChecker();
    private static Logger logger = LoggerFactory.getLogger(JsonHandler.class);


    LocalBookCollector(String root) {
        this.root = root;
        supportedImageFormats = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("gif", "png", "jpg")));
        File directory = new File(Paths.get(root, Constants.IMAGE_PATH).toString());
        if (!directory.exists() && !directory.mkdirs()) {
            logger.error("Couldn't create non-existent JSON directory: " + directory.getAbsolutePath());
        }
        File directoryJSON = new File(Paths.get(root, Constants.JSON_PATH).toString());
        if (!directoryJSON.exists() && !directoryJSON.mkdirs()) {
            logger.error("Couldn't create non-existent JSON directory: " + directoryJSON.getAbsolutePath());
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
    public Mono<Boolean> saveBook(String title, Author author, URI path) {
        File imagePath = new File(path);
        final Path fullImagePath = Paths.get(root, Constants.IMAGE_PATH, author.getLastName(),
            author.getFirstName());
        File imageFile = fullImagePath.toFile();
        if (!imageFile.exists() && !imageFile.mkdirs()) {
            logger.error("Couldn't create directories for: " + imageFile.getAbsolutePath());
        }
        URI savedImage = saveImage(imageFile, imagePath);
        Book book = new Book(title, author, savedImage);
        if (optionChecker.checkImage(root, savedImage) && book.checkBook(root)) {
            boolean bookSaved = Constants.SERIALIZER.writeJSON(book, root);
            jsonBooks = initializeBooks().cache();
            jsonFiles = retrieveJsonFiles();
            return Mono.just(bookSaved);
        }
        return Mono.just(false);
    }

    private URI saveImage(File directory, File imagePath) {
        String extension = FilenameUtils.getExtension(imagePath.getName());
        if (!supportedImageFormats.contains(extension)) {
            return null;
        }
        try {
            BufferedImage bufferedImage = ImageIO.read(imagePath);
            File image = new File(Paths.get(directory.getPath(), imagePath.getName()).toString());
            if (ImageIO.write(bufferedImage, extension, image)) {
                return image.toURI();
            }
        } catch (IOException ex) {
            logger.error("Error saving image: ", ex);
            return null;
        }
        return null;
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
    public boolean deleteBook(Book bookToCompare) {
        boolean delete = jsonFiles.removeIf(x -> {
            boolean result = optionChecker.checkFile(x, bookToCompare);
            if (result) {
                x.delete();
            }
            return result;
        });
        if (delete) {
            new File(bookToCompare.getCover()).delete();
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
            logger.error("Exception deleting book file.", e);
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
            if (file == null) {
                return;
            } else {
                if (file.isDirectory()) {
                    clearFiles(file.listFiles());
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

    /**
     * Determines whether the Flux is empty or not
     */
    @Override
    public boolean hasBooks() {
        if (jsonBooks.count().block() == null) {
            return false;
        }
        return jsonBooks.count().block() > 0;
    }


    /**
     * Determines if an entry is a file
     */
    boolean isFile(URI entry) {
        if (entry == null) {
            return false;
        }
        File fh = new File(entry);
        return fh.isFile();
    }

    /**
     * Converts a string to a File and then returns the file as a uri for the
     * Book image
     *
     * @param path - String containing the image file the user entered
     * @return URI - created from the converted file
     */
    @Override
    public URI retrieveURI(String path) {
        return new File(path).toURI();
    }

}
