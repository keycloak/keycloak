package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;

import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RefreshableKeycloakSecurityContext extends KeycloakSecurityContext {

    protected static Logger log = Logger.getLogger(RefreshableKeycloakSecurityContext.class);

    protected transient KeycloakDeployment deployment;
    protected String refreshToken;

    public RefreshableKeycloakSecurityContext() {
    }

    public RefreshableKeycloakSecurityContext(KeycloakDeployment deployment, String tokenString, AccessToken token, String idTokenString, IDToken idToken, String refreshToken) {
        super(tokenString, token, idTokenString, idToken);
        this.deployment = deployment;
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

    public void logout(KeycloakDeployment deployment) {
        try {
            ServerRequest.invokeLogout(deployment, refreshToken);
        } catch (Exception e) {
            log.error("failed to invoke remote logout", e);
        }
    }

    public boolean isActive() {
        return this.token.isActive() && this.token.getIssuedAt() > deployment.getNotBefore();
    }

    public KeycloakDeployment getDeployment() {
        return deployment;
    }

    public void setDeployment(KeycloakDeployment deployment) {
        this.deployment = deployment;
    }

    public void refreshExpiredToken() {
        if (log.isTraceEnabled()) {
            log.trace("checking whether to refresh.");
        }
        if (isActive()) return;
        if (this.deployment == null || refreshToken == null) return; // Might be serialized in HttpSession?

        if (log.isTraceEnabled()) {
            log.trace("Doing refresh");
        }
        AccessTokenResponse response = null;
        try {
            response = ServerRequest.invokeRefresh(deployment, refreshToken);
        } catch (IOException e) {
            log.error("Refresh token failure", e);
            return;
        } catch (ServerRequest.HttpFailure httpFailure) {
            log.error("Refresh token failure status: " + httpFailure.getStatus() + " " + httpFailure.getError());
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("received refresh response");
        }
        String tokenString = response.getToken();
        AccessToken token = null;
        try {
            token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealm());
            log.debug("Token Verification succeeded!");
        } catch (VerificationException e) {
            log.error("failed verification of token");
        }
        if (response.getNotBeforePolicy() > deployment.getNotBefore()) {
            deployment.setNotBefore(response.getNotBeforePolicy());
        }

        this.token = token;
        this.refreshToken = response.getRefreshToken();
        this.tokenString = tokenString;

    }


}
