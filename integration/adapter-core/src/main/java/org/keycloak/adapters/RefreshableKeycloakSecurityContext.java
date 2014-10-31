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
    protected transient AdapterTokenStore tokenStore;
    protected String refreshToken;

    public RefreshableKeycloakSecurityContext() {
    }

    public RefreshableKeycloakSecurityContext(KeycloakDeployment deployment, AdapterTokenStore tokenStore, String tokenString, AccessToken token, String idTokenString, IDToken idToken, String refreshToken) {
        super(tokenString, token, idTokenString, idToken);
        this.deployment = deployment;
        this.tokenStore = tokenStore;
        this.refreshToken = refreshToken;
    }

    @Override
    public AccessToken getToken() {
        refreshExpiredToken(true);
        return super.getToken();
    }

    @Override
    public String getTokenString() {
        refreshExpiredToken(true);
        return super.getTokenString();
    }

    public String getRefreshToken() {
        return refreshToken;
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

    public void setCurrentRequestInfo(KeycloakDeployment deployment, AdapterTokenStore tokenStore) {
        this.deployment = deployment;
        this.tokenStore = tokenStore;
    }

    /**
     * @param checkActive if true, then we won't send refresh request if current accessToken is still active.
     * @return true if accessToken is active or was successfully refreshed
     */
    public boolean refreshExpiredToken(boolean checkActive) {
        if (checkActive) {
            if (log.isTraceEnabled()) {
                log.trace("checking whether to refresh.");
            }
            if (isActive()) return true;
        }

        if (this.deployment == null || refreshToken == null) return false; // Might be serialized in HttpSession?

        if (!this.getRealm().equals(this.deployment.getRealm())) {
            // this should not happen, but let's check it anyway
            return false;
        }

        if (log.isTraceEnabled()) {
            log.trace("Doing refresh");
        }
        AccessTokenResponse response = null;
        try {
            response = ServerRequest.invokeRefresh(deployment, refreshToken);
        } catch (IOException e) {
            log.error("Refresh token failure", e);
            return false;
        } catch (ServerRequest.HttpFailure httpFailure) {
            log.error("Refresh token failure status: " + httpFailure.getStatus() + " " + httpFailure.getError());
            return false;
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
        tokenStore.refreshCallback(this);
        return true;
    }
}
