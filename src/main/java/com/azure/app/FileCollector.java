// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class FileCollector implements BookCollection {
    private final JsonHandler serializer;

    FileCollector() {
        serializer = new JsonHandler();
    }

    /**
     * Reads all the JSON files and then saves their informations in new Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    @Override
    public Flux<Book> registerBooks() {
        Flux<Book> savedBook = Flux.empty();
        try (Stream<Path> walk = Files.walk(Paths.get("C:\\Users\\t-katami\\Documents\\intern-project\\lib"))) {
            List<String> result = walk.map(x -> x.toString()).filter(f -> f.endsWith(".json")).collect(Collectors.toList());
            if (result.isEmpty() || result == null) {
                return Flux.empty();
            }
            savedBook = Flux.create(bookFluxSink -> {
                for (int i = 0; i < result.size(); i++) {
                    bookFluxSink.next(new JsonHandler().fromJSONtoBook(new File(result.get(i))));
                }
                bookFluxSink.complete();

            }, FluxSink.OverflowStrategy.BUFFER);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return savedBook;
    }

    /**
     * Saves the book to a JSON file.
     *
     * @param title  - String with the title of the book
     * @param author - Author of the book
     * @param path   - the File path
     * @param choice - String containing y/n about whether the user wants to save it or not
     */
    @Override
    public Mono<Boolean> saveBook(String title, Author author, File path, String choice) {
        if (!choice.equalsIgnoreCase("Y")) {
            return Mono.just(false);
        }

        Mono<Book> book = Mono.just(new Book(title, author, path));

        return book.map(x -> {
            return serializer.writeJSON(x);
        });
    }
}
