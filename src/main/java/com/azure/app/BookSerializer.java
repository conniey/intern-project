package com.azure.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class BookSerializer {

    public void writeJSON(Book book) {
        try {
            File dir = new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib\\" + book.getLastName());
            dir.mkdir();
            File dir2 = new File(dir.getAbsolutePath() + "\\" + book.getFirstName());
            dir2.mkdir();
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(new File(dir2.getAbsolutePath() + "\\" + book.getTitle() + ".json"), book);
        } catch (IOException ex) {
            System.out.println("Error in File Handling. Did not save.");
        }
    }

}
