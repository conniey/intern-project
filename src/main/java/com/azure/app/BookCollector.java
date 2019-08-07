package com.azure.app;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;

public class BookCollector {
    private ImageProvider imageProvider;
    private DocumentProvider documentProvider;

    interface DocumentProvider {
        /**
         * Returns the Flux of Book objects
         *
         * @return Flux<Book> the flux with all the book information </Book>
         */
        Flux<Book> getBooks();

        /**
         * Saves the book as a JSON file
         *
         * @param title  - String containing the title of the book
         * @param author - Author object of the book
         * @param path   - File containing the cover image of the book
         * @return Mono<Boolean> that determines whether the book got saved or not
         * true - book was successfully saved
         * false - book wasn't saved </Boolean>
         */
        Mono<Void> saveBook(String title, Author author, URI path);

        Mono<Void> editBook(Book oldBook, Book newBook, int saveCover);

        /**
         * Deletes the book and the file based off its information.
         *
         * @param book - Book that'll be deleted
         * @return Mono<Boolean> determines whether or not book was successfully deleted </Boolean>
         * true - Book was deleted
         * false - Book wasn't deleted
         */
        Mono<Void> deleteBook(Book book);

        /**
         * Filters out the book based on the specified title.
         *
         * @param title - String of the book title the user is looking for
         * @return - Flux of Book objects with that title
         */
        Flux<Book> findBook(String title);

        /**
         * Filters out the books based on the specified author.
         *
         * @param author - Contains the name of the author the user is looking for
         * @return - Flux of Book objects by that author
         */
        Flux<Book> findBook(Author author);
    }

    interface ImageProvider {
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

    public BookCollector(DocumentProvider documentCollection, ImageProvider imageProvider) {
        this.documentProvider = documentCollection;
        this.imageProvider = imageProvider;
    }

    /**
     * Saves the book to the storage system.
     *
     * @param book - Book object that's going to be saved
     * @return {@Link Mono} returns a Mono that was successful or has an error
     */
    Mono<Void> saveBook(Book book) {
        if (book.checkBook() && isFile(book.getCover())) {
            return documentProvider.saveBook(book.getTitle(), book.getAuthor(), book.getCover())
                .then(imageProvider.saveImage(book));
        }
        return Mono.error(new IllegalArgumentException("Book can't be saved."));
    }

    /**
     * Returns the Flux of Book objects
     *
     * @return Flux<Book> the flux with all the book information </Book>
     */
    Flux<Book> getBooks() {
        return documentProvider.getBooks();
    }


    Mono<Void> editBook(Book oldBook, Book newBook, int saveCover) {
        return documentProvider.editBook(oldBook, newBook, saveCover).
            then(imageProvider.editImage(oldBook, newBook, saveCover));
    }

    /**
     * Deletes the book and the file based off its information.
     *
     * @param book - Book that'll be deleted
     * @return Mono<Boolean> determines whether or not book was successfully deleted </Boolean>
     * true - Book was deleted
     * false - Book wasn't deleted
     */
    Mono<Void> deleteBook(Book book) {
        if (book.checkBook()) {
            return documentProvider.deleteBook(book).then(imageProvider.deleteImage(book));
        }
        return Mono.error(new IllegalArgumentException("Book wasn't deleted."));
    }

    /**
     * Filters out the book based on the specified title.
     *
     * @param title - String of the book title the user is looking for
     * @return - Flux of Book objects with that title
     */
    Flux<Book> findBook(String title) {
        return documentProvider.findBook(title);
    }

    /**
     * Filters out the books based on the specified author.
     *
     * @param author - Contains the name of the author the user is looking for
     * @return - Flux of Book objects by that author
     */
    Flux<Book> findBook(Author author) {
        return documentProvider.findBook(author);
    }

    /**
     * Determines whether the collection has books or not.
     *
     * @return {@Link Mono<Boolean>} - true if there are books
     * false - if there are no books
     */
    Mono<Boolean> hasBooks() {
        return getBooks().hasElements();
    }

    /**
     * Converts the path to a file and then returns the URI to that path
     *
     * @param path - String containing the image file the user entered
     * @return URI of the image path
     */
    URI retrieveURI(String path) {
        return new File(path).toURI();
    }

    /**
     * Grab a String containing the absolute path to the book's cover location
     * If it's in Azure Database storage, the cover will be downloaded to the temporary directory.
     *
     * @param book - Book object of whose cover you want to retrieve
     * @return {@Link Mono} holds a String of the absolute path
     */
    Mono<String> grabCoverImage(Book book) {
        return imageProvider.grabCoverImage(book);
    }

    /**
     * Determines if an entry is a file
     */
    private boolean isFile(URI entry) {
        if (entry == null) {
            return false;
        }
        File fh = new File(entry);
        return fh.isFile();
    }
}
