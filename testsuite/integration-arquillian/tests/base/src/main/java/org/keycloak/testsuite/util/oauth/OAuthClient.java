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

package org.keycloak.testsuite.util.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.testsuite.pages.LoginPage;

import org.apache.http.impl.client.CloseableHttpClient;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class OAuthClient extends AbstractOAuthClient<OAuthClient> {

    public static String SERVER_ROOT;
    public static String AUTH_SERVER_ROOT;
    public static String APP_ROOT;
    public static String APP_AUTH_ROOT;

    static {
        updateURLs(getAuthServerContextRoot());
    }

    // Workaround, but many tests directly use system properties like OAuthClient.AUTH_SERVER_ROOT instead of taking the URL from suite context
    public static void updateURLs(String serverRoot) {
        SERVER_ROOT = removeDefaultPorts(serverRoot);
        AUTH_SERVER_ROOT = SERVER_ROOT + "/auth";
        updateAppRootRealm("master");
    }

    public static void updateAppRootRealm(String realm) {
        APP_ROOT = AUTH_SERVER_ROOT + "/realms/" + realm + "/app";
        APP_AUTH_ROOT = APP_ROOT + "/auth";
    }

    public static void resetAppRootRealm() {
        updateAppRootRealm("master");
    }

    public OAuthClient(CloseableHttpClient httpClient, WebDriver webDriver) {
        super(AUTH_SERVER_ROOT, httpClient, webDriver);
        init();
    }

    public OAuthClient newConfig() {
        OAuthClient newClient = new OAuthClient(httpClientManager.get(), driver);
        newClient.init();
        return newClient;
    }

    public void init() {
        config = new OAuthClientConfig()
                .realm("test")
                .client("test-app", "password")
                .redirectUri(APP_ROOT + "/auth")
                .postLogoutRedirectUri(APP_ROOT + "/auth")
                .responseType(OAuth2Constants.CODE);
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public void fillLoginForm(String username, String password) {
        LoginPage loginPage = new LoginPage();
        PageFactory.initElements(driver, loginPage);
        loginPage.login(username, password);
    }

    /**
     * @deprecated This method is deprecated, use {@link OAuthClient#client(String)} for public clients,
     * or {@link OAuthClient#client(String, String)} for confidential clients
     */
    @Deprecated
    public OAuthClient clientId(String clientId) {
        config.clientId(clientId);
        return this;
    }

    public WebDriver getDriver() {
        return driver;
    }

}
