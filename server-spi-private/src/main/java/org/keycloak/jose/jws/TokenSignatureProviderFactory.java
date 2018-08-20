package org.keycloak.jose.jws;

import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

// KEYCLOAK-7560 Refactoring Token Signing and Verifying by Token Signature SPI

public interface TokenSignatureProviderFactory<T extends TokenSignatureProvider> extends ComponentFactory<T, TokenSignatureProvider> {
    T create(KeycloakSession session, ComponentModel model);
}
