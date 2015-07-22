package org.keycloak.testsuite.federation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.IDToken;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPExampleServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
        IDToken idToken = securityContext.getIdToken();

        PrintWriter out = resp.getWriter();
        out.println("<html><head><title>LDAP Portal</title></head><body>");
        out.println("<table border><tr><th>Attribute name</th><th>Attribute values</th></tr>");

        out.printf("<tr><td>%s</td><td>%s</td></tr>", "preferred_username", idToken.getPreferredUsername());
        out.println();
        out.printf("<tr><td>%s</td><td>%s</td></tr>", "name", idToken.getName());
        out.println();
        out.printf("<tr><td>%s</td><td>%s</td></tr>", "email", idToken.getEmail());
        out.println();

        for (Map.Entry<String, Object> claim : idToken.getOtherClaims().entrySet()) {
            String value = claim.getValue().toString();
            out.printf("<tr><td>%s</td><td>%s</td></tr>", claim.getKey(), value);
            out.println();
        }

        out.println("</table></body></html>");
        out.flush();
    }

}
