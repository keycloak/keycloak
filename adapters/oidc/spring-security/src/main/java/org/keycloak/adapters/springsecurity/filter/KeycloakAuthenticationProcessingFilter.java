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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.KeycloakAuthenticationException;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationFailureHandler;
import org.keycloak.adapters.springsecurity.authentication.SpringSecurityRequestAuthenticator;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.keycloak.adapters.springsecurity.token.AdapterTokenStoreFactory;
import org.keycloak.adapters.springsecurity.token.SpringSecurityAdapterTokenStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

/**
 * Provides a Keycloak authentication processing filter.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
public class KeycloakAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter implements ApplicationContextAware {

    public static final String DEFAULT_LOGIN_URL = "/sso/login";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String SCHEME_BEARER = "bearer ";
    public static final String SCHEME_BASIC = "basic ";


    /**
     * Request matcher that matches requests to the {@link KeycloakAuthenticationEntryPoint#DEFAULT_LOGIN_URI default login URI}
     * and any request with a <code>Authorization</code> header.
     */
    public static final RequestMatcher DEFAULT_REQUEST_MATCHER =
            new OrRequestMatcher(new AntPathRequestMatcher(DEFAULT_LOGIN_URL), new RequestHeaderRequestMatcher(AUTHORIZATION_HEADER));

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthenticationProcessingFilter.class);

    private ApplicationContext applicationContext;
    private AdapterDeploymentContext adapterDeploymentContext;
    private AdapterTokenStoreFactory adapterTokenStoreFactory = new SpringSecurityAdapterTokenStoreFactory();
    private AuthenticationManager authenticationManager;

    /**
     * Creates a new Keycloak authentication processing filter with given {@link AuthenticationManager} and the
     * {@link KeycloakAuthenticationProcessingFilter#DEFAULT_REQUEST_MATCHER default request matcher}.
     *
     * @param authenticationManager the {@link AuthenticationManager} to authenticate requests (cannot be null)
     * @see KeycloakAuthenticationProcessingFilter#DEFAULT_REQUEST_MATCHER
     */
    public KeycloakAuthenticationProcessingFilter(AuthenticationManager authenticationManager) {
        this(authenticationManager, DEFAULT_REQUEST_MATCHER);
        setAuthenticationFailureHandler(new KeycloakAuthenticationFailureHandler());
    }

    /**
     * Creates a new Keycloak authentication processing filter with given {@link AuthenticationManager} and
     * {@link RequestMatcher}.
     * <p>
     *     Note: the given request matcher must support matching the <code>Authorization</code> header if
     *     bearer token authentication is to be accepted.
     * </p>
     *
     * @param authenticationManager the {@link AuthenticationManager} to authenticate requests (cannot be null)
     * @param requiresAuthenticationRequestMatcher the {@link RequestMatcher} used to determine if authentication
     *  is required (cannot be null)
     *
     *  @see RequestHeaderRequestMatcher
     *  @see OrRequestMatcher
     *
     */
    public KeycloakAuthenticationProcessingFilter(AuthenticationManager authenticationManager, RequestMatcher
            requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        this.authenticationManager = authenticationManager;
        super.setAuthenticationManager(authenticationManager);
        super.setAllowSessionCreation(false);
        super.setContinueChainBeforeSuccessfulAuthentication(false);
    }

    @Override
    public void afterPropertiesSet() {
        adapterDeploymentContext = applicationContext.getBean(AdapterDeploymentContext.class);
        super.afterPropertiesSet();
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        log.debug("Attempting Keycloak authentication");

        HttpFacade facade = new SimpleHttpFacade(request, response);
        KeycloakDeployment deployment = adapterDeploymentContext.resolveDeployment(facade);

        // using Spring authenticationFailureHandler
        deployment.setDelegateBearerErrorResponseSending(true);

        AdapterTokenStore tokenStore = adapterTokenStoreFactory.createAdapterTokenStore(deployment, request);
        RequestAuthenticator authenticator
                = new SpringSecurityRequestAuthenticator(facade, request, deployment, tokenStore, -1);

        AuthOutcome result = authenticator.authenticate();
        log.debug("Auth outcome: {}", result);

        if (AuthOutcome.FAILED.equals(result)) {
            AuthChallenge challenge = authenticator.getChallenge();
            if (challenge != null) {
                challenge.challenge(facade);
            }
            throw new KeycloakAuthenticationException("Invalid authorization header, see WWW-Authenticate header for details");
        }

        if (AuthOutcome.NOT_ATTEMPTED.equals(result)) {
            AuthChallenge challenge = authenticator.getChallenge();
            if (challenge != null) {
                challenge.challenge(facade);
            }
            if (deployment.isBearerOnly()) {
                // no redirection in this mode, throwing exception for the spring handler
                throw new KeycloakAuthenticationException("Authorization header not found,  see WWW-Authenticate header");
            } else {
                // let continue if challenged, it may redirect
                return null;
            }
        }

        else if (AuthOutcome.AUTHENTICATED.equals(result)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Assert.notNull(authentication, "Authentication SecurityContextHolder was null");
            return authenticationManager.authenticate(authentication);
        }
        else {
            AuthChallenge challenge = authenticator.getChallenge();
            if (challenge != null) {
                challenge.challenge(facade);
            }
            return null;
        }
    }

    /**
     * Returns true if the request was made with a bearer token authorization header.
     *
     * @param request the current <code>HttpServletRequest</code>
     *
     * @return <code>true</code> if the <code>request</code> was made with a bearer token authorization header;
     * <code>false</code> otherwise.
     */
    protected boolean isBearerTokenRequest(HttpServletRequest request) {
        String authValue = request.getHeader(AUTHORIZATION_HEADER);
        return authValue != null && authValue.toLowerCase().startsWith(SCHEME_BEARER);
    }

    /**
     * Returns true if the request was made with a Basic authentication authorization header.
     *
     * @param request the current <code>HttpServletRequest</code>
     * @return <code>true</code> if the <code>request</code> was made with a Basic authentication authorization header;
     * <code>false</code> otherwise.
     */
    protected boolean isBasicAuthRequest(HttpServletRequest request) {
        String authValue = request.getHeader(AUTHORIZATION_HEADER);
        return authValue != null && authValue.toLowerCase().startsWith(SCHEME_BASIC);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {

        if (!(this.isBearerTokenRequest(request) || this.isBasicAuthRequest(request))) {
            super.successfulAuthentication(request, response, chain, authResult);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Authentication success using bearer token/basic authentication. Updating SecurityContextHolder to contain: {}", authResult);
        }

        SecurityContextHolder.getContext().setAuthentication(authResult);

        // Fire event
        if (this.eventPublisher != null) {
            eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authResult, this.getClass()));
        }

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }

    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        super.unsuccessfulAuthentication(request, response, failed);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Sets the adapter token store factory to use when creating per-request adapter token stores.
     *
     * @param adapterTokenStoreFactory the <code>AdapterTokenStoreFactory</code> to use
     */
    public void setAdapterTokenStoreFactory(AdapterTokenStoreFactory adapterTokenStoreFactory) {
        Assert.notNull(adapterTokenStoreFactory, "AdapterTokenStoreFactory cannot be null");
        this.adapterTokenStoreFactory = adapterTokenStoreFactory;
    }

    /**
     * This filter does not support explicitly enabling session creation.
     *
     * @throws UnsupportedOperationException this filter does not support explicitly enabling session creation.
     */
    @Override
    public final void setAllowSessionCreation(boolean allowSessionCreation) {
        throw new UnsupportedOperationException("This filter does not support explicitly setting a session creation policy");
    }

    /**
     * This filter does not support explicitly setting a continue chain before success policy
     *
     * @throws UnsupportedOperationException this filter does not support explicitly setting a continue chain before success policy
     */
    @Override
    public final void setContinueChainBeforeSuccessfulAuthentication(boolean continueChainBeforeSuccessfulAuthentication) {
        throw new UnsupportedOperationException("This filter does not support explicitly setting a continue chain before success policy");
    }
}