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

import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

public class TestAppHelper {
    private OAuthClient oauth;
    private LoginPage loginPage;
    private LoginTotpPage loginTotpPage;

    private String refreshToken;

    public TestAppHelper(OAuthClient oauth, LoginPage loginPage) {
        this.oauth = oauth;
        this.loginPage = loginPage;
    }
    public TestAppHelper(OAuthClient oauth, LoginPage loginPage, LoginTotpPage loginTotpPage) {
        this.oauth = oauth;
        this.loginPage = loginPage;
        this.loginTotpPage = loginTotpPage;
    }

    public void login(String username, String password) {
        startLogin(username, password);
        completeLogin();
    }

    public void startLogin(String username, String password) {
        oauth.openLoginForm();
        loginPage.login(username, password);
    }

    public void completeLogin() {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        refreshToken = tokenResponse.getRefreshToken();
    }

    public void login(String username, String password, String otp) {
        startLogin(username, password);
        loginTotpPage.login(otp);
        completeLogin();
    }

    public void login(String username, String password, String realm, String clientId, String idp) {
        oauth.client(clientId);
        oauth.realm(realm);
        oauth.openLoginForm();
        loginPage.clickSocial(idp);
        loginPage.login(username, password);
        completeLogin();
    }

    public boolean logout() {
        try {
            return oauth.doLogout(refreshToken).isSuccess();
        } catch (RuntimeException e) {
            return false;
        }
    }

}
