package org.keycloak.email;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.KeycloakSessionUtil;
import org.keycloak.vault.VaultStringSecret;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

public class TokenAuthEmailAuthenticator implements EmailAuthenticator {

    private static final Logger logger = Logger.getLogger(TokenAuthEmailAuthenticator.class);
    public static final int FALLBACK_EXPIRES_AT_IN_SECONDS = 60;

    private final Map<String, TokenAuthEmailAuthenticator.TokenStoreEntry> tokenStore = new ConcurrentHashMap<>();

    @Override
    public void connect(KeycloakSession session, Map<String, String> config, Transport transport) throws EmailException {
        try {
            String token = gatherValidToken(session, config);

            transport.connect(config.get("user"), token);

        } catch (AuthenticationFailedException e) {

            this.tokenStore.remove(session.getContext().getRealm().getId());
            logger.debugf("AuthenticationFailed-Exception for SMTP in realm %s failed response was %s, will try again", KeycloakSessionUtil.getRealmNameFromContext(session), e.getMessage());

            String token = gatherValidToken(session, config);

            try {
                transport.connect(config.get("user"), token);
            } catch (MessagingException ex) {
                logger.warnf("Retry after AuthenticationFailed-Exception for SMTP in realm %s failed response was %s", KeycloakSessionUtil.getRealmNameFromContext(session), ex);
                throw new EmailException("Retry after AuthenticationFailed-Exception for SMTP failed.", ex);
            }

        } catch (MessagingException e) {
            throw new EmailException("Connect failed for SMTP " + KeycloakSessionUtil.getRealmNameFromContext(session), e);
        }
    }

    private String gatherValidToken(KeycloakSession session, Map<String, String> config) throws EmailException {
        try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(config.get("authTokenClientSecret"))) {
            String authTokenClientSecret = vaultStringSecret.get().orElse(config.get("authTokenClientSecret"));
            String authTokenUrl = config.get("authTokenUrl");
            String authTokenClientId = config.get("authTokenClientId");
            String authTokenScope = config.get("authTokenScope");
            int authTokenClientSecretHash = authTokenClientSecret.hashCode();

            TokenStoreEntry tokenStoreEntry = this.tokenStore.get(session.getContext().getRealm().getId());
            if (isValidAuthToken(authTokenUrl, authTokenScope, authTokenClientId, authTokenClientSecretHash, tokenStoreEntry)) {
                return tokenStoreEntry.token;
            }

            synchronized (this.tokenStore) {
                if (isValidAuthToken(authTokenUrl, authTokenScope, authTokenClientId, authTokenClientSecretHash, tokenStoreEntry)) {
                    return tokenStoreEntry.token;
                }

                JsonNode response = fetchTokenViaHTTP(session, authTokenUrl, authTokenScope, authTokenClientId, authTokenClientSecret);

                Optional<String> maybeToken = getAccessToken(session, response);
                Optional<LocalDateTime> maybeExpiresAt = getExpiresIn(session, response);

                if (maybeToken.isPresent()) {
                    String token = maybeToken.get();
                    this.tokenStore.put(session.getContext().getRealm().getId(),
                            new TokenStoreEntry(
                                    maybeExpiresAt.orElse(LocalDateTime.now().plusSeconds(FALLBACK_EXPIRES_AT_IN_SECONDS)),
                                    authTokenUrl,
                                    authTokenScope,
                                    authTokenClientId,
                                    authTokenClientSecretHash,
                                    token));
                    return token;
                } else {
                    throw new EmailException("No access token found in token-response for SMTP");
                }
            }
        } catch (IOException e) {
            throw new EmailException("Failed to gather valid token for SMTP", e);
        }
    }

    private static boolean isValidAuthToken(String authTokenUrl, String authTokenScope, String authTokenClientId, int authTokenHash, TokenStoreEntry tokenStoreEntry) {
        return tokenStoreEntry != null
                && authTokenUrl != null && authTokenUrl.equals(tokenStoreEntry.url)
                && authTokenScope != null && authTokenScope.equals(tokenStoreEntry.scope)
                && authTokenClientId != null && authTokenClientId.equals(tokenStoreEntry.clientId)
                && authTokenHash == tokenStoreEntry.clientSecretHash
                && tokenStoreEntry.expiration_at.plusSeconds(30).isAfter(LocalDateTime.now());
    }

    private Optional<String> getAccessToken(KeycloakSession session, JsonNode response) {
        if (response.has("access_token")) {
            return Optional.of(response.get("access_token").asText());
        } else {
            logger.warnf("Got no access_token from response for SMTP auth in realm %s, response was %s", KeycloakSessionUtil.getRealmNameFromContext(session), response);
            return Optional.empty();
        }
    }

    private Optional<LocalDateTime> getExpiresIn(KeycloakSession session, JsonNode response) {
        //token-lifetime, must be given beside the token because token can be opaque (must not be a jwt token)
        if (response.has("expires_in")) {
            String expiresIn = response.get("expires_in").asText();
            return Optional.of(LocalDateTime.now().plusSeconds(Long.parseLong(expiresIn)));
        } else {
            logger.warnf("Got no expires_in from response for SMTP auth in realm %s, response was %s", KeycloakSessionUtil.getRealmNameFromContext(session), response.asText());
            return Optional.of((LocalDateTime.now().plusSeconds(FALLBACK_EXPIRES_AT_IN_SECONDS)));
        }
    }

    private JsonNode fetchTokenViaHTTP(KeycloakSession session, String authTokenUrl, String authTokenScope, String authTokenClientId, String authTokenClientSecret) throws IOException {
        return SimpleHttp.create(session).doPost(authTokenUrl)
                .param("client_id", authTokenClientId)
                .param("client_secret", authTokenClientSecret)
                .param("scope", authTokenScope)
                .param("grant_type", "client_credentials").asJson();
    }

    record TokenStoreEntry(
            LocalDateTime expiration_at,
            String url,
            String scope,
            String clientId,
            int clientSecretHash,
            String token) {
    }
}
