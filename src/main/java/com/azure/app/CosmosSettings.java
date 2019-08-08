// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.fasterxml.jackson.annotation.JsonProperty;

class CosmosSettings {
    @JsonProperty("host")
    private String host;

    @JsonProperty("key")
    private String key;

    public String host() {
        return host;
    }

    public String key() {
        return key;
    }

    public CosmosSettings host(String host) {
        this.host = host;
        return this;
    }

    public CosmosSettings key(String key) {
        this.key = key;
        return this;
    }
}
