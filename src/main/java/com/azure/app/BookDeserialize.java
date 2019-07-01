// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;


public class BookDeserialize {

    public Book fromJSONtoBook(File jsonFile) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            Book b = mapper.readValue(jsonFile, Book.class);
            return b;
        } catch (JsonGenerationException e) {
            System.out.println("Couldn't generate.");
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error.");
        }
        return null;
    }

    public void iterateThroughLibrary() {
        File[] files = new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib").listFiles();
        showFiles(files);
    }

    public void showFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                showFiles((file.listFiles()));
            } else {
                Book book = fromJSONtoBook(file);
                System.out.println(book);
            }
        }
    }
}
