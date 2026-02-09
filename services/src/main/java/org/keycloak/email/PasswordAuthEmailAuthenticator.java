package org.keycloak.email;

import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;

import org.keycloak.models.KeycloakSession;
import org.keycloak.vault.VaultStringSecret;

public class PasswordAuthEmailAuthenticator implements EmailAuthenticator {

    @Override
    public void connect(KeycloakSession session, Map<String, String> config, Transport transport) throws EmailException {
        try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(config.get("password"))) {
            transport.connect(config.get("user"), vaultStringSecret.get().orElse(config.get("password")));
        } catch (MessagingException e) {
            throw new EmailException("Password based SMTP connect failed", e);
        }
    }

}
