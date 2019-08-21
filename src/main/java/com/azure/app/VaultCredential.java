package com.azure.app;

import com.azure.core.credentials.AccessToken;
import com.azure.core.credentials.TokenCredential;
import com.azure.identity.DeviceCodeChallenge;
import com.azure.identity.implementation.IdentityClient;
import com.azure.identity.implementation.IdentityClientBuilder;
import com.azure.identity.implementation.IdentityClientOptions;
import com.azure.identity.implementation.MsalToken;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class VaultCredential implements TokenCredential {
    private final Consumer<DeviceCodeChallenge> deviceCodeChallengeConsumer;
    private final IdentityClient identityClient;
    private final AtomicReference<MsalToken> cachedToken;

    VaultCredential(String clientId, Consumer<DeviceCodeChallenge> deviceCodeChallengeConsumer, IdentityClientOptions identityClientOptions) {
        this.deviceCodeChallengeConsumer = deviceCodeChallengeConsumer;
        this.identityClient = (new IdentityClientBuilder()).tenantId("organizations").clientId(clientId).identityClientOptions(identityClientOptions).build();
        this.cachedToken = new AtomicReference<>();
    }

    public Mono<AccessToken> getToken(String... scopes) {
        return Mono.defer(() -> {
            return this.cachedToken.get() != null ? this.identityClient.authenticateWithUserRefreshToken(scopes, (MsalToken) this.cachedToken.get()).onErrorResume((t) -> Mono.empty()) : Mono.empty();
        }).switchIfEmpty(Mono.defer(() -> this.identityClient.authenticateWithDeviceCode(scopes, this.deviceCodeChallengeConsumer))).map((msalToken) -> {
            this.cachedToken.set(msalToken);
            return msalToken;
        });
    }
}
