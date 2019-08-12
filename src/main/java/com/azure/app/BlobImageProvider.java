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
import java.util.Locale;
import java.util.Set;

final class BlobImageProvider implements ImageProvider {
    private final Set<String> supportedImageFormats;
    private Mono<ContainerAsyncClient> imageContainerClient;
<<<<<<< HEAD
    private static Logger logger = LoggerFactory.getLogger(BlobImageProvider.class);
=======
    private static Logger logger = LoggerFactory.getLogger(JsonHandler.class);
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6

    BlobImageProvider(BlobSettings blobSettings) {
        SharedKeyCredential credential = new SharedKeyCredential(blobSettings.getAccountName(),
            blobSettings.getKey());
        String endpoint = String.format(Locale.ROOT, blobSettings.getUrl());
        BlobServiceAsyncClient storageAsyncClient = new BlobServiceClientBuilder()
            .endpoint(endpoint)
            .credential(credential)
            .buildAsyncClient();
<<<<<<< HEAD
        ContainerAsyncClient container = storageAsyncClient.getContainerAsyncClient("book-covers");
        imageContainerClient = container.exists().flatMap(exists -> exists.value() ? Mono.just(container)
            : container.create().then(Mono.just(container))).cache();
=======
        ContainerAsyncClient container = storageAsyncClient.getContainerAsyncClient("cosmos-book-covers");
        imageContainerClient = container.exists().flatMap(exists -> {
            if (exists.value()) {
                return Mono.just(container);
            } else {
                return container.create().then(Mono.just(container));
            }
        });
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
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
<<<<<<< HEAD
        String blobLastName = author.getLastName().replace(' ', '-');
        String blobFirstName = author.getFirstName().replace(' ', '-');
        String blobName = fileName.replace(' ', '-');
        try {
            blobLastName = URLEncoder.encode(blobLastName, StandardCharsets.US_ASCII.toString());
            blobName = URLEncoder.encode(blobName, StandardCharsets.US_ASCII.toString());
            blobFirstName = URLEncoder.encode(blobFirstName, StandardCharsets.US_ASCII.toString());
=======
        String blobLastName,
            blobFirstName,
            blobName;
        try {
            blobLastName = URLEncoder.encode(author.getLastName().toLowerCase(), StandardCharsets.US_ASCII.toString());
            blobName = URLEncoder.encode(fileName, StandardCharsets.US_ASCII.toString());
            blobFirstName = URLEncoder.encode(author.getFirstName().toLowerCase(), StandardCharsets.US_ASCII.toString());
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
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
            logger.error("Error encoding names: ", e);
            return null;
        }
        return new String[]{blobName, blobFirstName, blobLastName};
    }

<<<<<<< HEAD
    private Mono<Void> duplicateImages(Book b) {
        return deleteImage(b).onErrorResume(error -> Mono.empty());
    }

    @Override
    public Mono<Void> saveImage(Book b) {
        final String extension = FilenameUtils.getExtension(new File(b.getCover()).getName());
=======
    @Override
    public Mono<Void> saveImage(Book b) {
        String extension = FilenameUtils.getExtension(new File(b.getCover()).getName());
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
        if (!supportedImageFormats.contains(extension)) {
            return Mono.error(new IllegalStateException("Error. Wrong file format for image"));
        }
        String[] blobInfo = getBlobInformation(b.getAuthor(), b.getTitle() + "." + extension);
        if (blobInfo == null) {
            return Mono.error(new IllegalArgumentException("Error encoding blob name"));
        }
<<<<<<< HEAD
        return duplicateImages(b) //just in case this book already exists, this should delete the old file
            .then(imageContainerClient.flatMap(containerAsyncClient -> {
                final BlockBlobAsyncClient blockBlobClient = containerAsyncClient.getBlockBlobAsyncClient(blobInfo[2] + "/" + blobInfo[1]
                    + "/" + blobInfo[0]);
                File path = new File(b.getCover());
                return blockBlobClient.uploadFromFile(path.getAbsolutePath()).then();
            }));
=======
        return imageContainerClient.flatMap(containerAsyncClient -> {
            final BlockBlobAsyncClient blockBlobClient = containerAsyncClient.getBlockBlobAsyncClient(blobInfo[2] + "/" + blobInfo[1]
                + "/" + blobInfo[0]);
            File path = new File(b.getCover());
            return blockBlobClient.uploadFromFile(path.getAbsolutePath()).then();
        });
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
    }

    @Override
    public Mono<Void> editImage(Book oldBook, Book newBook, int saveCover) {
<<<<<<< HEAD
        if (saveCover == 1) { //don't want to change image
            return deleteImage(oldBook).then(saveImage(newBook));
        } else {
            String[] blobConversion = getBlobInformation(oldBook.getAuthor(), oldBook.getTitle());
            Mono<BlobItem> bookMono = locateImage(blobConversion);
=======
        if (saveCover == 1) {
            return deleteImage(oldBook).then(saveImage(newBook));
        } else {
            String[] blobConversion = getBlobInformation(oldBook.getAuthor(), oldBook.getTitle());
            Flux<BlobItem> file = imageContainerClient.flatMapMany(containerAsyncClient ->
                containerAsyncClient.listBlobsFlat().filter(blobItem -> {
                    assert blobConversion != null;
                    return blobItem.name().contains(blobConversion[2] + "/"
                        + blobConversion[1]
                        + "/" + blobConversion[0] + ".");
                }));
            Mono<BlobItem> bookMono = file.hasElements().flatMap(notEmpty -> notEmpty ? file.elementAt(0)
                : Mono.error(new IllegalStateException("Cover image not found.")));
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
            return imageContainerClient.flatMap(containerAsyncClient ->
                bookMono.flatMap(blobItem -> {
                    final BlockBlobAsyncClient blockBlob = containerAsyncClient.getBlockBlobAsyncClient(blobItem.name());
                    String property = "java.io.tmpdir";
                    String tempDir = System.getProperty(property);
                    String newFileName;
                    try {
                        newFileName = URLEncoder.encode(newBook.getTitle(), StandardCharsets.US_ASCII.toString());
                    } catch (UnsupportedEncodingException e) {
                        logger.error("Error encoding file: ", e);
                        return Mono.error(e);
                    }
                    String extension = blobItem.name().substring(blobItem.name().lastIndexOf("."));
                    File newFile = new File(tempDir + newFileName + extension);
                    try {
                        newFile.createNewFile();
                    } catch (IOException e) {
                        logger.error("Exception creating the file: ", e);
                        return Mono.error(e);
                    }
                    Book saveBook = new Book(newBook.getTitle(), newBook.getAuthor(), newFile.toURI());
                    return blockBlob.downloadToFile(newFile.getAbsolutePath()).then(deleteImage(oldBook)).then(saveImage(saveBook));
                }));
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
<<<<<<< HEAD
        Mono<BlobItem> file = locateImage(blobConversion);
=======
        Mono<BlobItem> file = imageContainerClient.flatMapMany(containerAsyncClient
            -> containerAsyncClient.listBlobsFlat().filter(blobItem ->
            blobItem.name().contains(blobConversion[2] + "/"
                + blobConversion[1]
                + "/" + blobConversion[0] + "."))).elementAt(0);
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
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
<<<<<<< HEAD
        String[] blobInfo = getBlobInformation(book.getAuthor(), book.getTitle());
        Mono<BlobItem> file = locateImage(blobInfo);
=======
        String[] blobConversion = getBlobInformation(book.getAuthor(), book.getTitle());
        Mono<BlobItem> file = imageContainerClient.flatMapMany(containerAsyncClient ->
            containerAsyncClient.listBlobsFlat().filter(blobItem -> {
                assert blobConversion != null;
                return blobItem.name().contains(blobConversion[2] + "/"
                    + blobConversion[1]
                    + "/" + blobConversion[0] + ".");
            })).elementAt(0);
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
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
                    logger.error("Exception creating the file: ", e);
                    return Mono.error(e);
                }
                return blockBlob.downloadToFile(newFile.getAbsolutePath()).then(Mono.fromCallable(() ->
                    newFile.getAbsolutePath() + "\n\tThis was downloaded and saved to the user's TEMP folder."));
            }));
    }
<<<<<<< HEAD

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
=======
>>>>>>> 610c5ed95752fce00be79839723fb68ac620ddf6
}
