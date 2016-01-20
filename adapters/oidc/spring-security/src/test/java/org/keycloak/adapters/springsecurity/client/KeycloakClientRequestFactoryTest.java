package org.keycloak.adapters.springsecurity.client;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.springsecurity.account.KeycloakRole;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Keycloak client request factory tests.
 */
public class KeycloakClientRequestFactoryTest {

    @Spy
    private KeycloakClientRequestFactory factory;

    @Mock
    private OidcKeycloakAccount account;

    @Mock
    private KeycloakAuthenticationToken keycloakAuthenticationToken;

    @Mock
    private KeycloakSecurityContext keycloakSecurityContext;

    @Mock
    private HttpUriRequest request;

    private String bearerTokenString;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        bearerTokenString = UUID.randomUUID().toString();

        SecurityContextHolder.getContext().setAuthentication(keycloakAuthenticationToken);
        when(keycloakAuthenticationToken.getAccount()).thenReturn(account);
        when(account.getKeycloakSecurityContext()).thenReturn(keycloakSecurityContext);
        when(keycloakSecurityContext.getTokenString()).thenReturn(bearerTokenString);
    }

    @Test
    public void testPostProcessHttpRequest() throws Exception {
        factory.postProcessHttpRequest(request);
        verify(factory).getKeycloakSecurityContext();
        verify(request).setHeader(eq(KeycloakClientRequestFactory.AUTHORIZATION_HEADER), eq("Bearer " + bearerTokenString));
    }

    @Test
    public void testGetKeycloakSecurityContext() throws Exception {
        KeycloakSecurityContext context = factory.getKeycloakSecurityContext();
        assertNotNull(context);
        assertEquals(keycloakSecurityContext, context);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetKeycloakSecurityContextInvalidAuthentication() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new PreAuthenticatedAuthenticationToken("foo", "bar", Collections.singleton(new KeycloakRole("baz"))));
        factory.getKeycloakSecurityContext();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetKeycloakSecurityContextNullAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        factory.getKeycloakSecurityContext();
    }
}
