package org.keycloak.email;

import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;

import org.keycloak.models.KeycloakSession;

public class DefaultEmailAuthenticator implements EmailAuthenticator {

    @Override
    public void connect(KeycloakSession session, Map<String, String> config, Transport transport) throws EmailException {
        try {
            transport.connect();
        } catch (MessagingException e) {
            throw new EmailException("Non authenticated connect failed", e);
        }
    }
}
