// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

final class LocalImageProvider implements ImageProvider {
    private final Set<String> supportedImageFormats;
    private List<File> jsonFiles;
    private String root;
    private final OptionChecker optionChecker = new OptionChecker();
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalImageProvider.class);

    LocalImageProvider(String root) {
        this.root = root;
        supportedImageFormats = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("gif", "png", "jpg")));
        File directory = new File(Paths.get(root, Constants.IMAGE_PATH).toString());
        if (!directory.exists() && !directory.mkdirs()) {
            LOGGER.error("Couldn't create non-existent JSON directory: " + directory.getAbsolutePath());
        }
        jsonFiles = retrieveJsonFiles();
    }

    /**
     * Deletes the old book if the new book has a the same title and author
     *
     * @param bookToCompare
     */
    private void duplicateImage(Book bookToCompare, File newImage) {
        //Checks to see if the book has a duplicate, if so it'll delete it so it can be overwritten
        jsonFiles.removeIf(x -> {
            boolean result = optionChecker.checkFile(x, bookToCompare);
            if (result) {
                x.delete();
                Book imageToSave = new Book(bookToCompare.getTitle(), bookToCompare.getAuthor(),
                    newImage.toURI());
                saveImage(imageToSave);
                return true;
            }
            return false;
        });
    }

    private List<File> retrieveJsonFiles() {
        try (Stream<Path> walk = Files.walk(Paths.get(Constants.IMAGE_PATH))) {
            return walk.map(Path::toFile).filter(f -> f.getName().endsWith("gif") || f.getName().endsWith("png")
                || f.getName().endsWith("jpg"))
                .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Exception acquiring image file.", e);
            return Collections.emptyList();
        }
    }

    /**
     * Clears out any empty directories that might have been leftover from when the JSON file was deleted.
     */
    private void deleteEmptyDirectories() {
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
     * Grab a String containing the absolute path to the book's cover location
     * If it's in Azure Database storage, the cover will be downloaded to the temporary directory.
     *
     * @param book - Book object of whose cover you want to retrieve
     * @return {@Link Mono} holds a String of the absolute path
     */
    @Override
    public Mono<String> grabCoverImage(Book book) {
        return Mono.just(Paths.get(System.getProperty("user.dir"), book.getCover().getPath()).toString());
    }

    @Override
    public Mono<Void> saveImage(Book book) {
        File imagePath = new File(book.getCover());
        final Path fullImagePath = Paths.get(root, Constants.IMAGE_PATH, book.getAuthor().getLastName(),
            book.getAuthor().getFirstName());
        File imageFile = fullImagePath.toFile();
        if (!imageFile.exists() && !imageFile.mkdirs()) {
            LOGGER.error("Couldn't create directories for: " + imageFile.getAbsolutePath());
        }
        duplicateImage(book, imagePath);
        String extension = FilenameUtils.getExtension(imagePath.getName());
        if (!supportedImageFormats.contains(extension)) {
            LOGGER.error("Error. Wrong image format.");
            return Mono.error(new IllegalArgumentException("Wrong image format"));
        }
        try {
            BufferedImage bufferedImage = ImageIO.read(imagePath);
            String safeTitle = book.getTitle().replace(' ', '-');
            File image = new File(Paths.get(imageFile.getPath(), safeTitle + "." + extension).toString());
            if (ImageIO.write(bufferedImage, extension, image)) {
                return Mono.empty().then();
            }
        } catch (IOException ex) {
            LOGGER.error("Error saving image: ", ex);
            return Mono.error(ex);
        }
        return Mono.error(new IllegalArgumentException("Error saving cover image"));
    }

    @Override
    public Mono<Void> editImage(Book oldBook, Book newBook, int saveCover) {
        if (saveCover == 1) { // Overwriting/changing cover
            return saveImage(newBook);
        } else {
            File image = Paths.get(System.getProperty("user.dir"), oldBook.getCover().getPath()).toFile();
            newBook = new Book(newBook.getTitle(), newBook.getAuthor(), image.toURI());
            return saveImage(newBook).then(deleteImage(oldBook));
        }
    }

    @Override
    public Mono<Void> deleteImage(Book book) {
        return Mono.just(Paths.get(System.getProperty("user.dir"), book.getCover().getPath()).toFile().delete()).map(result -> {
            if (!result.booleanValue()) {
                return Mono.error(new IllegalStateException("Image wasn't deleted."));
            } else {
                deleteEmptyDirectories();
                return Mono.empty();
            }
        }).then();
    }
}
