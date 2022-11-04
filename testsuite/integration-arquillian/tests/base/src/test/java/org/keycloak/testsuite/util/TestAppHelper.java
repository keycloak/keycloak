/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;

public class TestAppHelper {
    private OAuthClient oauth;
    private LoginPage loginPage;
    private AppPage appPage;
    private String refreshToken;

    public TestAppHelper(OAuthClient oauth, LoginPage loginPage, AppPage appPage) {
        this.oauth = oauth;
        this.loginPage = loginPage;
        this.appPage = appPage;
    }

    public boolean login(String username, String password) throws URISyntaxException, IOException {
        loginPage.open();
        loginPage.login(username, password);

        if (loginPage.isCurrent()) {
            return false;
        }

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        refreshToken = tokenResponse.getRefreshToken();

        return appPage.isCurrent();
    }

    public boolean logout() {
        try (CloseableHttpResponse response = oauth.doLogout(refreshToken, "password")) {
            return response.getStatusLine().getStatusCode() == Response.Status.NO_CONTENT.getStatusCode();
        } catch (IOException e) {
            return false;
        }
    }

}
