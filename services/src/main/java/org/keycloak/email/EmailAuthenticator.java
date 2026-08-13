package org.keycloak.email;

import java.util.Map;

import jakarta.mail.Transport;

import org.keycloak.models.KeycloakSession;

public interface EmailAuthenticator {

    void connect(KeycloakSession session, Map<String, String> config, Transport transport) throws EmailException;

    enum AuthenticatorType {
        NONE,
        BASIC,
        TOKEN
    }
}
