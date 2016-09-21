package org.keycloak.testsuite.adapter.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author mhajas
 */
@WebServlet("/basic-auth")
public class BasicAuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String value = req.getParameter("value");
        System.out.println("In BasicAuthServlet with value: " + value);

        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.printf(value);
        pw.flush();
    }

}
