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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.HttpClientBuilder;
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
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@WebServlet("/exchange-linking")
public class LinkAndExchangeServlet extends HttpServlet {

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public AccessTokenResponse doTokenExchange(String realm, String token, String requestedIssuer,
                                               String clientId, String clientSecret) throws Exception {

        try (CloseableHttpClient client = (CloseableHttpClient) new HttpClientBuilder().disableTrustManager().build()) {
            String exchangeUrl = KeycloakUriBuilder.fromUri(ServletTestUtils.getAuthServerUrlBase())
                    .path("/auth/realms/{realm}/protocol/openid-connect/token").build(realm).toString();

            HttpPost post = new HttpPost(exchangeUrl);
            HashMap<String, String> parameters = new HashMap<>();

            if (clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
                post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.toString());
                post.setHeader(HttpHeaders.AUTHORIZATION, authorization);
            } else {
                parameters.put("client_id", clientId);
            }

            parameters.put(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);
            parameters.put(OAuth2Constants.SUBJECT_TOKEN, token);
            parameters.put(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);
            parameters.put(OAuth2Constants.REQUESTED_ISSUER, requestedIssuer);

            post.setEntity(new StringEntity(getPostDataString(parameters)));
            HttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200 || statusCode == 400) {
                return JsonSerialization.readValue(EntityUtils.toString(response.getEntity()), AccessTokenResponse.class);
            } else {
                throw new RuntimeException("Unknown error!");
            }
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

            String clientId = token.getIssuedFor();
            String linkUrl = null;
            try {
                AccessTokenResponse response = doTokenExchange(realm, tokenString, provider,  clientId, "password");
                String error = response.getError();
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
                    .queryParam("response", "true")
                    .queryParam("realm", realm)
                    .queryParam("provider", provider).build().toString();
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
            pw.println("trying exchange");
            try {
                String provider = request.getParameter("provider");
                String realm = request.getParameter("realm");
                KeycloakSecurityContext session = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
                AccessToken token = session.getToken();
                String clientId = token.getIssuedFor();
                String tokenString = session.getTokenString();
                AccessTokenResponse response = doTokenExchange(realm, tokenString, provider,  clientId, "password");
                error = (String)response.getOtherClaims().get("error");
                if (error == null) {
                    if (response.getToken() != null) pw.println("Exchange token received");
                } else {
                    pw.print("Error with exchange: " + error);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
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
