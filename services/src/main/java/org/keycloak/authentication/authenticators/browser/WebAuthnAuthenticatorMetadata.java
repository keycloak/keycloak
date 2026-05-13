package org.keycloak.authentication.authenticators.browser;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata for a WebAuthn authenticator identified by its AAGUID.
 *
 * @param name human-readable authenticator provider name
 * @param iconLight icon for light theme (data URI)
 * @param iconDark icon for dark theme (data URI)
 */
public record WebAuthnAuthenticatorMetadata(
        String name,
        @JsonProperty("icon_light") String iconLight,
        @JsonProperty("icon_dark") String iconDark) {
}
