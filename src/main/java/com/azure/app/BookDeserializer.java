package com.azure.app;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class BookDeserializer {
    public static void fromJsonToObject() {
        try {
            FileReader fh = new FileReader("Book.json");
        } catch (FileNotFoundException ex) {

        }
    }
}
