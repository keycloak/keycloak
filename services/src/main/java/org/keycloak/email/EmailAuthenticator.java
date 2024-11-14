package org.keycloak.email;

import jakarta.mail.Transport;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

public interface EmailAuthenticator {

    void connect(KeycloakSession session, Map<String, String> config, Transport transport) throws EmailException;

    enum AuthenticatorType {
        NONE,
        BASIC,
        TOKEN
    }
}


