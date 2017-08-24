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

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@WebServlet("/client-linking")
public class LinkAndExchangeServlet extends HttpServlet {
    public AccessTokenResponse doTokenExchange(String realm, String token, String requestedIssuer,
                                               String clientId, String clientSecret) throws Exception {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            String exchangeUrl = KeycloakUriBuilder.fromUri(ServletTestUtils.getAuthServerUrlBase())
                    .path("/auth/realms/{realm}/protocol/openid-connect/token").build(realm).toString();

            HttpPost post = new HttpPost(exchangeUrl);

            List<NameValuePair> parameters = new LinkedList<NameValuePair>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.SUBJECT_TOKEN, token));
            parameters.add(new BasicNameValuePair(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.REQUESTED_ISSUER, requestedIssuer));

            if (clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
                post.setHeader("Authorization", authorization);
            } else {
                parameters.add(new BasicNameValuePair("client_id", clientId));

            }

            UrlEncodedFormEntity formEntity;
            try {
                formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            post.setEntity(formEntity);
            CloseableHttpResponse response = client.execute(post);
            AccessTokenResponse tokenResponse = JsonSerialization.readValue(response.getEntity().getContent(), AccessTokenResponse.class);
            response.close();
            return tokenResponse;
        } finally {
            client.close();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-cache");
        if (request.getRequestURI().endsWith("/link") && request.getParameter("response") == null) {
            String provider = request.getParameter("provider");
            String realm = request.getParameter("realm");
            KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
            AccessToken token = session.getToken();
            String tokenString = session.getTokenString();

            String clientId = token.getAudience()[0];
            String linkUrl = null;
            try {
                AccessTokenResponse response = doTokenExchange(realm, tokenString, provider,  clientId, "password");
                String error = (String)response.getOtherClaims().get("error");
                if (error != null) {
                    System.out.println("*** error : " + error);
                    System.out.println("*** link-url: " + response.getOtherClaims().get("account-link-url"));
                    linkUrl = (String)response.getOtherClaims().get("account-link-url");
                } else {
                    Assert.assertNotNull(response.getToken());
                    resp.setStatus(200);
                    resp.setContentType("text/html");
                    PrintWriter pw = resp.getWriter();
                    pw.printf("<html><head><title>%s</title></head><body>", "Client Linking");
                    pw.println("Account Linked");
                    pw.print("</body></html>");
                    pw.flush();
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String redirectUri = KeycloakUriBuilder.fromUri(request.getRequestURL().toString())
                    .replaceQuery(null)
                    .queryParam("response", "true").build().toString();
            String accountLinkUrl = KeycloakUriBuilder.fromUri(linkUrl)
                    .queryParam("redirect_uri", redirectUri).build().toString();
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
