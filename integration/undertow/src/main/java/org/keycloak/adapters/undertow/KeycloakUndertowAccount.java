package org.keycloak.adapters.undertow;

import io.undertow.security.idm.Account;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSession;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class KeycloakUndertowAccount implements Account, Serializable {
    protected static Logger log = Logger.getLogger(KeycloakUndertowAccount.class);
    protected RefreshableKeycloakSession session;
    protected KeycloakPrincipal principal;
    protected Set<String> accountRoles;

    public KeycloakUndertowAccount(KeycloakPrincipal principal, RefreshableKeycloakSession session, AdapterConfig config, ResourceMetadata metadata) {
        this.principal = principal;
        this.session = session;
        setRoles(session.getToken(), config, metadata);
    }

    protected void setRoles(AccessToken accessToken, AdapterConfig adapterConfig, ResourceMetadata resourceMetadata) {
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
        return session.getToken();
    }

    public String getEncodedAccessToken() {
        return session.getTokenString();
    }

    public RefreshableKeycloakSession getSession() {
        return session;
    }

    public boolean isActive(RealmConfiguration realmConfiguration, AdapterConfig config) {
        // this object may have been serialized, so we need to reset realm config/metadata
        session.setRealmConfiguration(realmConfiguration);
        session.setMetadata(realmConfiguration.getMetadata());
        log.info("realmConfig notBefore: " + realmConfiguration.getNotBefore());
        if (session.isActive()) return true;

        session.refreshExpiredToken();
        if (!session.isActive()) return false;

        setRoles(session.getToken(), config, realmConfiguration.getMetadata());
        return true;
    }



}
