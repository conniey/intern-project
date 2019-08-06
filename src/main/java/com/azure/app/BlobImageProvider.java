package com.azure.app;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.data.appconfiguration.ConfigurationAsyncClient;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import com.azure.data.appconfiguration.models.SettingSelector;
import com.azure.storage.blob.BlockBlobAsyncClient;
import com.azure.storage.blob.ContainerAsyncClient;
import com.azure.storage.blob.StorageAsyncClient;
import com.azure.storage.blob.StorageClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.common.credentials.SharedKeyCredential;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class BlobImageProvider implements BookCollector.ImageProvider {
    private final Set<String> supportedImageFormats;
    private Mono<StorageAsyncClient> storageClient;
    private Mono<ContainerAsyncClient> imageContainerClient;
    private static Logger logger = LoggerFactory.getLogger(BlobBookCollector.class);

    BlobImageProvider(ConfigurationAsyncClient client) {
        supportedImageFormats = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("gif", "png", "jpg")));
        SettingSelector keys = new SettingSelector().keys("BLOB*");
        storageClient = client.listSettings(keys).collectList().map(list -> {
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
            assert accountName != null;
            assert accountKey != null;
            SharedKeyCredential credential = new SharedKeyCredential(accountName, accountKey);
            assert url != null;
            String endPoint = String.format(Locale.ROOT, url);
            return StorageClient.storageClientBuilder()
                .endpoint(endPoint)
                .credential(credential)
                .httpLogDetailLevel(HttpLogDetailLevel.HEADERS)
                .buildAsyncClient();
        });
        imageContainerClient = storageClient.flatMap(container -> {
            final ContainerAsyncClient temp = container.getContainerAsyncClient("book-covers");
            return temp.exists().flatMap(exists -> {
                if (exists.value()) {
                    return Mono.just(temp);
                } else {
                    return temp.create().then(Mono.just(temp));
                }
            });
        });
    }

    /**
     * Saves the book's cover image to a Blob Storage
     *
     * @param imagePath - the File with the cover image
     * @param author    - Author of the Book
     * @param title     - String with the title of the book
     * @return {@Mono Void}
     */
    public Mono<Void> saveImage(File imagePath, Author author, String title) {
        String extension = FilenameUtils.getExtension(imagePath.getName());
        if (!supportedImageFormats.contains(extension)) {
            return Mono.error(new IllegalStateException("Error. Wrong file format for image"));
        }
        String[] blobInfo = getBlobInformation(author, title + "." + extension);
        if (blobInfo == null) {
            return Mono.error(new IllegalArgumentException("Error encoding blob name"));
        }
        return imageContainerClient.flatMap(containerAsyncClient -> {
            final BlockBlobAsyncClient blockBlobClient = containerAsyncClient.getBlockBlobAsyncClient(blobInfo[2] + "/" + blobInfo[1]
                + "/" + blobInfo[0]);
            return blockBlobClient.uploadFromFile(imagePath.getAbsolutePath()).then();
        });
    }

    /**
     * Retrieve valid names for the Blobs made
     *
     * @param author   - Author of the book will make up the folders the Blob will be contained in
     * @param fileName - the title of the main blob
     * @return String array with all the valid names
     */
    private String[] getBlobInformation(Author author, String fileName) {
        String blobLastName,
            blobFirstName,
            blobName;
        try {
            blobLastName = URLEncoder.encode(author.getLastName().toLowerCase(), StandardCharsets.US_ASCII.toString());
            blobName = URLEncoder.encode(fileName, StandardCharsets.US_ASCII.toString());
            blobFirstName = URLEncoder.encode(author.getFirstName().toLowerCase(), StandardCharsets.US_ASCII.toString());
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
        Mono<BlobItem> file = imageContainerClient.flatMapMany(containerAsyncClient
            -> containerAsyncClient.listBlobsFlat().filter(blobItem ->
            blobItem.name().contains(blobConversion[2] + "/"
                + blobConversion[1]
                + "/" + blobConversion[0] + "."))).elementAt(0);
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
        String[] blobConversion = getBlobInformation(book.getAuthor(), book.getTitle());
        Mono<BlobItem> file = imageContainerClient.flatMapMany(containerAsyncClient ->
            containerAsyncClient.listBlobsFlat().filter(blobItem -> {
                assert blobConversion != null;
                return blobItem.name().contains(blobConversion[2] + "/"
                    + blobConversion[1]
                    + "/" + blobConversion[0] + ".");
            })).elementAt(0);
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

    @Override
    public Mono<Void> saveImage(Book b) {
        String extension = FilenameUtils.getExtension(new File(b.getCover()).getName());
        String[] blobInfo = getBlobInformation(b.getAuthor(), b.getTitle() + "." + extension);
        if (blobInfo == null) {
            return Mono.error(new IllegalArgumentException("Error encoding blob name"));
        }
        return imageContainerClient.flatMap(containerAsyncClient -> {
            final BlockBlobAsyncClient blockBlobClient = containerAsyncClient.getBlockBlobAsyncClient(blobInfo[2] + "/" + blobInfo[1]
                + "/" + blobInfo[0]);
            File path = new File(b.getCover());
            return blockBlobClient.uploadFromFile(path.getAbsolutePath()).then();
        });
    }

    @Override
    public Mono<Void> editImage(Book oldBook, Book newBook, int saveCover) {
        return null;
    }
}