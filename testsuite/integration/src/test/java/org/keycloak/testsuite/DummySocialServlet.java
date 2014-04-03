package org.keycloak.testsuite;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.keycloak.OAuth2Constants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class DummySocialServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();
        pw.print("<html>");
        pw.print("<body>");
        pw.print("<form method=\"post\">");
        pw.print("<label for=\"id\">ID</label><input type=\"text\" id=\"id\" name=\"id\" />");
        pw.print("<label for=\"username\">Username</label><input type=\"text\" id=\"username\" name=\"username\" />");
        pw.print("<label for=\"firstname\">First Name</label><input type=\"text\" id=\"firstname\" name=\"firstname\" />");
        pw.print("<label for=\"lastname\">Last Name</label><input type=\"text\" id=\"lastname\" name=\"lastname\" />");
        pw.print("<label for=\"email\">Email</label><input type=\"text\" id=\"email\" name=\"email\" />");
        pw.print("<input type=\"submit\" id=\"login\" name=\"login\" value=\"login\" />");
        pw.print("<input type=\"submit\" id=\"cancel\" name=\"cancel\" value=\"cancel\" />");
        pw.print("</form>");
        pw.print("</body>");
        pw.print("</html>");
        pw.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String state = null;
        String redirectUri = null;

        List<NameValuePair> query = null;
        try {
            URI uri = URI.create(req.getRequestURL().append('?').append(req.getQueryString()).toString());
            query = URLEncodedUtils.parse(uri, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (NameValuePair p : query) {
            if (OAuth2Constants.STATE.equals(p.getName())) {
                state = p.getValue();
            } else if (OAuth2Constants.REDIRECT_URI.equals(p.getName())) {
                redirectUri = p.getValue();
            }
        }

        String redirect;
        if (req.getParameter("login") != null) {
            redirect = redirectUri + "?id=" + req.getParameter("id") + "&username=" + req.getParameter("username") + "&state=" + state + "&code=" + UUID.randomUUID().toString();
            if (req.getParameter("firstname") != null) {
                redirect += "&firstname=" + req.getParameter("firstname");
            }
            if (req.getParameter("lastname") != null) {
                redirect += "&lastname=" + req.getParameter("lastname");
            }
            if (req.getParameter("email") != null) {
                redirect += "&email=" + req.getParameter("email");
            }
        } else {
            redirect = redirectUri + "?error=access_denied&state=" + state;
        }

        resp.sendRedirect(redirect);
    }

}
