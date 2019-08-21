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

final class KeyVaultStorage {
    private SecretAsyncClient secretAsyncClient;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyVaultStorage.class);

    KeyVaultStorage() {
        DeviceCodeCredential credential = new DeviceCodeCredentialBuilder()
            .deviceCodeChallengeConsumer(challenge -> System.out.println(challenge.message()))
            .clientId("04b07795-8ddb-461a-bbee-02f9e1bf7b46")
            .build();
        secretAsyncClient = new SecretClientBuilder()
            .endpoint(System.getenv("KEY_VALUE"))
            .credential(credential)
            .buildAsyncClient();
    }

    Mono<BlobSettings> getBlobInformation() {
        Mono<Secret> secret = secretAsyncClient.getSecret("BLOB-INFO");
        return secret.flatMap(secretValue -> {
            try {
                return Mono.just(MAPPER.readValue(secretValue.value(), BlobSettings.class));
            } catch (IOException e) {
                LOGGER.error("Error setting up Blob Settings: ", e);
                return Mono.error(new IllegalStateException("Couldn't set up Blob storage settings."));
            }
        });
    }

    Mono<CosmosSettings> getCosmosInformation() {
        Mono<Secret> secret = secretAsyncClient.getSecret("COSMOS-INFO");
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
        Mono<Secret> secret = secretAsyncClient.getSecret("AZURE-APPCONFIG");
        return secret.map(Secret::value).onErrorResume(error -> Mono.just("Couldn't set up App Configuration"));
    }
}
