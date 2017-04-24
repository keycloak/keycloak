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
import org.junit.Test;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.pages.LoginPage;

import javax.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class LoginPageTest extends AbstractI18NTest {

    @Page
    protected LoginPage loginPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                .providerId("github")
                .alias("github")
                .build());
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                .providerId("saml")
                .alias("mysaml")
                .build());
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                .providerId("oidc")
                .alias("myoidc")
                .displayName("MyOIDC")
                .build());

    }

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
        ProfileAssume.assumeCommunity();
        
        DefaultHttpClient httpClient = (DefaultHttpClient) new HttpClientBuilder().build();
        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();

        loginPage.open();
        Response response = client.target(driver.getCurrentUrl()).request().acceptLanguage("de").get();
        Assert.assertTrue(response.readEntity(String.class).contains("Anmeldung bei test"));

        response = client.target(driver.getCurrentUrl()).request().acceptLanguage("en").get();
        Assert.assertTrue(response.readEntity(String.class).contains("Log in to test"));
    }

    @Test
    public void testIdentityProviderCapitalization(){
        loginPage.open();
        Assert.assertEquals("GitHub", loginPage.findSocialButton("github").getText());
        Assert.assertEquals("mysaml", loginPage.findSocialButton("mysaml").getText());
        Assert.assertEquals("MyOIDC", loginPage.findSocialButton("myoidc").getText());

    }
}
