// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.app;

import com.azure.identity.DeviceCodeChallenge;
import com.azure.identity.credential.DeviceCodeCredential;
import com.azure.identity.credential.DeviceCodeCredentialBuilder;
import com.azure.identity.implementation.IdentityClientOptions;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.Secret;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;

final class KeyVaultStorage {

    private SecretAsyncClient secretAsyncClient;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyVaultStorage.class);

    KeyVaultStorage(String[] scopes) {
        DeviceCodeCredential deviceCodeCredential = new DeviceCodeCredentialBuilder()
            .deviceCodeChallengeConsumer(challenge -> System.out.println(challenge.message()))
            .clientId("b8053ea3-2cca-43e0-baba-357391862bbe")
            .authorityHost("https://login.microsoftonline.com/organizations/")
            .build();
        secretAsyncClient = new SecretClientBuilder()
            .endpoint(System.getenv("KEY_VALUE"))
            .credential(deviceCodeCredential)
            .buildAsyncClient();
    }

    KeyVaultStorage(int i) {
        final Properties oAuthProperties = new Properties();
        try {
            oAuthProperties.load(App.class.getClassLoader().getResourceAsStream("oAuth.properties"));
        } catch (IOException e) {
            System.out.println("Unable to read oAuth configuration. Make sure you have a properly formatted oAuth.properties file.");
            return;
        }
        final String appId = oAuthProperties.getProperty("app.id");
        // Get an access token
        Authentication.initialize(appId);
        Consumer<DeviceCodeChallenge> deviceCodeConsumer = (DeviceCodeChallenge deviceCode) -> {
            // Print the login information to the console
            System.out.println(deviceCode.message());
        };
        VaultCredential credential = new VaultCredential("5955292d-1e44-4b42-9c6a-f8b37f4ffe89", deviceCodeConsumer, new IdentityClientOptions());
        secretAsyncClient = new SecretClientBuilder()
            .endpoint(System.getenv("KEY_VALUE"))
            .credential(credential)
            .buildAsyncClient();
    }

    KeyVaultStorage() {

        DeviceCodeCredential credential = new DeviceCodeCredentialBuilder()
            .clientId("945e7458-8357-4947-a670-f4485dd5e337")
            .authorityHost("https://vault.azure.net/user_impersonation")
            .deviceCodeChallengeConsumer(challenge -> System.out.println(challenge.message()))
            .build();
        //     DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
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
