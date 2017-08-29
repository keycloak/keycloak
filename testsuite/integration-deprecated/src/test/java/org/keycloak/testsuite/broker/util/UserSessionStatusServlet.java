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
package org.keycloak.testsuite.broker.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * @author pedroigor
 */
public class UserSessionStatusServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().toString().endsWith("logout")) {
            String redirect = UriBuilder.fromUri("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/logout")
                    .queryParam("redirect_uri", "http://localhost:8081/test-app").build().toString();
            resp.sendRedirect(redirect);
            //resp.setStatus(200);
            //req.logout();
            return;
        }

        writeSessionStatus(req, resp);
    }

    private void writeSessionStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        KeycloakSecurityContext context = (KeycloakSecurityContext)req.getAttribute(KeycloakSecurityContext.class.getName());
        IDToken idToken = context.getIdToken();
        AccessToken accessToken = context.getToken();
        JsonNode jsonNode = new ObjectMapper().valueToTree(new UserSessionStatus(idToken, accessToken, context.getTokenString()));
        PrintWriter writer = resp.getWriter();

        writer.println(jsonNode.toString());

        writer.flush();
    }

    public static class UserSessionStatus implements Serializable {

        private String accessTokenString;
        private AccessToken accessToken;
        private IDToken idToken;

        public UserSessionStatus() {

        }

        public UserSessionStatus(IDToken idToken, AccessToken accessToken, String tokenString) {
            this.idToken = idToken;
            this.accessToken = accessToken;
            this.accessTokenString = tokenString;
        }

        public IDToken getIdToken() {
            return this.idToken;
        }

        public void setIdToken(IDToken idToken) {
            this.idToken = idToken;
        }

        public AccessToken getAccessToken() {
            return this.accessToken;
        }

        public void setAccessToken(AccessToken accessToken) {
            this.accessToken = accessToken;
        }

        public String getAccessTokenString() {
            return this.accessTokenString;
        }
    }
}
