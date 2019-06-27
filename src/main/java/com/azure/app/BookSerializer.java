package com.azure.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class BookSerializer {

    public void writeJSON(Book book) {
        try {
            File dir = new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib\\" + book.getAuthor());
            dir.mkdir();
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File(dir.getAbsolutePath() + "\\" + book.getTitle() + ".json"), book);
        } catch (IOException ex) {
            System.out.println("Error in File Handling. Did not save.");
        }
    }

}
