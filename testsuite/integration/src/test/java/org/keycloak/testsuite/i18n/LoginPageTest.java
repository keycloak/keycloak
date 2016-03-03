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
package org.keycloak.testsuite.i18n;

import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 */
public class LoginPageTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {

        }
    });

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected LoginPage loginPage;

    @Test
    public void languageDropdown() {
        loginPage.open();
        Assert.assertEquals("English", loginPage.getLanguageDropdownText());

        loginPage.openLanguage("Deutsch");
        Assert.assertEquals("Deutsch", loginPage.getLanguageDropdownText());

        loginPage.openLanguage("English");
        Assert.assertEquals("English", loginPage.getLanguageDropdownText());
    }

    @Test
    public void uiLocalesParameter() {
        loginPage.open();
        Assert.assertEquals("English", loginPage.getLanguageDropdownText());

        //test if cookie works
        oauth.uiLocales("de");
        loginPage.open();
        Assert.assertEquals("Deutsch", loginPage.getLanguageDropdownText());

        driver.manage().deleteAllCookies();
        loginPage.open();
        Assert.assertEquals("Deutsch", loginPage.getLanguageDropdownText());

        oauth.uiLocales("en de");
        driver.manage().deleteAllCookies();
        loginPage.open();
        Assert.assertEquals("English", loginPage.getLanguageDropdownText());

        oauth.uiLocales("fr de");
        driver.manage().deleteAllCookies();
        loginPage.open();
        Assert.assertEquals("Deutsch", loginPage.getLanguageDropdownText());
    }

    @Test
    public void acceptLanguageHeader() {
        DefaultHttpClient httpClient = (DefaultHttpClient) new HttpClientBuilder().build();
        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();

        loginPage.open();
        Response response = client.target(driver.getCurrentUrl()).request().acceptLanguage("de").get();
        Assert.assertTrue(response.readEntity(String.class).contains("Anmeldung bei test"));

        response = client.target(driver.getCurrentUrl()).request().acceptLanguage("en").get();
        Assert.assertTrue(response.readEntity(String.class).contains("Log in to test"));
    }
}
