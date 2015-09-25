package org.keycloak.adapters.springsecurity.token;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.springsecurity.account.KeycloakRole;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.security.Principal;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Spring Security token store tests.
 */
public class SpringSecurityTokenStoreTest {

    private SpringSecurityTokenStore store;

    @Mock
    private KeycloakDeployment deployment;

    @Mock
    private Principal principal;

    @Mock
    private RequestAuthenticator requestAuthenticator;

    @Mock
    private RefreshableKeycloakSecurityContext keycloakSecurityContext;

    private MockHttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        request = new MockHttpServletRequest();
        store = new SpringSecurityTokenStore(deployment, request);
    }

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testIsCached() throws Exception {
        Authentication authentication = new PreAuthenticatedAuthenticationToken("foo", "bar", Collections.singleton(new KeycloakRole("ROLE_FOO")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        assertFalse(store.isCached(requestAuthenticator));
    }

    @Test
    public void testSaveAccountInfo() throws Exception {
        OidcKeycloakAccount account = new SimpleKeycloakAccount(principal, Collections.singleton("FOO"), keycloakSecurityContext);
        Authentication authentication;

        store.saveAccountInfo(account);
        authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication instanceof KeycloakAuthenticationToken);
    }

    @Test(expected = IllegalStateException.class)
    public void testSaveAccountInfoInvalidAuthenticationType() throws Exception {
        OidcKeycloakAccount account = new SimpleKeycloakAccount(principal, Collections.singleton("FOO"), keycloakSecurityContext);
        Authentication authentication = new PreAuthenticatedAuthenticationToken("foo", "bar", Collections.singleton(new KeycloakRole("ROLE_FOO")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        store.saveAccountInfo(account);
    }

    @Test
    public void testLogout() throws Exception {
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        assertFalse(session.isInvalid());
        store.logout();
        assertTrue(session.isInvalid());
    }
}
