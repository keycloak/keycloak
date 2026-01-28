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

import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;


public class TestAppHelper {
    private OAuthClient oauth;
    private LoginPage loginPage;
    private LoginTotpPage loginTotpPage;
    private AppPage appPage;
    private String refreshToken;

    public TestAppHelper(OAuthClient oauth, LoginPage loginPage, AppPage appPage) {
        this.oauth = oauth;
        this.loginPage = loginPage;
        this.appPage = appPage;
    }
    public TestAppHelper(OAuthClient oauth, LoginPage loginPage, LoginTotpPage loginTotpPage, AppPage appPage) {
        this.oauth = oauth;
        this.loginPage = loginPage;
        this.loginTotpPage = loginTotpPage;
        this.appPage = appPage;
    }

    public boolean login(String username, String password) {
        startLogin(username, password);

        if (loginPage.isCurrent()) {
            return false;
        }

        completeLogin();

        return appPage.isCurrent();
    }

    public boolean startLogin(String username, String password) {
        loginPage.open();
        loginPage.login(username, password);

        return appPage.isCurrent();
    }

    public void completeLogin() {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        refreshToken = tokenResponse.getRefreshToken();
    }

    public boolean login(String username, String password, String otp) {
        startLogin(username, password);

        loginTotpPage.login(otp);
        if (loginTotpPage.isCurrent()) {
            return false;
        }

        completeLogin();

        return appPage.isCurrent();
    }

    public boolean login(String username, String password, String realm, String clientId, String idp) {
        oauth.clientId(clientId);
        loginPage.open(realm);
        loginPage.clickSocial(idp);
        loginPage.login(username, password);

        if (loginPage.isCurrent(realm)) {
            return false;
        }

        completeLogin();

        return appPage.isCurrent();
    }

    public boolean logout() {
        try {
            return oauth.doLogout(refreshToken).isSuccess();
        } catch (RuntimeException e) {
            return false;
        }
    }

}
