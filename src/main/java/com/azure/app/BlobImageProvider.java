// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlockBlobAsyncClient;
import com.azure.storage.blob.ContainerAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.common.credentials.SharedKeyCredential;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class BlobImageProvider implements ImageProvider {
    private final Set<String> supportedImageFormats;
    private Mono<ContainerAsyncClient> imageContainerClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobImageProvider.class);

    BlobImageProvider(BlobSettings blobSettings) {
        SharedKeyCredential credential = new SharedKeyCredential(blobSettings.getAccountName(),
            blobSettings.getKey());
        BlobServiceAsyncClient storageAsyncClient = new BlobServiceClientBuilder()
            .endpoint(blobSettings.getUrl())
            .credential(credential)
            .buildAsyncClient();
        ContainerAsyncClient container = storageAsyncClient.getContainerAsyncClient("book-covers");
        imageContainerClient = container.exists().flatMap(exists -> exists.value() ? Mono.just(container)
            : container.create().then(Mono.just(container))).cache();
        supportedImageFormats = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("gif", "png", "jpg")));
    }

    /**
     * Retrieve valid names for the Blobs made
     *
     * @param author   - Author of the book will make up the folders the Blob will be contained in
     * @param fileName - the title of the main blob
     * @return String array with all the valid names
     */
    private String[] getBlobInformation(Author author, String fileName) {
        String blobLastName = author.getLastName().replace(' ', '-');
        String blobFirstName = author.getFirstName().replace(' ', '-');
        String blobName = fileName.replace(' ', '-');
        try {
            blobLastName = URLEncoder.encode(blobLastName, StandardCharsets.US_ASCII.toString());
            blobName = URLEncoder.encode(blobName, StandardCharsets.US_ASCII.toString());
            blobFirstName = URLEncoder.encode(blobFirstName, StandardCharsets.US_ASCII.toString());
            String apostrophe = URLEncoder.encode("'", StandardCharsets.US_ASCII.toString());
            if (blobFirstName.contains(apostrophe)) {
                blobFirstName = blobFirstName.replace(apostrophe, "'");
            }
            if (blobLastName.contains(apostrophe)) {
                blobLastName = blobLastName.replace(apostrophe, "'");
            }
            if (blobName.contains(apostrophe)) {
                blobName = blobName.replace(apostrophe, "'");
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Error encoding names: ", e);
            return null;
        }
        return new String[]{blobName, blobFirstName, blobLastName};
    }

    private Mono<Void> duplicateImages(Book b) {
        return deleteImage(b).onErrorResume(error -> Mono.empty());
    }

    @Override
    public Mono<Void> saveImage(Book b) {
        final String extension = FilenameUtils.getExtension(new File(b.getCover()).getName());
        if (!supportedImageFormats.contains(extension)) {
            return Mono.error(new IllegalStateException("Error. Wrong file format for image"));
        }
        String[] blobInfo = getBlobInformation(b.getAuthor(), b.getTitle() + "." + extension);
        if (blobInfo == null) {
            return Mono.error(new IllegalArgumentException("Error encoding blob name"));
        }
        return duplicateImages(b) //just in case this book already exists, this should delete the old file
            .then(imageContainerClient.flatMap(containerAsyncClient -> {
                final BlockBlobAsyncClient blockBlobClient = containerAsyncClient.getBlockBlobAsyncClient(blobInfo[2] + "/" + blobInfo[1]
                    + "/" + blobInfo[0]);
                File path = new File(b.getCover());
                return blockBlobClient.uploadFromFile(path.getAbsolutePath()).then();
            }));
    }

    @Override
    public Mono<Void> editImage(Book oldBook, Book newBook, boolean saveCover) {
        if (saveCover) {
            String[] blobConversion = getBlobInformation(oldBook.getAuthor(), oldBook.getTitle());
            Mono<BlobItem> bookMono = locateImage(blobConversion);
            return imageContainerClient.flatMap(containerAsyncClient ->
                bookMono.flatMap(blobItem -> {
                    final BlockBlobAsyncClient blockBlob = containerAsyncClient.getBlockBlobAsyncClient(blobItem.name());
                    String property = "java.io.tmpdir";
                    String tempDir = System.getProperty(property);
                    String newFileName;
                    try {
                        newFileName = URLEncoder.encode(newBook.getTitle(), StandardCharsets.US_ASCII.toString());
                    } catch (UnsupportedEncodingException e) {
                        LOGGER.error("Error encoding file: ", e);
                        return Mono.error(e);
                    }
                    String extension = blobItem.name().substring(blobItem.name().lastIndexOf("."));
                    File newFile = new File(tempDir + newFileName + extension);
                    try {
                        newFile.createNewFile();
                    } catch (IOException e) {
                        LOGGER.error("Exception creating the file: ", e);
                        return Mono.error(e);
                    }
                    Book saveBook = new Book(newBook.getTitle(), newBook.getAuthor(), newFile.toURI());
                    return blockBlob.downloadToFile(newFile.getAbsolutePath()).then(deleteImage(oldBook)).then(saveImage(saveBook));
                }));
        } else { // User selected 3 - change image
            return deleteImage(oldBook).then(saveImage(newBook));
        }
    }

    /**
     * Deletes the image cover from the Blob Storage
     *
     * @param book - Book with the information for the cover that will be deleted.
     * @return Mono {@Link Boolean} determines whether image was sucessfully deleted or not
     * true - Book was deleted
     * false - Book wasn't deleted
     */
    public Mono<Void> deleteImage(Book book) {
        String[] blobConversion = getBlobInformation(book.getAuthor(), book.getTitle());
        Mono<BlobItem> file = locateImage(blobConversion);
        return imageContainerClient.flatMap(containerAsyncClient ->
            file.flatMap(blobItem -> {
                final BlockBlobAsyncClient blob = containerAsyncClient.getBlockBlobAsyncClient(blobItem.name());
                return blob.delete().then();
            }));
    }

    /**
     * Grab a String containing the absolute path to the book's cover location
     * If it's in Azure Database storage, the cover will be downloaded to the temporary directory.
     *
     * @param book - Book object of whose cover you want to retrieve
     * @return {@Link Mono} holds a String of the absolute path
     */
    @Override
    public Mono<String> grabCoverImage(Book book) {
        String[] blobInfo = getBlobInformation(book.getAuthor(), book.getTitle());
        Mono<BlobItem> file = locateImage(blobInfo);
        return imageContainerClient.flatMap(containerAsyncClient ->
            file.flatMap(blobItem -> {
                final BlockBlobAsyncClient blockBlob = containerAsyncClient.getBlockBlobAsyncClient(blobItem.name());
                String property = "java.io.tmpdir";
                String tempDir = System.getProperty(property);
                File newFile = new File(tempDir + blobItem.name().
                    substring(blobItem.name().lastIndexOf("/") + 1));
                try {
                    newFile.createNewFile();
                } catch (IOException e) {
                    LOGGER.error("Exception creating the file: ", e);
                    return Mono.error(e);
                }
                return blockBlob.downloadToFile(newFile.getAbsolutePath()).then(Mono.fromCallable(() ->
                    newFile.getAbsolutePath() + "\n\tThis was downloaded and saved to the user's TEMP folder."));
            }));
    }

    /**
     * Locates the specified image based on author and title
     *
     * @param blobConversion - String array containing the necessary information
     * @return - a single blob item of the image - there should never be more than one because duplicateImage should prevent that
     */
    private Mono<BlobItem> locateImage(String[] blobConversion) {
        Flux<BlobItem> findFiles = imageContainerClient.flatMapMany(containerAsyncClient
            -> containerAsyncClient.listBlobsFlat().filter(blobItem ->
            blobItem.name().contains(blobConversion[2] + "/"
                + blobConversion[1]
                + "/" + blobConversion[0] + ".")));
        return findFiles.hasElements().flatMap(exists -> exists ? findFiles.elementAt(0)
            : Mono.error(new IllegalStateException("Cannot find the image.")));
    }
}
