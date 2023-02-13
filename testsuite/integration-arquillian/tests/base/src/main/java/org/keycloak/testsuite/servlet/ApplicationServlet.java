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
package org.keycloak.testsuite.servlet;

import org.keycloak.services.resources.RealmsResource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ApplicationServlet extends HttpServlet {

    private static final String LINK = "<a href=\"%s\" id=\"%s\">%s</a>";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String title;
        if (req.getRequestURI().endsWith("auth")) {
            title = "AUTH_RESPONSE";
        } else if (req.getRequestURI().endsWith("logout")) {
            title = "LOGOUT_REQUEST";
        } else {
            title = "APP_REQUEST";
        }

        PrintWriter pw = resp.getWriter();
        pw.printf("<html><head><title>%s</title></head><body>", title);
        UriBuilder base = UriBuilder.fromUri("/auth");
        pw.printf(LINK, RealmsResource.accountUrl(base).build("test"), "account", "account");

        pw.print("</body></html>");
        pw.flush();
    }

}
