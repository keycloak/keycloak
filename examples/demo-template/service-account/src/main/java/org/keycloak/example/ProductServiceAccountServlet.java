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

package org.keycloak.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.authentication.ClientCredentialsProviderUtils;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.common.util.UriUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ProductServiceAccountServlet extends HttpServlet {

    public static final String ERROR = "error";
    public static final String TOKEN = "token";
    public static final String TOKEN_PARSED = "idTokenParsed";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String PRODUCTS = "products";

    public static String getLoginUrl(HttpServletRequest request) {
        return "/service-account-portal/app/login";
    }

    public static String getLogoutUrl(HttpServletRequest request) {
        return "/service-account-portal/app/logout";
    }

    @Override
    public void init() throws ServletException {
        String adapterConfigLocation = "/WEB-INF/keycloak.json";
        InputStream config = getServletContext().getResourceAsStream(adapterConfigLocation);
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(config);
        getServletContext().setAttribute(KeycloakDeployment.class.getName(), deployment);

        HttpClient client = new DefaultHttpClient();
        getServletContext().setAttribute(HttpClient.class.getName(), client);
    }

    @Override
    public void destroy() {
        getHttpClient().getConnectionManager().shutdown();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String reqUri = req.getRequestURI();
        if (reqUri.endsWith("/login")) {
            serviceAccountLogin(req);
        } else if (reqUri.endsWith("/logout")){
            logout(req);
        }

        // Don't load products if some error happened during login,refresh or logout
        if (req.getAttribute(ERROR) == null) {
            loadProducts(req);
        }

        req.getRequestDispatcher("/WEB-INF/page.jsp").forward(req, resp);
    }

    private void serviceAccountLogin(HttpServletRequest req) {
        KeycloakDeployment deployment = getKeycloakDeployment();
        HttpClient client = getHttpClient();

        try {
            HttpPost post = new HttpPost(deployment.getTokenUrl());
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));

            // Add client credentials according to the method configured in keycloak-client-secret.json or keycloak-client-signed-jwt.json file
            Map<String, String> reqHeaders = new HashMap<>();
            Map<String, String> reqParams = new HashMap<>();
            ClientCredentialsProviderUtils.setClientCredentials(deployment, reqHeaders, reqParams);
            for (Map.Entry<String, String> header : reqHeaders.entrySet()) {
                post.setHeader(header.getKey(), header.getValue());
            }
            for (Map.Entry<String, String> param : reqParams.entrySet()) {
                formparams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            }

            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);

            HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            if (status != 200) {
                String json = getContent(entity);
                String error = "Service account login failed. Bad status: " + status + " response: " + json;
                req.setAttribute(ERROR, error);
            } else if (entity == null) {
                req.setAttribute(ERROR, "No entity");
            } else {
                String json = getContent(entity);
                AccessTokenResponse tokenResp = JsonSerialization.readValue(json, AccessTokenResponse.class);
                setTokens(req, deployment, tokenResp);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            req.setAttribute(ERROR, "Service account login failed. IOException occured. See server.log for details. Message is: " + ioe.getMessage());
        } catch (VerificationException vfe) {
            req.setAttribute(ERROR, "Service account login failed. Failed to verify token Message is: " + vfe.getMessage());
        }
    }

    private void setTokens(HttpServletRequest req, KeycloakDeployment deployment, AccessTokenResponse tokenResponse) throws IOException, VerificationException {
        String token = tokenResponse.getToken();
        String refreshToken = tokenResponse.getRefreshToken();
        AdapterTokenVerifier.VerifiedTokens parsedTokens = AdapterTokenVerifier.verifyTokens(token, tokenResponse.getIdToken(), deployment);
        AccessToken tokenParsed = parsedTokens.getAccessToken();
        req.getSession().setAttribute(TOKEN, token);
        req.getSession().setAttribute(REFRESH_TOKEN, refreshToken);
        req.getSession().setAttribute(TOKEN_PARSED, tokenParsed);
    }

    private void loadProducts(HttpServletRequest req) {
        HttpClient client = getHttpClient();
        String token = (String) req.getSession().getAttribute(TOKEN);

        String requestOrigin = UriUtils.getOrigin(req.getRequestURL().toString());
        HttpGet get = new HttpGet(requestOrigin + "/database/products");
        if (token != null) {
            get.addHeader("Authorization", "Bearer " + token);
        }
        try {
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                String json = getContent(entity);
                String error = "Failed retrieve products. Status: " + status;
                req.setAttribute(ERROR, error);
            } else if (entity == null) {
                req.setAttribute(ERROR, "No entity");
            } else {
                String products = getContent(entity);
                req.setAttribute(PRODUCTS, products);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            req.setAttribute(ERROR, "Failed retrieve products. IOException occured. See server.log for details. Message is: " + ioe.getMessage());
        }
    }

    private void logout(HttpServletRequest req) {
        KeycloakDeployment deployment = getKeycloakDeployment();
        String refreshToken = (String) req.getSession().getAttribute(REFRESH_TOKEN);
        if (refreshToken == null) {
            req.setAttribute(ERROR, "No refresh token available. Please login first");
        } else {
            try {
                ServerRequest.invokeLogout(deployment, refreshToken);
                req.getSession().removeAttribute(TOKEN);
                req.getSession().removeAttribute(REFRESH_TOKEN);
                req.getSession().removeAttribute(TOKEN_PARSED);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                req.setAttribute(ERROR, "Failed refresh token. See server.log for details. Message is: " + ioe.getMessage());
            } catch (ServerRequest.HttpFailure hfe) {
                hfe.printStackTrace();
                req.setAttribute(ERROR, "Failed refresh token. See server.log for details. Status was: " + hfe.getStatus() + ", Error is: " + hfe.getError());
            }
        }
    }

    private String getContent(HttpEntity entity) throws IOException {
        if (entity == null) return null;
        InputStream is = entity.getContent();
        return StreamUtil.readString(is);
    }

    private KeycloakDeployment getKeycloakDeployment() {
        return (KeycloakDeployment) getServletContext().getAttribute(KeycloakDeployment.class.getName());
    }

    private HttpClient getHttpClient() {
        return (HttpClient) getServletContext().getAttribute(HttpClient.class.getName());
    }
}
