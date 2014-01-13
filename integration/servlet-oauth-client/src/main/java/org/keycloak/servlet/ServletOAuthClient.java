package org.keycloak.servlet;

import org.apache.http.client.HttpClient;
import org.keycloak.AbstractOAuthClient;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.adapters.TokenGrantRequest;
import org.keycloak.util.KeycloakUriBuilder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletOAuthClient extends AbstractOAuthClient {
    protected HttpClient client;

    /**
     * Creates a Client for obtaining access token from code
     */
    public void start() {
        if (client == null) {
            client = new HttpClientBuilder().trustStore(truststore)
                    .hostnameVerification(HttpClientBuilder.HostnameVerificationPolicy.ANY)
                    .connectionPoolSize(10)
                    .build();
        }
    }

    /**
     * closes cllient
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

    public String resolveBearerToken(String redirectUri, String code) throws IOException, TokenGrantRequest.HttpFailure {
        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put("password", password);
        return TokenGrantRequest.invoke(client, code, codeUrl, redirectUri, clientId, credentials).getToken();
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

        KeycloakUriBuilder uriBuilder =  KeycloakUriBuilder.fromUri(authUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state);
        if (scope != null) {
            uriBuilder.queryParam("scope", scope);
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
            if (!name.equals("code")) continue;
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
     * @throws org.keycloak.adapters.TokenGrantRequest.HttpFailure
     */
    public String getBearerToken(HttpServletRequest request) throws IOException, TokenGrantRequest.HttpFailure {
        String error = request.getParameter("error");
        if (error != null) throw new IOException("OAuth error: " + error);
        String redirectUri = request.getRequestURL().append("?").append(request.getQueryString()).toString();
        String stateCookie = getCookieValue(stateCookieName, request);
        if (stateCookie == null) throw new IOException("state cookie not set");
        // we can call get parameter as this should be a redirect
        String state = request.getParameter("state");
        String code = request.getParameter("code");

        if (state == null) throw new IOException("state parameter was null");
        if (!state.equals(stateCookie)) {
            throw new IOException("state parameter invalid");
        }
        if (code == null) throw new IOException("code parameter was null");
        return resolveBearerToken(redirectUri, code);
    }


}
