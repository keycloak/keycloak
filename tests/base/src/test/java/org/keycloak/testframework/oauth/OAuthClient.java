/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testframework.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClientConfig;

import org.apache.http.impl.client.CloseableHttpClient;
import org.openqa.selenium.By;
import org.openqa.selenium.support.PageFactory;

/**
 * OAuth client to send OAuth request and handle callbacks.
 * Test-local shim keeps compatibility with legacy callers that expect {@code newConfig()}.
 */
public class OAuthClient extends AbstractOAuthClient<OAuthClient> {

    private final ManagedWebDriver managedWebDriver;
    private final ClientResource clientResource;

    public OAuthClient(String baseUrl, CloseableHttpClient httpClient, ManagedWebDriver managedWebDriver, ClientResource clientResource) {
        super(baseUrl, httpClient, managedWebDriver.driver());
        this.managedWebDriver = managedWebDriver;
        this.clientResource = clientResource;

        config = new OAuthClientConfig()
                .responseType(OAuth2Constants.CODE);
    }

    public OAuthClient(String baseUrl, CloseableHttpClient httpClient, ManagedWebDriver managedWebDriver) {
        this(baseUrl, httpClient, managedWebDriver, null);
    }

    public OAuthClient newConfig() {
        return new OAuthClient(baseUrl, httpClient().get(), managedWebDriver, clientResource);
    }

    @Override
    public void fillLoginForm(String username, String password) {
        LoginPage loginPage = new LoginPage(managedWebDriver);
        PageFactory.initElements(driver, loginPage);
        loginPage.fillLogin(username, password);
        loginPage.submit();
    }

    @Override
    public AuthorizationEndpointResponse parseLoginResponse() {
        if (config.getResponseMode() != null && config.getResponseMode().equals(OIDCResponseMode.FORM_POST.value())) {
            managedWebDriver.waiting().waitForOAuthCallback(webdriver1 -> webdriver1.findElement(By.id(OAuth2Constants.CODE)).isDisplayed() || webdriver1.findElement(By.id(OAuth2Constants.ERROR)).isDisplayed());
        } else if (config.getResponseMode() != null && config.getResponseMode().equals(OIDCResponseMode.FORM_POST_JWT.value())) {
            managedWebDriver.waiting().waitForOAuthCallback(webdriver1 -> webdriver1.findElement(By.id(OAuth2Constants.RESPONSE)).isDisplayed());
        } else {
            managedWebDriver.waiting().waitForOAuthCallback();
        }
        return super.parseLoginResponse();
    }

    public ClientRegistration clientRegistration() {
        return ClientRegistration.create().httpClient(httpClient().get()).url(baseUrl, config.getRealm()).build();
    }

    public ClientResource clientResource() {
        return clientResource;
    }

    public void close() {
        if (clientResource != null) {
            clientResource.remove();
        }
    }

}
