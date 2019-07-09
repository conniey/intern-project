// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

class DeleteBook {
    private final BookCollection collection;
    private FilterBooks findBook;
    private Constants constants;

    DeleteBook(BookCollection collection) {
        this.collection = collection;
        findBook = new FilterBooks(this.collection);
    }

    /**
     * Searches for all the books with the specified title and then returns them.
     *
     * @param title - String with the title the user asked for
     * @return - Flux<Book> contains the books with said title </Book>
     */
    Flux<Book> lookupTitle(String title) {
        return findBook.findTitles(title);
    }

    /**
     * Clears out any empty directories that might have been leftover from when the JSON file was deleted.
     */
    void deleteEmptyDirectories() {
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
    void clearFiles(File[] files) {
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
     * @param book - Book object with the information about the file you want to delete
     * @return boolean - true if file was sucessfully deleted
     * false - otherwise
     */
    boolean deleteFile(Book book) {
        List<File> jsonFiles = collection.traverseJsonFiles();
        if (jsonFiles != null) {
            for (int i = 0; i < jsonFiles.size(); i++) {
                File json = jsonFiles.get(i);
                if (new OptionChecker().checkFile(json, book)) {
                    if (json.delete()) {
                        book.getCover().delete();
                        deleteEmptyDirectories();
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
