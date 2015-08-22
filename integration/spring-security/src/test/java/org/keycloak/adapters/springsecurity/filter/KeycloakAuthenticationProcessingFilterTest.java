package org.keycloak.adapters.springsecurity.filter;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextBean;
import org.keycloak.adapters.springsecurity.account.KeycloakRole;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Keycloak authentication process filter test cases.
 */
public class KeycloakAuthenticationProcessingFilterTest {

    private KeycloakAuthenticationProcessingFilter filter;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AdapterDeploymentContextBean adapterDeploymentContextBean;

    @Mock
    private FilterChain chain;

    private MockHttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private AuthenticationSuccessHandler successHandler;

    @Mock
    private AuthenticationFailureHandler failureHandler;

    @Mock
    private KeycloakAccount keycloakAccount;

    @Mock
    private KeycloakDeployment keycloakDeployment;

    @Mock
    private KeycloakSecurityContext keycloakSecurityContext;

    private final List<? extends GrantedAuthority> authorities = Collections.singletonList(new KeycloakRole("ROLE_USER"));

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        request = spy(new MockHttpServletRequest());
        filter = new KeycloakAuthenticationProcessingFilter(authenticationManager);

        filter.setApplicationContext(applicationContext);
        filter.setAuthenticationSuccessHandler(successHandler);
        filter.setAuthenticationFailureHandler(failureHandler);

        when(applicationContext.getBean(eq(AdapterDeploymentContextBean.class))).thenReturn(adapterDeploymentContextBean);
        when(adapterDeploymentContextBean.getDeployment()).thenReturn(keycloakDeployment);
        when(keycloakAccount.getPrincipal()).thenReturn(
                new KeycloakPrincipal<KeycloakSecurityContext>(UUID.randomUUID().toString(), keycloakSecurityContext));


        filter.afterPropertiesSet();
    }

    @Test
    public void testIsBearerTokenRequest() throws Exception {
        assertFalse(filter.isBearerTokenRequest(request));
        this.setBearerAuthHeader(request);
        assertTrue(filter.isBearerTokenRequest(request));
    }

    @Test
    public void testIsBasicAuthRequest() throws Exception {
        assertFalse(filter.isBasicAuthRequest(request));
        this.setBasicAuthHeader(request);
        assertTrue(filter.isBasicAuthRequest(request));
    }

    @Test
    public void testSuccessfulAuthenticationInteractive() throws Exception {
        Authentication authentication = new KeycloakAuthenticationToken(keycloakAccount, authorities);
        filter.successfulAuthentication(request, response, chain, authentication);

        verify(successHandler).onAuthenticationSuccess(eq(request), eq(response), eq(authentication));
        verify(chain, never()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    public void testSuccessfulAuthenticationBearer() throws Exception {
        Authentication authentication = new KeycloakAuthenticationToken(keycloakAccount, authorities);
        this.setBearerAuthHeader(request);
        filter.successfulAuthentication(request, response, chain, authentication);

        verify(chain).doFilter(eq(request), eq(response));
        verify(successHandler, never()).onAuthenticationSuccess(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(Authentication.class));
    }

    @Test
    public void testSuccessfulAuthenticationBasicAuth() throws Exception {
        Authentication authentication = new KeycloakAuthenticationToken(keycloakAccount, authorities);
        this.setBasicAuthHeader(request);
        filter.successfulAuthentication(request, response, chain, authentication);

        verify(chain).doFilter(eq(request), eq(response));
        verify(successHandler, never()).onAuthenticationSuccess(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(Authentication.class));
    }

    @Test
    public void testUnsuccessfulAuthenticationInteractive() throws Exception {
        AuthenticationException exception = new BadCredentialsException("OOPS");
        filter.unsuccessfulAuthentication(request, response, exception);
        verify(failureHandler).onAuthenticationFailure(eq(request), eq(response), eq(exception));
    }

    @Test
    public void testUnsuccessfulAuthenticatioBearer() throws Exception {
        AuthenticationException exception = new BadCredentialsException("OOPS");
        this.setBearerAuthHeader(request);
        filter.unsuccessfulAuthentication(request, response, exception);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verify(failureHandler, never()).onAuthenticationFailure(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(AuthenticationException.class));
    }

    @Test
    public void testUnsuccessfulAuthenticatioBasicAuth() throws Exception {
        AuthenticationException exception = new BadCredentialsException("OOPS");
        this.setBasicAuthHeader(request);
        filter.unsuccessfulAuthentication(request, response, exception);
        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        verify(failureHandler, never()).onAuthenticationFailure(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(AuthenticationException.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetAllowSessionCreation() throws Exception {
        filter.setAllowSessionCreation(true);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetContinueChainBeforeSuccessfulAuthentication() throws Exception {
        filter.setContinueChainBeforeSuccessfulAuthentication(true);
    }

    private void setBearerAuthHeader(MockHttpServletRequest request) {
        request.addHeader(KeycloakAuthenticationProcessingFilter.AUTHORIZATION_HEADER, "Bearer " + UUID.randomUUID().toString());
    }

    private void setBasicAuthHeader(MockHttpServletRequest request) {
        request.addHeader(KeycloakAuthenticationProcessingFilter.AUTHORIZATION_HEADER, "Basic " + UUID.randomUUID().toString());
    }

}
