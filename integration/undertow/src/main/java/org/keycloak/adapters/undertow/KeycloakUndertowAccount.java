package org.keycloak.adapters.undertow;

import io.undertow.security.idm.Account;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.adapters.TokenGrantRequest;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class KeycloakUndertowAccount implements Account {
    protected static Logger log = Logger.getLogger(KeycloakUndertowAccount.class);
    protected AccessToken accessToken;
    protected String encodedAccessToken;
    protected String refreshToken;
    protected KeycloakPrincipal principal;
    protected Set<String> accountRoles;
    protected RealmConfiguration realmConfiguration;
    protected ResourceMetadata resourceMetadata;
    protected AdapterConfig adapterConfig;

    public KeycloakUndertowAccount(KeycloakPrincipal principal, AccessToken accessToken, String encodedAccessToken, String refreshToken,
                                   RealmConfiguration realmConfiguration, ResourceMetadata resourceMetadata, AdapterConfig adapterConfig) {
        this.principal = principal;
        this.accessToken = accessToken;
        this.encodedAccessToken = encodedAccessToken;
        this.refreshToken = refreshToken;
        this.realmConfiguration = realmConfiguration;
        this.resourceMetadata = resourceMetadata;
        this.adapterConfig = adapterConfig;
        setRoles(accessToken);
    }

    protected void setRoles(AccessToken accessToken) {
        Set<String> roles = null;
        if (adapterConfig.isUseResourceRoleMappings()) {
            AccessToken.Access access = accessToken.getResourceAccess(resourceMetadata.getResourceName());
            if (access != null) roles = access.getRoles();
        } else {
            AccessToken.Access access = accessToken.getRealmAccess();
            if (access != null) roles = access.getRoles();
        }
        if (roles == null) roles = Collections.emptySet();
        this.accountRoles = roles;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Set<String> getRoles() {
        return accountRoles;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public String getEncodedAccessToken() {
        return encodedAccessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public ResourceMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    public void refreshExpiredToken() {
        if (accessToken.isActive()) return;

        log.debug("Doing refresh");
        AccessTokenResponse response = null;
        try {
            response = TokenGrantRequest.invokeRefresh(realmConfiguration, getRefreshToken());
        } catch (IOException e) {
            log.error("Refresh token failure", e);
            return;
        } catch (TokenGrantRequest.HttpFailure httpFailure) {
            log.error("Refresh token failure status: " + httpFailure.getStatus() + " " + httpFailure.getError());
            return;
        }
        String tokenString = response.getToken();
        AccessToken token = null;
        try {
            token = RSATokenVerifier.verifyToken(tokenString, realmConfiguration.getMetadata().getRealmKey(), realmConfiguration.getMetadata().getRealm());
            log.debug("Token Verification succeeded!");
        } catch (VerificationException e) {
            log.error("failed verification of token");
        }
        this.accessToken = token;
        this.refreshToken = response.getRefreshToken();
        this.encodedAccessToken = tokenString;
        setRoles(this.accessToken);

    }
}
