// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeleteBook {

    private FilterBooks findBook = new FilterBooks(new LocalBookCollector().registerBooks());

    /**
     * Searches for all the books with the specified title and then returns them.
     *
     * @param title - String with the title the user asked for
     * @return - Flux<Book> contains the books with said title </Book>
     */
    public Flux<Book> lookupTitle(String title) {
        return findBook.findTitles(title);
    }

    /**
     * Clears out any empty directories that might have been leftover from when the JSON file was deleted.
     */
    public void deleteEmptyDirectories() {
        File[] files = new File("\\lib\\jsonFiles\\").listFiles();
        clearFiles(files);
        File[] imageFiles = new File("\\lib\\images\\").listFiles();
        clearFiles(imageFiles);
    }

    /**
     * Assists the emptyDirectory method by traversing through the files. When it finds an empty directory without a
     * JSON file, it deletes that file.
     *
     * @param files - the folder containing the other files in the library.
     */
    public void clearFiles(File[] files) {
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
     * Deletes the JSON file.
     *
     * @param files - the folder containing the other files in the library
     * @param book  - Book object with the information about the file you want to delete
     * @return boolean - true if file was sucessfully deleted
     *                   false - otherwise
     */
    public boolean deleteFile(File files, Book book) {
        try (Stream<Path> walk = Files.walk(Paths.get(files.getAbsolutePath()))) {
            List<String> result = walk.map(x -> x.toString()).filter(f -> f.endsWith(".json")).collect(Collectors.toList());
            for (String file : result) {
                File newFile = new File(file);
                if (new OptionChecker().checkFile(newFile, book)) {
                    if (newFile.delete()) {
                        book.getCover().delete();
                        deleteEmptyDirectories();
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
