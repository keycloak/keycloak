package org.keycloak.adapters.undertow;

import io.undertow.security.idm.Account;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

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
    protected RefreshableKeycloakSecurityContext session;
    protected KeycloakPrincipal principal;
    protected Set<String> accountRoles;

    public KeycloakUndertowAccount(KeycloakPrincipal principal, RefreshableKeycloakSecurityContext session, KeycloakDeployment deployment) {
        this.principal = principal;
        this.session = session;
        setRoles(session.getToken(), deployment);
    }

    protected void setRoles(AccessToken accessToken, KeycloakDeployment deployment) {
        Set<String> roles = null;
        if (deployment.isUseResourceRoleMappings()) {
            AccessToken.Access access = accessToken.getResourceAccess(deployment.getResourceName());
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

    public RefreshableKeycloakSecurityContext getKeycloakSecurityContext() {
        return session;
    }

    public boolean isActive(KeycloakDeployment deployment) {
        // this object may have been serialized, so we need to reset realm config/metadata
        session.setDeployment(deployment);
        log.info("realmConfig notBefore: " + deployment.getNotBefore());
        if (session.isActive()) {
            log.info("session is active");
            return true;
        }

        log.info("session is not active try refresh");
        session.refreshExpiredToken();
        if (!session.isActive()) {
            log.info("session is not active return with failure");

            return false;
        }
        log.info("refresh succeeded");

        setRoles(session.getToken(), deployment);
        return true;
    }



}
