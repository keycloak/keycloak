package org.keycloak.adapters.springsecurity.facade;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.internal.util.collections.Sets;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class SimpleHttpFacadeTest {

    @Before
    public void setup() {
        SecurityContext springSecurityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(springSecurityContext);
        Set<String> roles = Sets.newSet("user");
        Principal principal = mock(Principal.class);
        RefreshableKeycloakSecurityContext keycloakSecurityContext = mock(RefreshableKeycloakSecurityContext.class);
        KeycloakAccount account = new SimpleKeycloakAccount(principal, roles, keycloakSecurityContext);
        KeycloakAuthenticationToken token = new KeycloakAuthenticationToken(account, false);
        springSecurityContext.setAuthentication(token);
    }

    @Test
    public void shouldRetrieveKeycloakSecurityContext() {
        SimpleHttpFacade facade = new SimpleHttpFacade(new MockHttpServletRequest(), new MockHttpServletResponse());

        assertNotNull(facade.getSecurityContext());
    }
}
