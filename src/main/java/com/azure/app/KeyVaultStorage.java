// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.identity.credential.DeviceCodeCredential;
import com.azure.identity.credential.DeviceCodeCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.Secret;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import static com.azure.app.Constants.APP_CONFIGURATION_CREDENTIALS;
import static com.azure.app.Constants.AZURE_SIGN_IN_CLIENT_ID;
import static com.azure.app.Constants.BLOB_CREDENTIALS;
import static com.azure.app.Constants.COSMOS_CREDENTIALS;
import static com.azure.app.Constants.KEY_VAULT_URL;

final class KeyVaultStorage {
    private SecretAsyncClient secretAsyncClient;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyVaultStorage.class);

    KeyVaultStorage() {
        Properties keyVaultCredentials = new Properties();
        try {
            keyVaultCredentials.load(Objects.requireNonNull(KeyVaultStorage.class
                .getClassLoader()
                .getResourceAsStream("credential.properties")));
        } catch (IOException e) {
            System.out.println("Unable to read credentials.");
            LOGGER.error("Couldn't read Credential properties: ", e);
            return;
        }
        DeviceCodeCredential credential = new DeviceCodeCredentialBuilder()
            .deviceCodeChallengeConsumer(challenge -> System.out.println(challenge.message()))
            .clientId(keyVaultCredentials.getProperty(AZURE_SIGN_IN_CLIENT_ID))
            .build();
        secretAsyncClient = new SecretClientBuilder()
            .endpoint(keyVaultCredentials.getProperty(KEY_VAULT_URL))
            .credential(credential)
            .buildAsyncClient();
    }

    /**
     * Acquires the information needed to set up the Blob Storage from a Key Vault secret.
     *
     * @return - A Mono with the blob settings
     */
    Mono<BlobSettings> getBlobInformation() {
        Mono<Secret> secret = secretAsyncClient.getSecret(BLOB_CREDENTIALS);
        return secret.flatMap(secretValue -> {
            try {
                return Mono.just(MAPPER.readValue(secretValue.value(), BlobSettings.class));
            } catch (IOException e) {
                LOGGER.error("Error setting up Blob Settings: ", e);
                return Mono.error(new IllegalStateException("Couldn't set up Blob storage settings."));
            }
        });
    }

    /**
     * Acquires the information needed to set up the cosmos storage from a Key Vault secret.
     *
     * @return - Mono with the cosmos settings
     */
    Mono<CosmosSettings> getCosmosInformation() {
        Mono<Secret> secret = secretAsyncClient.getSecret(COSMOS_CREDENTIALS);
        return secret.flatMap(secretValue -> {
            try {
                return Mono.just(MAPPER.readValue(secretValue.value(), CosmosSettings.class));
            } catch (IOException e) {
                LOGGER.error("Error setting up Cosmos Settings: ", e);
                return Mono.error(new IllegalStateException("Couldn't set up Cosmos storage settings"));
            }
        });
    }

    Mono<String> getConnectionString() {
        Mono<Secret> secret = secretAsyncClient.getSecret(APP_CONFIGURATION_CREDENTIALS);
        return secret.map(Secret::value).onErrorResume(error -> Mono.just("Couldn't set up App Configuration"));
    }
}
