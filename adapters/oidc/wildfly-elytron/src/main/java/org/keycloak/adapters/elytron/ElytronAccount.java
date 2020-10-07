/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.elytron;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ElytronAccount implements Serializable, OidcKeycloakAccount {

    private static final long serialVersionUID = -6775274346765339292L;
    protected static Logger log = Logger.getLogger(ElytronAccount.class);

    private final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal;

    public ElytronAccount(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        this.principal = principal;
    }

    @Override
    public RefreshableKeycloakSecurityContext getKeycloakSecurityContext() {
        return principal.getKeycloakSecurityContext();
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Set<String> getRoles() {
        Set<String> roles = new HashSet<>();

        return roles;
    }

    void setCurrentRequestInfo(KeycloakDeployment deployment, AdapterTokenStore tokenStore) {
        principal.getKeycloakSecurityContext().setCurrentRequestInfo(deployment, tokenStore);
    }

    public boolean checkActive() {
        RefreshableKeycloakSecurityContext session = getKeycloakSecurityContext();

        if (session.isActive() && !session.getDeployment().isAlwaysRefreshToken()) {
            log.debug("session is active");
            return true;
        }

        log.debug("session not active");

        return false;
    }

    boolean tryRefresh() {
        log.debug("Trying to refresh");

        RefreshableKeycloakSecurityContext securityContext = getKeycloakSecurityContext();

        if (securityContext == null) {
            log.debug("No security context. Aborting refresh.");
        }

        if (securityContext.refreshExpiredToken(false)) {
            log.debug("refresh succeeded");
            return true;
        }

        return checkActive();
    }
}
