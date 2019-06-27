package com.azure.app;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BookSerializer {
    BookSerializer() {
    }

    private void writeJSON(Book book) {
        ObjectMapper mapper = new ObjectMapper();
    }
}
