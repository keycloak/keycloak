package org.keycloak.quarkus.deployment;

import java.util.Map;

import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorMetadata;

import io.quarkus.builder.item.SimpleBuildItem;

final class WebAuthnMetadataBuildItem extends SimpleBuildItem {

    private final Map<String, WebAuthnAuthenticatorMetadata> metadata;

    WebAuthnMetadataBuildItem(Map<String, WebAuthnAuthenticatorMetadata> metadata) {
        this.metadata = metadata;
    }

    Map<String, WebAuthnAuthenticatorMetadata> getMetadata() {
        return metadata;
    }
}
