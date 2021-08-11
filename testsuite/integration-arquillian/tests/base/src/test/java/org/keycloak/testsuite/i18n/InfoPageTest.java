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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.OAuthClient;

import javax.ws.rs.core.UriBuilder;

public class InfoPageTest extends AbstractI18NTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected InfoPage infoPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    //KEYCLOAK-18846
    @Test
    public void changeLanguage() {

        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        appPage.assertCurrent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().assertEvent();

        UriBuilder baseUriBuilder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
        UriBuilder loginActionsUriBuilder = LoginActionsService.loginActionsBaseUrl(baseUriBuilder);
        UriBuilder authUriBuilder = loginActionsUriBuilder.path(LoginActionsService.AUTHENTICATE_PATH);
        String authUrl = authUriBuilder.build(TEST_REALM_NAME).toString();
        driver.navigate().to(authUrl);

        infoPage.assertCurrent();
        Assert.assertEquals("English", infoPage.getLanguageDropdownText());

        loginPage.openLanguage("Deutsch");

        infoPage.assertCurrent();
        Assert.assertEquals("Deutsch", infoPage.getLanguageDropdownText());

    }

}
