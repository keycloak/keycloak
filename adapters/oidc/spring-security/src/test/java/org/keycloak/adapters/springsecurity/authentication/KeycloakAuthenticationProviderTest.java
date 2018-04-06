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

package org.keycloak.adapters.springsecurity.authentication;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.internal.util.collections.Sets;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.security.Principal;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Keycloak authentication provider tests.
 */
public class KeycloakAuthenticationProviderTest {
    private KeycloakAuthenticationProvider provider = new KeycloakAuthenticationProvider();
    private KeycloakAuthenticationToken token;
    private KeycloakAuthenticationToken interactiveToken;
    private Set<String> roles = Sets.newSet("user", "admin");

    @Before
    public void setUp() throws Exception {
        Principal principal = mock(Principal.class);
        RefreshableKeycloakSecurityContext securityContext = mock(RefreshableKeycloakSecurityContext.class);
        KeycloakAccount account = new SimpleKeycloakAccount(principal, roles, securityContext);

        token = new KeycloakAuthenticationToken(account, false);
        interactiveToken = new KeycloakAuthenticationToken(account, true);
    }

    @Test
    public void testAuthenticate() throws Exception {
        assertAuthenticationResult(provider.authenticate(token));
    }

    @Test
    public void testAuthenticateInteractive() throws Exception {
        assertAuthenticationResult(provider.authenticate(interactiveToken));
    }

    @Test
    public void testSupports() throws Exception {
        assertTrue(provider.supports(KeycloakAuthenticationToken.class));
        assertFalse(provider.supports(PreAuthenticatedAuthenticationToken.class));
    }

    @Test
    public void testGrantedAuthoritiesMapper() throws Exception {
        SimpleAuthorityMapper grantedAuthorityMapper = new SimpleAuthorityMapper();
        grantedAuthorityMapper.setPrefix("ROLE_");
        grantedAuthorityMapper.setConvertToUpperCase(true);
        provider.setGrantedAuthoritiesMapper(grantedAuthorityMapper);

        Authentication result = provider.authenticate(token);
        assertEquals(Sets.newSet("ROLE_USER", "ROLE_ADMIN"),
            AuthorityUtils.authorityListToSet(result.getAuthorities()));
    }

    private void assertAuthenticationResult(Authentication result) {
        assertNotNull(result);
        assertEquals(roles, AuthorityUtils.authorityListToSet(result.getAuthorities()));
        assertTrue(result.isAuthenticated());
        assertNotNull(result.getPrincipal());
        assertNotNull(result.getCredentials());
        assertNotNull(result.getDetails());
    }
}
