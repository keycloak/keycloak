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

package org.keycloak.testsuite.keycloaksaml;

import org.junit.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class InputServlet extends HttpServlet {

    private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String appBase = System.getProperty("app.server.base.url", "http://localhost:8081");
        if (req.getRequestURI().endsWith("insecure")) {
            if (System.getProperty("insecure.user.principal.unsupported") == null) Assert.assertNotNull(req.getUserPrincipal());
            resp.setContentType("text/html");
            PrintWriter pw = resp.getWriter();
            pw.printf("<html><head><title>%s</title></head><body>", "Insecure Page");
            if (req.getUserPrincipal() != null) pw.printf("UserPrincipal: " + req.getUserPrincipal().getName());
            pw.print("</body></html>");
            pw.flush();
            return;
        }
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
        if (!FORM_URLENCODED.equals(req.getContentType())) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter pw = resp.getWriter();
            resp.setContentType("text/plain");
            pw.printf("Expecting content type " + FORM_URLENCODED +
                    ", received " + req.getContentType() + " instead");
            pw.flush();
            return;
        }
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.printf("parameter="+req.getParameter("parameter"));
        pw.flush();
    }

}
