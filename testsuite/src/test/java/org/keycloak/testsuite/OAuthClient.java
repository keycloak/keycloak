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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OAuthClient {

    private WebDriver driver;

    private String baseUrl = "http://localhost:8081/auth-server/rest";

    private String realm = "test";

    private String responseType = "code";

    private String clientId = "test-app";

    private String redirectUri = "http://localhost:8081/app/auth";

    private String scope;

    private String state;

    public OAuthClient(WebDriver driver) {
        this.driver = driver;
    }

    // public void login(String username, String password) throws UnsupportedEncodingException {
    // HttpClient client = new DefaultHttpClient();
    // HttpPost post = new HttpPost(getLoginFormUrl());
    //
    // List<NameValuePair> parameters = new LinkedList<NameValuePair>();
    // parameters.add(new BasicNameValuePair("username", username));
    // parameters.add(new BasicNameValuePair("password", password));
    //
    // UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, Charset.forName("UTF-8"));
    // post.setEntity(formEntity);
    // }

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
        b.queryParam("response_type", responseType);
        b.queryParam("client_id", clientId);
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

}
