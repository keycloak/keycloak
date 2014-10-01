/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.adapters.undertow;

import io.undertow.security.idm.Account;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.KeycloakAccount;
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
public class KeycloakUndertowAccount implements Account, Serializable, KeycloakAccount {
    protected static Logger log = Logger.getLogger(KeycloakUndertowAccount.class);
    protected KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal;
    protected Set<String> accountRoles;

    public KeycloakUndertowAccount(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        this.principal = principal;
        setRoles(principal.getKeycloakSecurityContext().getToken());
    }

    protected void setRoles(AccessToken accessToken) {
        Set<String> roles = null;
        RefreshableKeycloakSecurityContext session = getKeycloakSecurityContext();
        if (session.getDeployment().isUseResourceRoleMappings()) {
            if (log.isTraceEnabled()) {
                log.trace("useResourceRoleMappings");
            }
            AccessToken.Access access = accessToken.getResourceAccess(session.getDeployment().getResourceName());
            if (access != null) roles = access.getRoles();
        } else {
            if (log.isTraceEnabled()) {
                log.trace("use realm role mappings");
            }
            AccessToken.Access access = accessToken.getRealmAccess();
            if (access != null) roles = access.getRoles();
        }
        if (roles == null) roles = Collections.emptySet();
        if (log.isTraceEnabled()) {
            log.trace("Setting roles: ");
            for (String role : roles) {
                log.trace("   role: " + role);
            }
        }

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

    public void setDeployment(KeycloakDeployment deployment) {
        principal.getKeycloakSecurityContext().setDeployment(deployment);
    }

    public boolean isActive() {
        // this object may have been serialized, so we need to reset realm config/metadata
        RefreshableKeycloakSecurityContext session = getKeycloakSecurityContext();
        if (session.isActive()) {
            log.debug("session is active");
            return true;
        }

        log.debug("session is not active try refresh");
        session.refreshExpiredToken();
        if (!session.isActive()) {
            log.debug("session is not active return with failure");

            return false;
        }
        log.debug("refresh succeeded");

        setRoles(session.getToken());
        return true;
    }



}
