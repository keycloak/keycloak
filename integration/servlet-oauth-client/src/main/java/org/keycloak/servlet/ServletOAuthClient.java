package org.keycloak.servlet;

import org.apache.http.client.HttpClient;
import org.keycloak.AbstractOAuthClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.util.KeycloakUriBuilder;
import org.keycloak.util.UriUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletOAuthClient extends AbstractOAuthClient {
    protected HttpClient client;

    public void start() {
    }

    /**
     * closes client
     */
    public void stop() {
        client.getConnectionManager().shutdown();
    }
    public HttpClient getClient() {
        return client;
    }

    public void setClient(HttpClient client) {
        this.client = client;
    }

    private AccessTokenResponse resolveBearerToken(HttpServletRequest request, String redirectUri, String code) throws IOException, ServerRequest.HttpFailure {
        // Don't send sessionId in oauth clients for now
        return ServerRequest.invokeAccessCodeToToken(client, publicClient, code, getUrl(request, tokenUrl, false), redirectUri, clientId, credentials, null);
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

        KeycloakUriBuilder uriBuilder =  KeycloakUriBuilder.fromUri(getUrl(request, authUrl, true))
                .queryParam(OAuth2Constants.CLIENT_ID, clientId)
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.STATE, state);
        if (scope != null) {
            uriBuilder.queryParam(OAuth2Constants.SCOPE, scope);
        }
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
        return ServerRequest.invokeRefresh(client, publicClient, refreshToken, getUrl(request, tokenUrl, false), clientId, credentials);
    }

    public static IDToken extractIdToken(String idToken) {
        if (idToken == null) return null;
        JWSInput input = new JWSInput(idToken);
        try {
            return input.readJsonContent(IDToken.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUrl(HttpServletRequest request, String url, boolean isBrowserRequest) {
        if (relativeUrlsUsed.useRelative(isBrowserRequest)) {
            String baseUrl = UriUtils.getOrigin(request.getRequestURL().toString());
            return baseUrl + url;
        } else {
            return url;
        }
    }

}
