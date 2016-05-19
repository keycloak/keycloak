package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.JsonSerialization;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineTokenServlet extends HttpServlet {

    private static final String OFFLINE_CLIENT_APP_URI = (System.getProperty("app.server.ssl.required", "false").equals("true")) ?
            System.getProperty("app.server.ssl.base.url", "https://localhost:8643") + "/offline-client" :
            System.getProperty("app.server.base.url", "http://localhost:8280") + "/offline-client";
    private static final String ADAPTER_ROOT_URL = (System.getProperty("auth.server.ssl.required", "false").equals("true")) ?
            System.getProperty("auth.server.ssl.base.url", "https://localhost:8543") :
            System.getProperty("auth.server.base.url", "http://localhost:8180");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //Accept timeOffset as argument to enforce timeouts
        String timeOffsetParam = req.getParameter("timeOffset");
        if (timeOffsetParam != null && !timeOffsetParam.isEmpty()) {
            Time.setOffset(Integer.parseInt(timeOffsetParam));
        }

        if (req.getRequestURI().endsWith("logout")) {

            UriBuilder redirectUriBuilder = UriBuilder.fromUri(OFFLINE_CLIENT_APP_URI);
            if (req.getParameter(OAuth2Constants.SCOPE) != null) {
                redirectUriBuilder.queryParam(OAuth2Constants.SCOPE, req.getParameter(OAuth2Constants.SCOPE));
            }
            String redirectUri = redirectUriBuilder.build().toString();

            String serverLogoutRedirect = UriBuilder.fromUri(ADAPTER_ROOT_URL + "/auth/realms/test/protocol/openid-connect/logout")
                    .queryParam("redirect_uri", redirectUri)
                    .build().toString();

            resp.sendRedirect(serverLogoutRedirect);
            return;
        }

        StringBuilder response = new StringBuilder("<html><head><title>Offline token servlet</title></head><body><pre>");
        RefreshableKeycloakSecurityContext ctx = (RefreshableKeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
        String accessTokenPretty = JsonSerialization.writeValueAsPrettyString(ctx.getToken());
        RefreshToken refreshToken;
        try {
            refreshToken = new JWSInput(ctx.getRefreshToken()).readJsonContent(RefreshToken.class);
        } catch (JWSInputException e) {
            throw new IOException(e);
        }
        String refreshTokenPretty = JsonSerialization.writeValueAsPrettyString(refreshToken);

        response = response.append("<span id=\"accessToken\">" + accessTokenPretty + "</span>")
                .append("<span id=\"refreshToken\">" + refreshTokenPretty + "</span>")
                .append("</pre></body></html>");
        resp.getWriter().println(response.toString());
    }
}

