package org.keycloak.testsuite.keycloaksaml;

import org.junit.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.List;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class SendUsernameServlet extends HttpServlet {

    public static Principal sentPrincipal;
    public static List<String> checkRoles;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doGet()");
        if (checkRoles != null) {
            for (String role : checkRoles) {
                System.out.println("check role: " + role);
                //Assert.assertTrue(req.isUserInRole(role));
                if (!req.isUserInRole(role)) {
                    resp.sendError(403);
                    return;
                }
            }

        }
        resp.setContentType("text/plain");
        OutputStream stream = resp.getOutputStream();
        Principal principal = req.getUserPrincipal();
        stream.write("request-path: ".getBytes());
        stream.write(req.getPathInfo().getBytes());
        stream.write("\n".getBytes());
        stream.write("principal=".getBytes());
        if (principal == null) {
            stream.write("null".getBytes());
            return;
        }
        String name = principal.getName();
        stream.write(name.getBytes());
        sentPrincipal = principal;

    }
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doPost()");
        if (checkRoles != null) {
            for (String role : checkRoles) {
                System.out.println("check role: " + role);
                Assert.assertTrue(req.isUserInRole(role));
            }

        }
        resp.setContentType("text/plain");
        OutputStream stream = resp.getOutputStream();
        Principal principal = req.getUserPrincipal();
        stream.write("request-path: ".getBytes());
        stream.write(req.getPathInfo().getBytes());
        stream.write("\n".getBytes());
        stream.write("principal=".getBytes());
        if (principal == null) {
            stream.write("null".getBytes());
            return;
        }
        String name = principal.getName();
        stream.write(name.getBytes());
        sentPrincipal = principal;
    }
}
