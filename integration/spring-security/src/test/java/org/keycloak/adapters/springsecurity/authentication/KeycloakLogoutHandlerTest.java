package org.keycloak.adapters.springsecurity.authentication;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextBean;
import org.keycloak.adapters.springsecurity.account.KeycloakRole;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Keycloak logout handler tests.
 */
public class KeycloakLogoutHandlerTest {

    private KeycloakAuthenticationToken keycloakAuthenticationToken;
    private KeycloakLogoutHandler keycloakLogoutHandler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Mock
    private AdapterDeploymentContextBean adapterDeploymentContextBean;

    @Mock
    private OidcKeycloakAccount keycloakAccount;

    @Mock
    private KeycloakDeployment keycloakDeployment;

    @Mock
    private RefreshableKeycloakSecurityContext session;

    private Collection<KeycloakRole> authorities = Collections.singleton(new KeycloakRole(UUID.randomUUID().toString()));

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        keycloakAuthenticationToken = mock(KeycloakAuthenticationToken.class);
        keycloakLogoutHandler = new KeycloakLogoutHandler(adapterDeploymentContextBean);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        when(adapterDeploymentContextBean.getDeployment()).thenReturn(keycloakDeployment);
        when(keycloakAuthenticationToken.getAccount()).thenReturn(keycloakAccount);
        when(keycloakAccount.getKeycloakSecurityContext()).thenReturn(session);
    }

    @Test
    public void testLogout() throws Exception {
        keycloakLogoutHandler.logout(request, response, keycloakAuthenticationToken);
        verify(session).logout(eq(keycloakDeployment));
    }

    @Test
    public void testLogoutAnonymousAuthentication() throws Exception {
        Authentication authentication = new AnonymousAuthenticationToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), authorities);
        keycloakLogoutHandler.logout(request, response, authentication);
        verifyZeroInteractions(session);
    }

    @Test
    public void testLogoutUsernamePasswordAuthentication() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), authorities);
        keycloakLogoutHandler.logout(request, response, authentication);
        verifyZeroInteractions(session);
    }

    @Test
    public void testLogoutRememberMeAuthentication() throws Exception {
        Authentication authentication = new RememberMeAuthenticationToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), authorities);
        keycloakLogoutHandler.logout(request, response, authentication);
        verifyZeroInteractions(session);
    }

    @Test
    public void testLogoutNullAuthentication() throws Exception {
        keycloakLogoutHandler.logout(request, response, null);
        verifyZeroInteractions(session);
    }

    @Test
    public void testHandleSingleSignOut() throws Exception {
        keycloakLogoutHandler.handleSingleSignOut(request, response, keycloakAuthenticationToken);
        verify(session).logout(eq(keycloakDeployment));
    }
}
