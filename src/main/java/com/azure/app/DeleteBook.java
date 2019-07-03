package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;

public class DeleteBook {
    // TODO crate delete class by splitting up the function
    // TODO create BookCollection
    //
    private FindBook findBook = new FindBook(new FileCollector().registerBooks());
    private Flux<Book> results;
    private long size;

    public Flux<Book> lookupTitle(String title) {
        results = findBook.findTitles(title);
        results.collectList().map(list -> {
            if (list == null) {
                size = 0;
            } else {
                size = list.size();
            }
            return list;
        });
        return findBook.findTitles(title);
    }

    public long getSize() {
        return size;
    }

    public Mono<Void> displayResults() {
        return results.collectList().map(list -> {
            for (int i = 0; i < list.size(); i++) {
                System.out.println(i + 1 + ". " + list.get(i));
            }
            return list;
        }).then();
    }

    public void deleteEmptyDirectories() {
        File[] files = new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib").listFiles();
        clearFiles(files);
    }

    public void clearFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                clearFiles(file.listFiles());
            }
            if (file.length() == 0 && !file.getAbsolutePath().contains(".json")) {
                file.delete();
            }
        }
    }

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
    }
}
