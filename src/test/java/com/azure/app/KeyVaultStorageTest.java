package com.azure.app;

import org.junit.Test;

public class KeyVaultStorageTest {

    @Test
    public void testInitialization() {
        KeyVaultStorage keyVaultStorage = new KeyVaultStorage();
        keyVaultStorage.getBlobInformation().block();
    }
}
