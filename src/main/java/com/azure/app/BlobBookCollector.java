// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.credentials.ConfigurationClientCredentials;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import com.azure.data.appconfiguration.models.SettingSelector;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class BlobBookCollector implements BookCollection {
    private final Set<String> supportedImageFormats;
    private Mono<StorageAsyncClient> storageClient;
    private Mono<ContainerAsyncClient> imageContainerClient;
    private Mono<ContainerAsyncClient> bookContainerClient;
    private Flux<Book> blobBooks;
    private BlockBlobAsyncClient blockBlobClient;
    private static Logger logger = LoggerFactory.getLogger(BlobBookCollector.class);

    BlobBookCollector() {
        supportedImageFormats = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("gif", "png", "jpg")));
        String connectionString = System.getenv("AZURE_APPCONFIG");
        if (connectionString == null || connectionString.isEmpty()) {
            System.err.println("Environment variable AZURE_APPCONFIG is not set. Cannot connect to App Configuration."
                + " Please set it.");
            return;
        }
        ConfigurationAsyncClient client;
        try {
            client = ConfigurationAsyncClient.builder()
                .credentials(new ConfigurationClientCredentials(connectionString))
                .build();
            SettingSelector keys = new SettingSelector().keys("BLOB*");
            storageClient = client.listSettings(keys).collectList().map(list ->
            {
                String accountName = null;
                String accountKey = null;
                String url = null;
                for (ConfigurationSetting configurationSetting : list) {
                    String key = configurationSetting.key();
                    if (key.contentEquals("BLOB_ACCOUNT_NAME")) {
                        accountName = configurationSetting.value();
                    } else if (key.contentEquals("BLOB_KEY")) {
                        accountKey = configurationSetting.value();
                    } else {
                        url = configurationSetting.value();
                    }
                }
                SharedKeyCredential credential = new SharedKeyCredential(accountName, accountKey);
                String endPoint = String.format(Locale.ROOT, url);
                return StorageClient.storageClientBuilder()
                    .endpoint(endPoint)
                    .credential(credential)
                    .buildAsyncClient();
            });
            bookContainerClient = storageClient.map(container ->
                container.getContainerAsyncClient("book-library"));
            imageContainerClient = storageClient.map(container ->
                container.getContainerAsyncClient("book-covers"));
            bookContainerClient.subscribe(containerAsyncClient -> containerAsyncClient.exists().map(created -> {
                if (!created.value()) {
                    containerAsyncClient.create();
                }
                return created;
            }));
            imageContainerClient.subscribe(containerAsyncClient -> containerAsyncClient.exists().map(created -> {
                if (!created.value()) {
                    containerAsyncClient.create();
                }
                return created;
            }));
            blobBooks = initializeBooks().cache();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            logger.error("Exception with App Configuration: ", e);
        }
    }

    /**
     * Returns the Flux of Book objects
     *
     * @return Flux<Book> the flux with all the book information. {@literal Flux<Book>}
     */
    @Override
    public Flux<Book> getBooks() {
        return blobBooks;
    }

    private Flux<Book> initializeBooks() {
        return bookContainerClient.flatMapMany(container -> container.listBlobsFlat().flatMap(blob -> {
            blockBlobClient = container.getBlockBlobAsyncClient(blob.name());
            File temporaryBookHolder = new File(blob.name().substring(blob.name().lastIndexOf("/") + 1));
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
        }));
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
        saveImage(new File(path), author);
        String blobLastName;
        String blobFirstName;
        String blobName;
        try {
            blobLastName = URLEncoder.encode(author.getLastName().toLowerCase(), StandardCharsets.US_ASCII.toString());
            blobName = URLEncoder.encode(bookFile.getName(), StandardCharsets.US_ASCII.toString());
            blobFirstName = URLEncoder.encode(author.getFirstName().toLowerCase(), StandardCharsets.US_ASCII.toString());
        } catch (UnsupportedEncodingException e) {
            return Mono.error(e);
        }
      /*  blockBlobClient = bookContainerClient.getBlockBlobAsyncClient(blobLastName + "/" + blobFirstName
            + "/" + blobName);
        return blockBlobClient.uploadFromFile(bookFile.getAbsolutePath()).then(Mono.fromCallable(() -> {
            bookFile.delete();
            return true;
        }));*/
        return null;
    }

    private boolean saveImage(File imagePath, Author author) {
        String extension = FilenameUtils.getExtension(imagePath.getName());
        if (!supportedImageFormats.contains(extension)) {
            return false;
        }
        try {
            BufferedImage bufferedImage = ImageIO.read(imagePath);
            File image = new File(imagePath.getName());
            File savedImage = image;
            String blobLastName;
            String blobFirstName;
            String blobName;
            try {
                blobLastName = URLEncoder.encode(author.getLastName().toLowerCase(), StandardCharsets.US_ASCII.toString());
                blobName = URLEncoder.encode(savedImage.getName(), StandardCharsets.US_ASCII.toString());
                blobFirstName = URLEncoder.encode(author.getFirstName().toLowerCase(), StandardCharsets.US_ASCII.toString());
            } catch (UnsupportedEncodingException e) {
                logger.error("Error encoding names: ", e);
                return false;
            }
            /*if (ImageIO.write(bufferedImage, extension, image)) {
                blockBlobClient = imageContainerClient.getBlockBlobAsyncClient(blobLastName + "/"
                    + blobFirstName + "/" + blobName);
                blockBlobClient.uploadFromFile(savedImage.getPath()).subscribe(Stream.builder()::accept);
                return true;
            }*/
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
     * @return Mono<Boolean> determines whether or not book was successfully deleted
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
