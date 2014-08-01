package org.keycloak.testsuite.adapter;

import org.junit.Assert;
import org.keycloak.KeycloakSecurityContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CallAuthenticatedServlet extends HttpServlet {
    private static final String LINK = "<a href=\"%s\" id=\"%s\">%s</a>";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.authenticate(resp)) {
            return;
        }

        KeycloakSecurityContext sc = (KeycloakSecurityContext)req.getAttribute(KeycloakSecurityContext.class.getName());
        Assert.assertNotNull(sc);
        resp.setContentType("text/html");
        PrintWriter pw = resp.getWriter();
        pw.printf("<html><head><title>%s</title></head><body>", "Customer Portal");
        pw.println("Stian Thorgersen");
        pw.println("Bill Burke");
        pw.print("</body></html>");
        pw.flush();



    }
}
