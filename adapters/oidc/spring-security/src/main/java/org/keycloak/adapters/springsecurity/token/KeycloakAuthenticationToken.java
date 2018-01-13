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

package org.keycloak.adapters.springsecurity.token;

import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.security.Principal;
import java.util.Collection;

/**
 * Represents the token for a Keycloak authentication request or for an authenticated principal once the request has been
 * processed by the {@link AuthenticationManager#authenticate(Authentication)}.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticationToken extends AbstractAuthenticationToken implements Authentication {

    private Principal principal;
    private boolean interactive;

    /**
     * Creates a new, unauthenticated Keycloak security token for the given account.
     */
    public KeycloakAuthenticationToken(KeycloakAccount account, boolean interactive) {
        super(null);
        Assert.notNull(account, "KeycloakAccount cannot be null");
        Assert.notNull(account.getPrincipal(), "KeycloakAccount.getPrincipal() cannot be null");
        this.principal = account.getPrincipal();
        this.setDetails(account);
        this.interactive = interactive;
    }

    public KeycloakAuthenticationToken(KeycloakAccount account, boolean interactive, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        Assert.notNull(account, "KeycloakAccount cannot be null");
        Assert.notNull(account.getPrincipal(), "KeycloakAccount.getPrincipal() cannot be null");
        this.principal = account.getPrincipal();
        this.setDetails(account);
        this.interactive = interactive;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.getAccount().getKeycloakSecurityContext();
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public OidcKeycloakAccount getAccount() {
        return (OidcKeycloakAccount) this.getDetails();
    }

    public boolean isInteractive() {
        return interactive;
    }
}
