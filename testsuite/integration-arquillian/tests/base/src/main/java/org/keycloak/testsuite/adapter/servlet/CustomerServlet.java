package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.KeycloakSecurityContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.annotation.WebServlet;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@WebServlet("/customer-portal")
public class CustomerServlet extends HttpServlet {
    private static final String LINK = "<a href=\"%s\" id=\"%s\">%s</a>";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();
        if (req.getRequestURI().endsWith("logout")) {
            resp.setStatus(200);
            pw.println("servlet logout ok");

            // Call logout before pw.flush
            req.logout();
            pw.flush();
            return;
        }
        KeycloakSecurityContext context = (KeycloakSecurityContext)req.getAttribute(KeycloakSecurityContext.class.getName());
        Client client = ClientBuilder.newClient();

        try {
            String appBase = System.getProperty("app.server.base.url", "http://localhost:8280");
            WebTarget target = client.target(appBase + "/customer-db/");
            Response response = target.request().get();
            if (response.getStatus() != 401) { // assert response status == 401
                throw new AssertionError("Response status code is not 401.");
            }
            response.close();
            String html = target.request()
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + context.getTokenString())
                                .get(String.class);
            resp.setContentType("text/html");
            pw.println(html);
            pw.flush();
        } finally {
            client.close();
        }
    }
}
