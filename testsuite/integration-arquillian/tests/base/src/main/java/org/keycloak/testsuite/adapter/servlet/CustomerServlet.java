package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.KeycloakSecurityContext;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

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
        KeycloakSecurityContext context = (KeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());

        //try {
        StringBuilder result = new StringBuilder();
        URL url = new URL(System.getProperty("app.server.base.url", "http://localhost:8280") + "/customer-db/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + context.getTokenString());
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        resp.setContentType("text/html");
        pw.println(result.toString());
        pw.flush();
//
//            Response response = target.request().get();
//            if (response.getStatus() != 401) { // assert response status == 401
//                throw new AssertionError("Response status code is not 401.");
//            }
//            response.close();
//            String html = target.request()
//                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + context.getTokenString())
//                                .get(String.class);
//            pw.println(html);
//            pw.flush();
//        } finally {
//            client.close();
//        }
    }
}
