// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;

import java.io.File;

public class DeleteBook {

    private FilterBooks findBook = new FilterBooks(new FileCollector().registerBooks());

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
        File[] files = new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib").listFiles();
        clearFiles(files);
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
     */
    public void deleteFile(File[] files, Book book) {
        for (File file : files) {
            if (file.isDirectory()) {
                deleteFile(file.listFiles(), book);
            } else {
                if (new OptionChecker().checkFile(file, book)) {
                    if (file.delete()) {
                        System.out.println("Book is deleted.");
                    }
                }
            }
        }
        deleteEmptyDirectories();
    }
}
