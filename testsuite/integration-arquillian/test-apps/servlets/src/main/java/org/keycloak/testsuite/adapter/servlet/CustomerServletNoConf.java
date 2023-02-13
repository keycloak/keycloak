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
@WebServlet("/customer-portal-noconf")
public class CustomerServletNoConf extends HttpServlet {
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
        String urlBase = ServletTestUtils.getUrlBase();

        URL url = new URL(urlBase + "/customer-db/");
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
