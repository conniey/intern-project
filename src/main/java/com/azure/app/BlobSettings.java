package com.azure.app;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BlobSettings {
    @JsonProperty("blobUrl")
    private String blobUrl;

    @JsonProperty("blobKey")
    private String blobKey;

    @JsonProperty("blobAccountName")
    private String blobAccountName;

    public String getUrl() {
        return blobUrl;
    }

    public String getKey() {
        return blobKey;
    }

    public String getAccountName() {
        return blobAccountName;
    }

    public BlobSettings url(String url) {
        blobUrl = url;
        return this;
    }

    public BlobSettings key(String key) {
        blobKey = key;
        return this;
    }

    public BlobSettings accountName(String accountName) {
        blobAccountName = accountName;
        return this;
    }
}
