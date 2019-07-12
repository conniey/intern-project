// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class JsonHandler {
    private static Logger logger = LoggerFactory.getLogger(JsonHandler.class);


    /**
     * Converts a json file back to a Book object
     *
     * @param jsonFile - the JSON file to be converted
     * @return Book - created from the JSON file
     */
    Book fromJSONtoBook(File jsonFile) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            Book b = mapper.readValue(jsonFile, Book.class);
            return b;
        } catch (JsonGenerationException e) {
            logger.error("Error generating JSON file: ", e);
        } catch (JsonMappingException e) {
            logger.error("Error mapping JSON file: ", e);
        } catch (IOException e) {
            logger.error("Error while writing JSON file: ", e);
        }
        return null;
    }

    /**
     * Converts a Book object a json object and stores it in a file.
     *
     * @param book - the Book object that's going to be converted to a json file
     * @return boolean - true if Book was successfully converted to Javadoc
     * false if Book wasn't successfully converted to Javadoc
     */
    boolean writeJSON(Book book, String root) {
        if (book.checkBook()) {
            final Path fullBookPath = Paths.get(root, Constants.JSON_PATH, book.getAuthor().getLastName(),
                book.getAuthor().getFirstName());
            final File bookFile = fullBookPath.toFile();
            if (!bookFile.exists() && !bookFile.mkdirs()) {
                logger.error("Could not create directories for: " + fullBookPath.toString());
            }
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(Paths.get(bookFile.getPath(), book.getTitle() + ".json").toFile(), book);
                return true;
            } catch (IOException ex) {
                logger.error("Couldn't find the right file: ", ex);
                return false;
            }
        } else {
            return false;
        }
    }
}
