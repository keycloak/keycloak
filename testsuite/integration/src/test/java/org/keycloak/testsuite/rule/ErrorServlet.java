package org.keycloak.testsuite.rule;

import org.keycloak.adapters.spi.AuthenticationError;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ErrorServlet extends HttpServlet {
    public static AuthenticationError authError;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        authError = (AuthenticationError)req.getAttribute(AuthenticationError.class.getName());

        Integer statusCode = (Integer) req.getAttribute("javax.servlet.error.status_code");

        resp.setContentType("text/html");
        PrintWriter pw = resp.getWriter();
        pw.printf("<html><head><title>%s</title></head><body>", "Error Page");
        pw.print("<h1>There was an error</h1>");
        if (statusCode != null)
            pw.print("<br/>HTTP status code: " + statusCode);
        if (authError != null)
            pw.print("<br/>Error info: " + authError.toString());
        pw.print("</body></html>");
        pw.flush();


    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
