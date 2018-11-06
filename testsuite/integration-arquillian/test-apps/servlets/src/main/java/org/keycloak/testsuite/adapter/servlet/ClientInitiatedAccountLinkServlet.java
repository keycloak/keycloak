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
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.representations.AccessToken;

import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@WebServlet("/client-linking")
public class ClientInitiatedAccountLinkServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-cache");
        if (request.getRequestURI().endsWith("/link") && request.getParameter("response") == null) {
            String provider = request.getParameter("provider");
            String realm = request.getParameter("realm");
            KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
            AccessToken token = session.getToken();
            String clientId = token.getIssuedFor();
            String nonce = UUID.randomUUID().toString();
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            String input = nonce + token.getSessionState() + clientId + provider;
            byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String hash = Base64Url.encode(check);
            request.getSession().setAttribute("hash", hash);
            String redirectUri = KeycloakUriBuilder.fromUri(request.getRequestURL().toString())
                    .replaceQuery(null)
                    .queryParam("response", "true").build().toString();
            String accountLinkUrl = KeycloakUriBuilder.fromUri(ServletTestUtils.getAuthServerUrlBase())
                    .path("/auth/realms/{realm}/broker/{provider}/link")
                    .queryParam("nonce", nonce)
                    .queryParam("hash", hash)
                    .queryParam("client_id", token.getIssuedFor())
                    .queryParam("redirect_uri", redirectUri).build(realm, provider).toString();
            resp.setStatus(302);
            resp.setHeader("Location", accountLinkUrl);
        } else if (request.getRequestURI().endsWith("/link") && request.getParameter("response") != null) {
            resp.setStatus(200);
            resp.setContentType("text/html");
            PrintWriter pw = resp.getWriter();
            pw.printf("<html><head><title>%s</title></head><body>", "Client Linking");
            String error = request.getParameter("link_error");
            if (error != null) {
                pw.println("Link error: " + error);
            } else {
                pw.println("Account Linked");
            }
            pw.print("</body></html>");
            pw.flush();
        } else {
            resp.setStatus(200);
            resp.setContentType("text/html");
            PrintWriter pw = resp.getWriter();
            pw.printf("<html><head><title>%s</title></head><body>", "Client Linking");
            pw.println("Unknown request: " + request.getRequestURL().toString());
            pw.print("</body></html>");
            pw.flush();

        }

    }
}
