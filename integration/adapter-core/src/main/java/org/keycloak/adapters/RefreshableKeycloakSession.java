package org.keycloak.adapters;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.jboss.logging.Logger;
import org.keycloak.representations.IDToken;

import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RefreshableKeycloakSession extends KeycloakSecurityContext {

    protected static Logger log = Logger.getLogger(RefreshableKeycloakSession.class);

    protected transient RealmConfiguration realmConfiguration;
    protected String refreshToken;

    public RefreshableKeycloakSession() {
    }

    public RefreshableKeycloakSession(String tokenString, AccessToken token, String idTokenString, IDToken idToken, ResourceMetadata metadata, RealmConfiguration realmConfiguration, String refreshToken) {
        super(tokenString, token, idTokenString, idToken, metadata);
        this.realmConfiguration = realmConfiguration;
        this.refreshToken = refreshToken;
    }

    @Override
    public AccessToken getToken() {
        refreshExpiredToken();
        return super.getToken();
    }

    @Override
    public String getTokenString() {
        refreshExpiredToken();
        return super.getTokenString();
    }

    public boolean isActive() {
        return this.token.isActive() && this.token.getIssuedAt() > realmConfiguration.getNotBefore();
    }

    public void setRealmConfiguration(RealmConfiguration realmConfiguration) {
        this.realmConfiguration = realmConfiguration;
    }

    public void refreshExpiredToken() {
        log.info("checking whether to refresh.");
        if (isActive()) return;
        if (this.realmConfiguration == null || refreshToken == null) return; // Might be serialized in HttpSession?

        log.info("Doing refresh");
        AccessTokenResponse response = null;
        try {
            response = ServerRequest.invokeRefresh(realmConfiguration, refreshToken);
        } catch (IOException e) {
            log.error("Refresh token failure", e);
            return;
        } catch (ServerRequest.HttpFailure httpFailure) {
            log.error("Refresh token failure status: " + httpFailure.getStatus() + " " + httpFailure.getError());
            return;
        }
        log.info("received refresh response");
        String tokenString = response.getToken();
        AccessToken token = null;
        try {
            token = RSATokenVerifier.verifyToken(tokenString, realmConfiguration.getMetadata().getRealmKey(), realmConfiguration.getMetadata().getRealm());
            log.info("Token Verification succeeded!");
        } catch (VerificationException e) {
            log.error("failed verification of token");
        }
        if (response.getNotBeforePolicy() > realmConfiguration.getNotBefore()) {
            realmConfiguration.setNotBefore(response.getNotBeforePolicy());
        }

        this.token = token;
        this.refreshToken = response.getRefreshToken();
        this.tokenString = tokenString;

    }


}
