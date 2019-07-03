// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class BookSerializer {

    /**
     * Converts a Book object a json object and stores it in a file.
     *
     * @param book - the Book object that's going to be converted to a json file
     * @return boolean - true if Book was successfully converted to Javadoc
     *                    false if Book wasn't successfully converted to Javadoc
     */
    public boolean writeJSON(Book book) {
        if (book.checkBook()) {
            try {

                File dir = new File("C:\\Users\\t-katami\\Documents\\intern-project\\lib\\"
                    + book.getAuthor().getLastName());
                dir.mkdir();
                File dir2 = new File(dir.getAbsolutePath() + "\\" + book.getAuthor().getFirstName());
                dir2.mkdir();
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(new File(dir2.getAbsolutePath() + "\\" + book.getTitle() + ".json"), book);
                return true;
            } catch (IOException ex) {
                return false;
            }
        } else {
            return false;
        }
    }
}
