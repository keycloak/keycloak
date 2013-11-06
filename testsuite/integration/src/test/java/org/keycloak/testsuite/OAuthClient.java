/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.resteasy.security.PemUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.keycloak.RSATokenVerifier;
import org.keycloak.representations.SkeletonKeyToken;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OAuthClient {

    private WebDriver driver;

    private String baseUrl = Constants.AUTH_SERVER_ROOT + "/rest";

    private String realm = "test";

    private String responseType = "code";

    private String grantType = "authorization_code";

    private String clientId = "test-app";

    private String redirectUri = "http://localhost:8081/app/auth";

    private String scope;

    private String state;

    private PublicKey realmPublicKey;

    public OAuthClient(WebDriver driver) throws Exception {
        this.driver = driver;

        JSONObject realmJson = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/testrealm.json")));
        realmPublicKey = PemUtils.decodePublicKey(realmJson.getString("publicKey"));
    }

    public AuthorizationCodeResponse doLogin(String username, String password) {
        openLoginForm();

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();

        return new AuthorizationCodeResponse(this);
    }

    public AccessTokenResponse doAccessTokenRequest(String code, String password) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(getAccessTokenUrl());

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        if (grantType != null) {
            parameters.add(new BasicNameValuePair("grant_type", grantType));
        }
        if (code != null) {
            parameters.add(new BasicNameValuePair("code", code));
        }
        if (redirectUri != null) {
            parameters.add(new BasicNameValuePair("redirect_uri", redirectUri));
        }
        if (clientId != null) {
            parameters.add(new BasicNameValuePair("client_id", clientId));
        }
        if (password != null) {
            parameters.add(new BasicNameValuePair("password", password));
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, Charset.forName("UTF-8"));
        post.setEntity(formEntity);

        return new AccessTokenResponse(client.execute(post));
    }

    public SkeletonKeyToken verifyToken(String token) throws Exception {
        return RSATokenVerifier.verifyToken(token, realmPublicKey, realm);
    }

    public boolean isAuthorizationResponse() {
        return getCurrentRequest().equals(redirectUri) && getCurrentQuery().containsKey("code");
    }

    public String getState() {
        return state;
    }

    public String getClientId() {
        return clientId;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getCurrentRequest() {
        return driver.getCurrentUrl().substring(0, driver.getCurrentUrl().indexOf('?'));
    }

    public URI getCurrentUri() {
        try {
            return new URI(driver.getCurrentUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getCurrentQuery() {
        Map<String, String> m = new HashMap<String, String>();
        List<NameValuePair> pairs = URLEncodedUtils.parse(getCurrentUri(), "UTF-8");
        for (NameValuePair p : pairs) {
            m.put(p.getName(), p.getValue());
        }
        return m;
    }

    public void openLoginForm() {
        driver.navigate().to(getLoginFormUrl());
    }

    public void openLogout() {
        UriBuilder b = UriBuilder.fromUri(baseUrl + "/realms/" + realm + "/tokens/logout");
        if (redirectUri != null) {
            b.queryParam("redirect_uri", redirectUri);
        }
        driver.navigate().to(b.build().toString());
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public String getLoginFormUrl() {
        UriBuilder b = UriBuilder.fromUri(baseUrl + "/realms/" + realm + "/tokens/login");
        if (responseType != null) {
            b.queryParam("response_type", responseType);
        }
        if (clientId != null) {
            b.queryParam("client_id", clientId);
        }
        if (redirectUri != null) {
            b.queryParam("redirect_uri", redirectUri);
        }
        if (scope != null) {
            b.queryParam("scope", scope);
        }
        if (state != null) {
            b.queryParam("state", state);
        }
        return b.build().toString();
    }

    public String getAccessTokenUrl() {
        UriBuilder b = UriBuilder.fromUri(baseUrl + "/realms/" + realm + "/tokens/access/codes");
        return b.build().toString();
    }

    public OAuthClient realm(String realm) {
        this.realm = realm;
        return this;
    }

    public OAuthClient clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public OAuthClient redirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public OAuthClient responseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public OAuthClient scope(String scope) {
        this.scope = scope;
        return this;
    }

    public OAuthClient state(String state) {
        this.state = state;
        return this;
    }

    public String getRealm() {
        return realm;
    }

    public static class AuthorizationCodeResponse {

        private boolean isRedirected;
        private String code;
        private String state;
        private String error;

        public AuthorizationCodeResponse(OAuthClient client) {
            isRedirected = client.getCurrentRequest().equals(client.getRedirectUri());
            code = client.getCurrentQuery().get("code");
            state = client.getCurrentQuery().get("state");
            error = client.getCurrentQuery().get("error");
        }

        public boolean isRedirected() {
            return isRedirected;
        }

        public String getCode() {
            return code;
        }

        public String getState() {
            return state;
        }

        public String getError() {
            return error;
        }

    }

    public static class AccessTokenResponse {
        private int statusCode;

        private String accessToken;
        private String tokenType;
        private int expiresIn;
        private String refreshToken;

        private String error;

        public AccessTokenResponse(HttpResponse response) throws Exception {
            statusCode = response.getStatusLine().getStatusCode();
            if (!"application/json".equals(response.getHeaders("Content-Type")[0].getValue())) {
                Assert.fail("Invalid content type");
            }

            JSONObject responseJson = new JSONObject(IOUtils.toString(response.getEntity().getContent()));

            if (statusCode == 200) {
                accessToken = responseJson.getString("access_token");
                tokenType = responseJson.getString("token_type");
                expiresIn = responseJson.getInt("expires_in");

                if (responseJson.has("refresh_token")) {
                    refreshToken = responseJson.getString("refresh_token");
                }
            } else {
                error = responseJson.getString("error");
            }
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getError() {
            return error;
        }

        public int getExpiresIn() {
            return expiresIn;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }
    }

}
