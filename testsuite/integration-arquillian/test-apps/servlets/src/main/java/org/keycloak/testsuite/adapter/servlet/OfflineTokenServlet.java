package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.OAuth2Constants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineTokenServlet extends AbstractShowTokensServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if (req.getRequestURI().endsWith("logout")) {

            UriBuilder redirectUriBuilder = UriBuilder.fromUri(ServletTestUtils.getUrlBase() + "/offline-client");
            if (req.getParameter(OAuth2Constants.SCOPE) != null) {
                redirectUriBuilder.queryParam(OAuth2Constants.SCOPE, req.getParameter(OAuth2Constants.SCOPE));
            }
            String redirectUri = redirectUriBuilder.build().toString();

            String serverLogoutRedirect = UriBuilder.fromUri(ServletTestUtils.getAuthServerUrlBase() + "/auth/realms/test/protocol/openid-connect/logout")
                    .queryParam("redirect_uri", redirectUri)
                    .build().toString();

            resp.sendRedirect(serverLogoutRedirect);
            return;
        }

        StringBuilder response = new StringBuilder("<html><head><title>Offline token servlet</title></head><body><pre>");

        String tokens = renderTokens(req);
        response = response.append(tokens);

        response.append("</pre></body></html>");
        resp.getWriter().println(response.toString());
    }
}

