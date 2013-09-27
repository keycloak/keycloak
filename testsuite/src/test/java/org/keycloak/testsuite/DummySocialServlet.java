package org.keycloak.testsuite;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

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

        List<NameValuePair> query = URLEncodedUtils.parse(req.getQueryString(), Charset.forName("UTF-8"));
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
