// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.storage.blob.BlockBlobAsyncClient;
import com.azure.storage.blob.ContainerAsyncClient;
import com.azure.storage.blob.StorageAsyncClient;
import com.azure.storage.blob.StorageClient;
import com.azure.storage.common.credentials.SharedKeyCredential;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public class BlobBookCollector implements BookCollection {
    private final Set<String> supportedImageFormats;
    private StorageAsyncClient storageClient;
    private ContainerAsyncClient imageContainerClient;
    private ContainerAsyncClient bookContainerClient;
    private BlockBlobAsyncClient blockBlobClient;
    private static Logger logger = LoggerFactory.getLogger(BlobBookCollector.class);

    BlobBookCollector() {
        String accountName = System.getenv("BLOB_ACCOUNTNAME");
        String accountKey = System.getenv("BLOB_KEY");

        SharedKeyCredential credential = new SharedKeyCredential(accountName, accountKey);
        String endPoint = String.format(Locale.ROOT, System.getenv("BLOB_URL"));

        storageClient = StorageClient.storageClientBuilder()
            .endpoint(endPoint)
            .credential(credential)
            .buildAsyncClient();

        bookContainerClient = storageClient.getContainerAsyncClient("book-library");
        imageContainerClient = storageClient.getContainerAsyncClient("book-covers");

        bookContainerClient.exists().subscribe(x -> {
            if (!x.value()) {
                bookContainerClient.create();
            }
        });

        imageContainerClient.exists().subscribe(x -> {
            if (!x.value()) {
                imageContainerClient.create();
            }
        });

        supportedImageFormats = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("gif", "png", "jpg")));
    }

    /**
     * Returns the Flux of Book objects
     *
     * @return Flux<Book> the flux with all the book information. {@literal Flux<Book>}
     */
    @Override
    public Flux<Book> getBooks() {

        return bookContainerClient.listBlobsFlat().flatMap(blob -> {
            blockBlobClient = bookContainerClient.getBlockBlobAsyncClient(blob.name());
            File temporaryBookHolder = new File(blob.name());
            try {
                temporaryBookHolder.createNewFile();
            } catch (IOException e) {
                logger.error("Error creating file.", e);
                return Mono.error(e);
            }
            return blockBlobClient.downloadToFile(temporaryBookHolder.getAbsolutePath())
                .then(Mono.fromCallable(() -> {
                    Book b = Constants.SERIALIZER.fromJSONtoBook(temporaryBookHolder);
                    temporaryBookHolder.delete();
                    return b;
                }));
        });
    }


    @Override
    public Mono<Boolean> saveBook(String title, Author author, URI path) {
        File relativePath = Paths.get(Constants.IMAGE_PATH, author.getLastName(), author.getFirstName(), new File(path).getName()).toFile();
        URI saved = relativePath.toURI();
        URI relative = new File(System.getProperty("user.dir")).toURI().relativize(saved);
        File bookFile = Constants.SERIALIZER.writeJSON(new Book(title, author, relative));
        if (bookFile == null) {
            return Mono.just(false);
        }
        saveImage(new File(path));
        String blobName = null;
        try {
            blobName = URLEncoder.encode(bookFile.getName(), StandardCharsets.US_ASCII.toString());
        } catch (UnsupportedEncodingException e) {
            return Mono.error(e);
        }
        blockBlobClient = bookContainerClient.getBlockBlobAsyncClient(blobName);
        return blockBlobClient.uploadFromFile(bookFile.getAbsolutePath()).then(Mono.fromCallable(() -> {
            bookFile.delete();
            return true;
        }));
    }

    private boolean saveImage(File imagePath) {
        String extension = FilenameUtils.getExtension(imagePath.getName());
        if (!supportedImageFormats.contains(extension)) {
            return false;
        }
        try {
            BufferedImage bufferedImage = ImageIO.read(imagePath);
            File image = new File(imagePath.getName());
            File savedImage = image;
            if (ImageIO.write(bufferedImage, extension, image)) {
                blockBlobClient = imageContainerClient.getBlockBlobAsyncClient(image.getName());
                blockBlobClient.uploadFromFile(savedImage.getPath()).subscribe(Stream.builder()::accept);
                return true;
            }
        } catch (IOException ex) {
            logger.error("Error saving image: ", ex);
            return false;
        }
        return false;
    }

    /**
     * Deletes the book and the file based off its information.
     *
     * @param book - Book that'll be deleted
     * @return Mono<Boolean> determines whether or not book was successfully deleted </Boolean>
     * true - Book was deleted
     * false - Book wasn't deleted
     */
    @Override
    public Mono<Boolean> deleteBook(Book book) {
        return null;
    }

    /**
     * Filters out the book based on the specified title.
     *
     * @param title - String of the book title the user is looking for
     * @return - Flux of Book objects with that title
     */
    @Override
    public Flux<Book> findBook(String title) {
        return null;
    }

    /**
     * Filters out the books based on the specified author.
     *
     * @param author - Contains the name of the author the user is looking for
     * @return - Flux of Book objects by that author
     */
    @Override
    public Flux<Book> findBook(Author author) {
        return null;
    }

    /**
     * Determines whether the collection has books or not.
     *
     * @return {@Link Mono<Boolean>} - true if there are books
     * false - if there are no books
     */
    @Override
    public Mono<Boolean> hasBooks() {
        return null;
    }

    /**
     * Converts the path to a file and then returns the URI to that path
     *
     * @param path - String containing the image file the user entered
     * @return URI of the image path
     */
    @Override
    public URI retrieveURI(String path) {
        return new File(path).toURI();
    }
}
