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

package org.keycloak.adapters.springsecurity.filter;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.KeycloakAuthenticationException;
import org.keycloak.adapters.springsecurity.account.KeycloakRole;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationEntryPoint;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationFailureHandler;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.KeycloakUriBuilder;
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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Keycloak authentication process filter test cases.
 */
public class KeycloakAuthenticationProcessingFilterTest {

    private KeycloakAuthenticationProcessingFilter filter;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AdapterDeploymentContext adapterDeploymentContext;

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
    
    private KeycloakAuthenticationFailureHandler keycloakFailureHandler;

    @Mock
    private OidcKeycloakAccount keycloakAccount;

    @Mock
    private KeycloakDeployment keycloakDeployment;

    @Mock
    private KeycloakSecurityContext keycloakSecurityContext;

    private final List<? extends GrantedAuthority> authorities = Collections.singletonList(new KeycloakRole("ROLE_USER"));

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        request = spy(new MockHttpServletRequest());
        request.setRequestURI("http://host");
        filter = new KeycloakAuthenticationProcessingFilter(authenticationManager);
        keycloakFailureHandler = new KeycloakAuthenticationFailureHandler();

        filter.setApplicationContext(applicationContext);
        filter.setAuthenticationSuccessHandler(successHandler);
        filter.setAuthenticationFailureHandler(failureHandler);

        when(applicationContext.getBean(eq(AdapterDeploymentContext.class))).thenReturn(adapterDeploymentContext);
        when(adapterDeploymentContext.resolveDeployment(any(HttpFacade.class))).thenReturn(keycloakDeployment);
        when(keycloakAccount.getPrincipal()).thenReturn(
                new KeycloakPrincipal<KeycloakSecurityContext>(UUID.randomUUID().toString(), keycloakSecurityContext));


        filter.afterPropertiesSet();
    }

    @Test
    public void testAttemptAuthenticationExpectRedirect() throws Exception {
        when(keycloakDeployment.getAuthUrl()).thenReturn(KeycloakUriBuilder.fromUri("http://localhost:8080/auth"));
        when(keycloakDeployment.getResourceName()).thenReturn("resource-name");
        when(keycloakDeployment.getStateCookieName()).thenReturn("kc-cookie");
        when(keycloakDeployment.getSslRequired()).thenReturn(SslRequired.NONE);
        when(keycloakDeployment.isBearerOnly()).thenReturn(Boolean.FALSE);

        filter.attemptAuthentication(request, response);
        verify(response).setStatus(302);
        verify(response).setHeader(eq("Location"), startsWith("http://localhost:8080/auth"));
    }

    @Test(expected = KeycloakAuthenticationException.class)
    public void testAttemptAuthenticationWithInvalidToken() throws Exception {
        request.addHeader("Authorization", "Bearer xxx");
        filter.attemptAuthentication(request, response);
    }

    @Test(expected = KeycloakAuthenticationException.class)
    public void testAttemptAuthenticationWithInvalidTokenBearerOnly() throws Exception {
        when(keycloakDeployment.isBearerOnly()).thenReturn(Boolean.TRUE);
        request.addHeader("Authorization", "Bearer xxx");
        filter.attemptAuthentication(request, response);
    }

    @Test
    public void testSuccessfulAuthenticationInteractive() throws Exception {
        request.setRequestURI("http://host" + KeycloakAuthenticationEntryPoint.DEFAULT_LOGIN_URI + "?query");
        Authentication authentication = new KeycloakAuthenticationToken(keycloakAccount, true, authorities);
        filter.successfulAuthentication(request, response, chain, authentication);

        verify(successHandler).onAuthenticationSuccess(eq(request), eq(response), eq(authentication));
        verify(chain, never()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    public void testSuccessfulAuthenticationBearer() throws Exception {
        Authentication authentication = new KeycloakAuthenticationToken(keycloakAccount, false, authorities);
        this.setBearerAuthHeader(request);
        filter.successfulAuthentication(request, response, chain, authentication);

        verify(chain).doFilter(eq(request), eq(response));
        verify(successHandler, never()).onAuthenticationSuccess(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(Authentication.class));
    }

    @Test
    public void testSuccessfulAuthenticationBasicAuth() throws Exception {
        Authentication authentication = new KeycloakAuthenticationToken(keycloakAccount, false, authorities);
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
        verify(failureHandler).onAuthenticationFailure(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(AuthenticationException.class));
    }

    @Test
    public void testUnsuccessfulAuthenticatioBasicAuth() throws Exception {
        AuthenticationException exception = new BadCredentialsException("OOPS");
        this.setBasicAuthHeader(request);
        filter.unsuccessfulAuthentication(request, response, exception);
        verify(failureHandler).onAuthenticationFailure(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(AuthenticationException.class));
    }
    
    @Test
    public void testDefaultFailureHanlder() throws Exception {
        AuthenticationException exception = new BadCredentialsException("OOPS");
        filter.setAuthenticationFailureHandler(keycloakFailureHandler);
        filter.unsuccessfulAuthentication(request, response, exception);
        
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), any(String.class));
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
        setAuthorizationHeader(request, "Bearer");
    }

    private void setBasicAuthHeader(MockHttpServletRequest request) {
        setAuthorizationHeader(request, "Basic");
    }

    private void setAuthorizationHeader(MockHttpServletRequest request, String scheme) {
      request.addHeader(KeycloakAuthenticationProcessingFilter.AUTHORIZATION_HEADER, scheme + " " + UUID.randomUUID().toString());
    }
}
