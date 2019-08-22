// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import java.nio.file.Paths;

final class Constants {
    static final JsonHandler SERIALIZER = new JsonHandler();
    static final String JSON_PATH = Paths.get("lib", "jsonFiles").toString();
    static final String IMAGE_PATH = Paths.get("lib", "images").toString();
    static final String BLOB_CREDENTIALS = "BLOB-INFO";
    static final String COSMOS_CREDENTIALS = "COSMOS-INFO";
    static final String APP_CONFIGURATION_CREDENTIALS = "AZURE-APPCONFIG";
    static final String KEY_VAULT_URL = "key_vault.url";
    static final String AZURE_SIGN_IN_CLIENT_ID = "client.ID";
}
