/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.example.multitenant.boundary;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.util.HostUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.UriUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
@WebServlet(urlPatterns = "/*")
public class ProtectedServlet extends HttpServlet {

    static class TypedList extends ArrayList<RoleRepresentation> {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String realm = req.getPathInfo().split("/")[1];
        if (realm.contains("?")) {
            realm = realm.split("\\?")[0];
        }

        if (req.getPathInfo().contains("logout")) {
            req.logout();
            resp.sendRedirect(req.getContextPath() + "/" + realm);
            return;
        }

        KeycloakPrincipal principal = (KeycloakPrincipal) req.getUserPrincipal();

        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();

        writer.write("Realm: ");
        writer.write(principal.getKeycloakSecurityContext().getRealm());

        writer.write("<br/>User: ");
        writer.write(principal.getKeycloakSecurityContext().getIdToken().getPreferredUsername());

        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet get = new HttpGet(getBaseUrl(req) + "/auth/admin/realms/" + principal.getKeycloakSecurityContext().getRealm() + "/roles" );
            get.addHeader("Authorization", "Bearer " + principal.getKeycloakSecurityContext().getTokenString());
            try {
                HttpResponse response = client.execute(get);
                int status = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity(); 
                if (status != 200) {
                    writer.write("<br/>Admin REST api Failure");
                } else {
                    InputStream is = entity.getContent();
                    try {
                        TypedList roleList = JsonSerialization.readValue(is, TypedList.class);
                        for (RoleRepresentation role : roleList) {
                            writer.write("<br/>Role: ");
                            writer.write(role.getName());
                        }                
                    } finally {
                        is.close();
                    } 
                }
            } catch (IOException e) {
                writer.write("<br/>IOException");
            }
        } finally {
            client.getConnectionManager().shutdown();
        }

        writer.write(String.format("<br/><a href=\"/multitenant/%s/logout\">Logout</a>", realm));
    }

    public static String getBaseUrl(HttpServletRequest request) {
        String useHostname = request.getServletContext().getInitParameter("useHostname");
        if (useHostname != null && "true".equalsIgnoreCase(useHostname)) {
            return "http://" + HostUtils.getHostName() + ":8080";
        } else {
            return UriUtils.getOrigin(request.getRequestURL().toString());
        }
    }
 }
