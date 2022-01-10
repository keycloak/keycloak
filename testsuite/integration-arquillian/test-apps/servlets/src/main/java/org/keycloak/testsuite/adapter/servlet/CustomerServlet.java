/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        try (PrintWriter pw = resp.getWriter()) {
            KeycloakSecurityContext context = (KeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
            if (req.getRequestURI().endsWith("logout")) {
                resp.setStatus(200);
                pw.println("<html><body>");
                pw.println("<div id=\"customer_portal_logout\">servlet logout ok</div>");
                pw.println("</body></html>");

                //Clear principal form database-service by calling logout
                StringBuilder result = new StringBuilder();
                String urlBase = ServletTestUtils.getUrlBase();

                URL url = new URL(urlBase + "/customer-db/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + context.getTokenString());
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                rd.close();
                pw.println(result.toString());
                // Call logout before pw.flush
                req.logout();
                pw.flush();
                return;
            }

            String urlBase = ServletTestUtils.getUrlBase();

            // Decide what to call based on the URL suffix
            String serviceUrl;
            if (req.getRequestURI().endsWith("/call-customer-db-audience-required")) {
                serviceUrl = urlBase + "/customer-db-audience-required/";
            } else {
                serviceUrl = urlBase + "/customer-db/";
            }

            String result = invokeService(serviceUrl, context);

            resp.setContentType("text/html");
            pw.println(result);
            pw.flush();
        }
    }

    private String invokeService(String serviceUrl, KeycloakSecurityContext context) throws IOException {
        StringBuilder result = new StringBuilder();

        URL url = new URL(serviceUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + context.getTokenString());

        if (conn.getResponseCode() != 200) {
            conn.getErrorStream().close();
            return "Service returned: " + conn.getResponseCode();
        }

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();

        return result.toString();
    }

}
