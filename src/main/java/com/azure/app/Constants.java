// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import java.nio.file.Paths;

public class Constants {
    static final JsonHandler SERIALIZER = new JsonHandler();
    static final String JSON_PATH = Paths.get(System.getProperty("user.dir"), "lib", "jsonFiles").toString();
    static final String IMAGE_PATH = Paths.get(System.getProperty("user.dir"), "lib", "images").toString();
}
