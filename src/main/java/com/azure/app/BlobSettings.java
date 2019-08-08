// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.fasterxml.jackson.annotation.JsonProperty;

final class BlobSettings {
    @JsonProperty("blobUrl")
    private String blobUrl;

    @JsonProperty("blobKey")
    private String blobKey;

    @JsonProperty("blobAccountName")
    private String blobAccountName;

    String getUrl() {
        return blobUrl;
    }

    String getKey() {
        return blobKey;
    }

    String getAccountName() {
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
