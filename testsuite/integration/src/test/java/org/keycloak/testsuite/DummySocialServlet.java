package org.keycloak.testsuite;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

public class DummySocialServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();
        pw.print("<html>");
        pw.print("<body>");
        pw.print("<form method=\"post\">");
        pw.print("<label for=\"username\">Username</label><input type=\"text\" id=\"username\" name=\"username\" />");
        pw.print("<input type=\"submit\" id=\"submit\" value=\"login\" />");
        pw.print("</form>");
        pw.print("</body>");
        pw.print("</html>");
        pw.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String accessToken = req.getParameter("username");
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
            if ("state".equals(p.getName())) {
                state = p.getValue();
            } else if ("redirect_uri".equals(p.getName())) {
                redirectUri = p.getValue();
            }
        }

        String redirect = redirectUri + "?access_token=" + accessToken + "&token_type=bearer&state=" + state;
        resp.sendRedirect(redirect);
    }

}
