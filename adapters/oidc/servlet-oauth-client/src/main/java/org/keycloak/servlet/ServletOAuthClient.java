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

package org.keycloak.servlet;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.util.TokenUtil;

import javax.security.cert.X509Certificate;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64Url;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletOAuthClient extends KeycloakDeploymentDelegateOAuthClient {

	// https://tools.ietf.org/html/rfc7636#section-4
	private String codeVerifier;
	private String codeChallenge;
	private String codeChallengeMethod = OAuth2Constants.PKCE_METHOD_S256;
	private static Logger logger = Logger.getLogger(ServletOAuthClient.class);

    public static String generateSecret() {
        return generateSecret(32);
    }

    public static String generateSecret(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return Base64Url.encode(buf);
    }

    private void setCodeVerifier() {
        codeVerifier = generateSecret();
        logger.debugf("Generated codeVerifier = %s", codeVerifier);
        return;
    }

    private void setCodeChallenge() {
        try {
            if (codeChallengeMethod.equals(OAuth2Constants.PKCE_METHOD_S256)) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(codeVerifier.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : md.digest()) {
                    String hex = String.format("%02x", b);
                    sb.append(hex);
                }
                codeChallenge = Base64Url.encode(sb.toString().getBytes());
            } else {
                codeChallenge = Base64Url.encode(codeVerifier.getBytes());
            }
            logger.debugf("Encode codeChallenge = %s, codeChallengeMethod = %s", codeChallenge, codeChallengeMethod);
        } catch (Exception e) {
            logger.info("PKCE client side unknown hash algorithm");
            codeChallenge = Base64Url.encode(codeVerifier.getBytes());
        }
    }

    /**
     * closes client
     */
    public void stop() {
        getDeployment().getClient().getConnectionManager().shutdown();
    }

    private AccessTokenResponse resolveBearerToken(HttpServletRequest request, String redirectUri, String code) throws IOException, ServerRequest.HttpFailure {
        // Don't send sessionId in oauth clients for now
        KeycloakDeployment resolvedDeployment = resolveDeployment(getDeployment(), request);

        // https://tools.ietf.org/html/rfc7636#section-4
        if (codeVerifier != null) {
            logger.debugf("Before sending Token Request, codeVerifier = %s", codeVerifier);
            return ServerRequest.invokeAccessCodeToToken(resolvedDeployment, code, redirectUri, null, codeVerifier);
        } else {
            logger.debug("Before sending Token Request without codeVerifier");
            return ServerRequest.invokeAccessCodeToToken(resolvedDeployment, code, redirectUri, null);
        }
    }

    /**
     * Start the process of obtaining an access token by redirecting the browser
     * to the authentication server
     *
     * @param relativePath path relative to context root you want auth server to redirect back to
     * @param request
     * @param response
     * @throws IOException
     */
    public void redirectRelative(String relativePath, HttpServletRequest request, HttpServletResponse response) throws IOException {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(request.getRequestURL().toString())
                .replacePath(request.getContextPath())
                .replaceQuery(null)
                .path(relativePath);
        String redirect = builder.toTemplate();
        redirect(redirect, request, response);
    }


    /**
     * Start the process of obtaining an access token by redirecting the browser
     * to the authentication server
     *
     * @param redirectUri full URI you want auth server to redirect back to
     * @param request
     * @param response
     * @throws IOException
     */
    public void redirect(String redirectUri, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String state = getStateCode();
        KeycloakDeployment resolvedDeployment = resolveDeployment(getDeployment(), request);
        String authUrl = resolvedDeployment.getAuthUrl().clone().build().toString();
        String scopeParam = TokenUtil.attachOIDCScope(scope);

        // https://tools.ietf.org/html/rfc7636#section-4
        if (resolvedDeployment.isPkce()) {
            setCodeVerifier();
            setCodeChallenge();
        }

        KeycloakUriBuilder uriBuilder =  KeycloakUriBuilder.fromUri(authUrl)
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, getClientId())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.STATE, state)
                .queryParam(OAuth2Constants.SCOPE, scopeParam);

        URI url = uriBuilder.build();

        String stateCookiePath = this.stateCookiePath;
        if (stateCookiePath == null) stateCookiePath = request.getContextPath();
        if (stateCookiePath.equals("")) stateCookiePath = "/";

        Cookie cookie = new Cookie(stateCookieName, state);
        cookie.setSecure(isSecure);
        cookie.setPath(stateCookiePath);
        response.addCookie(cookie);
        response.sendRedirect(url.toString());
    }

    protected String getCookieValue(String name, HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }

    protected String getCode(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null) return null;
        String[] params = query.split("&");
        for (String param : params) {
            int eq = param.indexOf('=');
            if (eq == -1) continue;
            String name = param.substring(0, eq);
            if (!name.equals(OAuth2Constants.CODE)) continue;
            return param.substring(eq + 1);
        }
        return null;
    }


    /**
     * Obtain the code parameter from the url after being redirected back from the auth-server.  Then
     * do an authenticated request back to the auth-server to turn the access code into an access token.
     *
     * @param request
     * @return
     * @throws IOException
     * @throws org.keycloak.adapters.ServerRequest.HttpFailure
     */
    public AccessTokenResponse getBearerToken(HttpServletRequest request) throws IOException, ServerRequest.HttpFailure {
        String error = request.getParameter(OAuth2Constants.ERROR);
        if (error != null) throw new IOException("OAuth error: " + error);
        String redirectUri = request.getRequestURL().append("?").append(request.getQueryString()).toString();
        String stateCookie = getCookieValue(stateCookieName, request);
        if (stateCookie == null) throw new IOException("state cookie not set");
        // we can call get parameter as this should be a redirect
        String state = request.getParameter(OAuth2Constants.STATE);
        String code = request.getParameter(OAuth2Constants.CODE);

        if (state == null) throw new IOException("state parameter was null");
        if (!state.equals(stateCookie)) {
            throw new IOException("state parameter invalid");
        }
        if (code == null) throw new IOException("code parameter was null");
        return resolveBearerToken(request, redirectUri, code);
    }

    public AccessTokenResponse refreshToken(HttpServletRequest request, String refreshToken) throws IOException, ServerRequest.HttpFailure {
        KeycloakDeployment resolvedDeployment = resolveDeployment(getDeployment(), request);
        return ServerRequest.invokeRefresh(resolvedDeployment, refreshToken);
    }

    public static IDToken extractIdToken(String idToken) {
        if (idToken == null) return null;
        try {
            JWSInput input = new JWSInput(idToken);
            return input.readJsonContent(IDToken.class);
        } catch (JWSInputException e) {
            throw new RuntimeException(e);
        }
    }

    private KeycloakDeployment resolveDeployment(KeycloakDeployment baseDeployment, HttpServletRequest request) {
        ServletFacade facade = new ServletFacade(request);
        return new AdapterDeploymentContext(baseDeployment).resolveDeployment(facade);
    }


    public static class ServletFacade implements OIDCHttpFacade {

        private final HttpServletRequest servletRequest;

        private ServletFacade(HttpServletRequest servletRequest) {
            this.servletRequest = servletRequest;
        }

        @Override
        public KeycloakSecurityContext getSecurityContext() {
            throw new IllegalStateException("Not yet implemented");
        }

        @Override
        public Request getRequest() {
            return new Request() {

                @Override
                public String getFirstParam(String param) {
                    return servletRequest.getParameter(param);
                }

                @Override
                public String getMethod() {
                    return servletRequest.getMethod();
                }

                @Override
                public String getURI() {
                    return servletRequest.getRequestURL().toString();
                }

                @Override
                public String getRelativePath() {
                    return servletRequest.getServletPath();
                }

                @Override
                public boolean isSecure() {
                    return servletRequest.isSecure();
                }

                @Override
                public String getQueryParamValue(String param) {
                    return servletRequest.getParameter(param);
                }

                @Override
                public Cookie getCookie(String cookieName) {
                    // TODO
                    return null;
                }

                @Override
                public String getHeader(String name) {
                    return servletRequest.getHeader(name);
                }

                @Override
                public List<String> getHeaders(String name) {
                    // TODO
                    return null;
                }

                @Override
                public InputStream getInputStream() {
                    try {
                        return servletRequest.getInputStream();
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }

                @Override
                public String getRemoteAddr() {
                    return servletRequest.getRemoteAddr();
                }

                @Override
                public void setError(AuthenticationError error) {
                    servletRequest.setAttribute(AuthenticationError.class.getName(), error);

                }

                @Override
                public void setError(LogoutError error) {
                    servletRequest.setAttribute(LogoutError.class.getName(), error);
                }

            };
        }

        @Override
        public Response getResponse() {
            throw new IllegalStateException("Not yet implemented");
        }

        @Override
        public X509Certificate[] getCertificateChain() {
            throw new IllegalStateException("Not yet implemented");
        }
    }
}
