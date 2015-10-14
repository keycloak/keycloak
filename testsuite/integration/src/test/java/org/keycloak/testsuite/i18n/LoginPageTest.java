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

        loginPage.openLanguage("German");
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
