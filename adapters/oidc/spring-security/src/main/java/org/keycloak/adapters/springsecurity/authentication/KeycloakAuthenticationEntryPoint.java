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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provides a Keycloak {@link AuthenticationEntryPoint authentication entry point}. Uses a
 * {@link RequestMatcher} to determine if the request is an interactive login request or a
 * API request, which should not be redirected to an interactive login page. By default,
 * this entry point uses a {@link HttpHeaderInspectingApiRequestMatcher} but can be overridden using in the
 * constructor.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 *
 * @see HttpHeaderInspectingApiRequestMatcher
 */
public class KeycloakAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Default Keycloak authentication login URI
     */
    public static final String DEFAULT_LOGIN_URI = "/sso/login";
    private static final String DEFAULT_REALM = "Unknown";
    private static final RequestMatcher DEFAULT_API_REQUEST_MATCHER = new HttpHeaderInspectingApiRequestMatcher();

    private final static Logger log = LoggerFactory.getLogger(KeycloakAuthenticationEntryPoint.class);

    private final RequestMatcher apiRequestMatcher;
    private String loginUri = DEFAULT_LOGIN_URI;
    private String realm = DEFAULT_REALM;

    private AdapterDeploymentContext adapterDeploymentContext;

    /**
     * Creates a new Keycloak authentication entry point.
     */
    public KeycloakAuthenticationEntryPoint(AdapterDeploymentContext adapterDeploymentContext) {
        this(adapterDeploymentContext, DEFAULT_API_REQUEST_MATCHER);
    }

    /**
     * Creates a new Keycloak authentication entry point using the given request
     * matcher to determine if the current request is an API request or a browser request.
     *
     * @param apiRequestMatcher the <code>RequestMatcher</code> to use to determine
     * if the current request is an API request or a browser request (required)
     */
    public KeycloakAuthenticationEntryPoint(AdapterDeploymentContext adapterDeploymentContext, RequestMatcher apiRequestMatcher) {
        Assert.notNull(apiRequestMatcher, "apiRequestMatcher required");
        Assert.notNull(adapterDeploymentContext, "adapterDeploymentContext required");
        this.adapterDeploymentContext = adapterDeploymentContext;
        this.apiRequestMatcher = apiRequestMatcher;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        HttpFacade facade = new SimpleHttpFacade(request, response);
        if (apiRequestMatcher.matches(request) || adapterDeploymentContext.resolveDeployment(facade).isBearerOnly()) {
            commenceUnauthorizedResponse(request, response);
        } else {
            commenceLoginRedirect(request, response);
        }
    }

    /**
     * Redirects to the login page. If HTTP sessions are disabled, the redirect URL is saved in a
     * cookie now, to be retrieved by the {@link KeycloakAuthenticationSuccessHandler} or the
     * {@link KeycloakAuthenticationFailureHandler} when the login sequence completes.
     */
    protected void commenceLoginRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getSession(false) == null && KeycloakCookieBasedRedirect.getRedirectUrlFromCookie(request) == null) {
            // If no session exists yet at this point, then apparently the redirect URL is not
            // stored in a session. We'll store it in a cookie instead.
            response.addCookie(KeycloakCookieBasedRedirect.createCookieFromRedirectUrl(request.getRequestURI()));
        }

        String queryParameters = "";
        if (!StringUtils.isEmpty(request.getQueryString())) {
            queryParameters = "?" + request.getQueryString();
        }

        String contextAwareLoginUri = request.getContextPath() + loginUri + queryParameters;
        log.debug("Redirecting to login URI {}", contextAwareLoginUri);
        response.sendRedirect(contextAwareLoginUri);
    }

    protected void commenceUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.addHeader(HttpHeaders.WWW_AUTHENTICATE, String.format("Bearer realm=\"%s\"", realm));
        response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
    }

    public void setLoginUri(String loginUri) {
        Assert.notNull(loginUri, "loginUri cannot be null");
        this.loginUri = loginUri;
    }

    public void setRealm(String realm) {
        Assert.notNull(realm, "realm cannot be null");
        this.realm = realm;
    }
}
