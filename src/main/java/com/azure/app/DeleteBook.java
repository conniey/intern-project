package com.azure.app;

import java.io.File;

public class DeleteBook {
    // TODO crate delete class by splitting up the function
    // TODO create BookCollection
    //


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
