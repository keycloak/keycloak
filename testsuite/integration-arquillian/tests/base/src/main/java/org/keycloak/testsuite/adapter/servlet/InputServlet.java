package org.keycloak.testsuite.adapter.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.annotation.WebServlet;

/**
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
@WebServlet("/input-portal")
public class InputServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String appBase = System.getProperty("app.server.base.url", "http://localhost:8280");
        String actionUrl = appBase + "/input-portal/secured/post";


        resp.setContentType("text/html");
        PrintWriter pw = resp.getWriter();
        pw.printf("<html><head><title>%s</title></head><body>", "Input Page");
        pw.printf("<form action=\"%s\" method=\"POST\">", actionUrl);
        pw.println("<input id=\"parameter\" type=\"text\" name=\"parameter\">");
        pw.println("<input name=\"submit\" type=\"submit\" value=\"Submit\"></form>");
        pw.print("</body></html>");
        pw.flush();


    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.printf("parameter="+req.getParameter("parameter"));
        pw.flush();
    }

}
