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

package org.keycloak.testsuite.adapter;

import org.junit.Assert;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CustomerServlet extends HttpServlet {
    private static final String LINK = "<a href=\"%s\" id=\"%s\">%s</a>";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();
        if (req.getRequestURI().toString().endsWith("logout")) {
            resp.setStatus(200);
            pw.println("servlet logout ok");

            // Call logout before pw.flush
            req.logout();
            pw.flush();
            return;
        }
        KeycloakSecurityContext context = (KeycloakSecurityContext)req.getAttribute(KeycloakSecurityContext.class.getName());
        Client client = ClientBuilder.newClient();

        try {
            String appBase = System.getProperty("app.server.base.url", "http://localhost:8081");
            WebTarget target = client.target(appBase + "/customer-db/");
            Response response = target.request().get();
            Assert.assertEquals(401, response.getStatus());
            response.close();

            // Assert not possible to authenticate with refresh token
            RefreshableKeycloakSecurityContext refreshableContext = (RefreshableKeycloakSecurityContext) context;
            response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshableContext.getRefreshToken())
                    .get();
            Assert.assertEquals(401, response.getStatus());
            response.close();

            String html = target.request()
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + context.getTokenString())
                                .get(String.class);
            resp.setContentType("text/html");
            pw.println(html);
            pw.flush();
        } finally {
            client.close();
        }


    }
}
