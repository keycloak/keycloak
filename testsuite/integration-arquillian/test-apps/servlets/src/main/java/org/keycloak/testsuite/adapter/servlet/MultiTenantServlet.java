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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
public class MultiTenantServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        String realm = req.getPathInfo().split("/")[1];
        if (realm.contains("?")) {
            realm = realm.split("\\?")[0];
        }
        
        if (req.getPathInfo() != null && req.getPathInfo().contains("logout")) {
            req.logout();
            resp.sendRedirect(req.getContextPath() + "/" + realm);
            return;
        }
        
        resp.setContentType("text/html");
        PrintWriter pw = resp.getWriter();
        KeycloakSecurityContext context = (KeycloakSecurityContext)req.getAttribute(KeycloakSecurityContext.class.getName());

        pw.print("Username: ");
        pw.println(context.getIdToken().getPreferredUsername());

        pw.print("<br/>Realm: ");
        pw.println(context.getRealm());

        pw.flush();
    }
}
