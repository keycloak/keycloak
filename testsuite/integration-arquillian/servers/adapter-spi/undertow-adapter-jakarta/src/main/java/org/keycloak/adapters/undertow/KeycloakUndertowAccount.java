/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.adapters.undertow;

import io.undertow.security.idm.Account;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;

import java.io.Serializable;
import java.security.Principal;
import java.util.Set;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class KeycloakUndertowAccount implements Account, Serializable, OidcKeycloakAccount {
    protected static Logger log = Logger.getLogger(KeycloakUndertowAccount.class);
    protected KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal;
    protected Set<String> accountRoles;

    public KeycloakUndertowAccount(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        this.principal = principal;
        setRoles(principal.getKeycloakSecurityContext());
    }

    protected void setRoles(RefreshableKeycloakSecurityContext session) {
        Set<String> roles = AdapterUtils.getRolesFromSecurityContext(session);
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

    @Override
    public RefreshableKeycloakSecurityContext getKeycloakSecurityContext() {
        return principal.getKeycloakSecurityContext();
    }

    public void setCurrentRequestInfo(KeycloakDeployment deployment, AdapterTokenStore tokenStore) {
        principal.getKeycloakSecurityContext().setCurrentRequestInfo(deployment, tokenStore);
    }

    // Check if accessToken is active and try to refresh if it's not
    public boolean checkActive() {
        // this object may have been serialized, so we need to reset realm config/metadata
        RefreshableKeycloakSecurityContext session = getKeycloakSecurityContext();
        if (session.isActive() && !session.getDeployment().isAlwaysRefreshToken()) {
            log.debug("session is active");
            return true;
        }

        log.debug("session is not active or refresh is enforced. Try refresh");
        boolean success = session.refreshExpiredToken(false);
        if (!success || !session.isActive()) {
            log.debug("session is not active return with failure");

            return false;
        }
        log.debug("refresh succeeded");

        setRoles(session);
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof KeycloakUndertowAccount))
            return false;

        KeycloakUndertowAccount otherAccount = (KeycloakUndertowAccount) other;

        return (this.principal != null ? this.principal.equals(otherAccount.principal) : otherAccount.principal == null) &&
                (this.accountRoles != null ? this.accountRoles.equals(otherAccount.accountRoles) : otherAccount.accountRoles == null);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.principal == null ? 0 : this.principal.hashCode());
        result = prime * result + (this.accountRoles == null ? 0 : this.accountRoles.hashCode());
        return result;
    }
}
