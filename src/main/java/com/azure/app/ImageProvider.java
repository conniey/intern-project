package com.azure.app;

import reactor.core.publisher.Mono;

public interface ImageProvider {
    /**
     * Grab a String containing the absolute path to the book's cover location
     * If it's in Azure Database storage, the cover will be downloaded to the temporary directory.
     *
     * @param book - Book object of whose cover you want to retrieve
     * @return {@Link Mono} holds a String of the absolute path
     */
    Mono<String> grabCoverImage(Book book);

    /**
     * Saves the book's cover image to a Blob Storage
     *
     * @param b - Book object with the cover image to save
     * @return {@Mono Void}
     */
    Mono<Void> saveImage(Book b);

    Mono<Void> editImage(Book oldBook, Book newBook, int saveCover);

    /**
     * Deletes the image cover from the Blob Storage
     *
     * @param book - Book with the information for the cover that will be deleted.
     * @return Mono {@Link Boolean} determines whether image was sucessfully deleted or not
     * true - Book was deleted
     * false - Book wasn't deleted
     */
    Mono<Void> deleteImage(Book book);
}
