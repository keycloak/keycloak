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
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.ProfileAssume;

import java.util.List;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class AccountPageTest extends AbstractI18NTest {

    @Page
    protected AccountUpdateProfilePage accountUpdateProfilePage;

    @Page
    protected LoginPage loginPage;

    @Test
    public void languageDropdown() {
        accountUpdateProfilePage.open();
        loginPage.login("login@test.com", "password");
        Assert.assertTrue(accountUpdateProfilePage.isCurrent());

        Assert.assertEquals("English", accountUpdateProfilePage.getLanguageDropdownText());

        accountUpdateProfilePage.openLanguage("Deutsch");
        Assert.assertEquals("Deutsch", accountUpdateProfilePage.getLanguageDropdownText());

        accountUpdateProfilePage.openLanguage("English");
        Assert.assertEquals("English", accountUpdateProfilePage.getLanguageDropdownText());
        accountUpdateProfilePage.logout();
    }

    @Test
    public void testLocalizedReferrerLinkContent() {
        ProfileAssume.assumeCommunity();
        
        RealmResource testRealm = testRealm();
        List<ClientRepresentation> foundClients = testRealm.clients().findByClientId("var-named-test-app");
        if (foundClients.isEmpty()) {
            Assert.fail("Unable to find var-named-test-app");
        }
        ClientRepresentation namedClient = foundClients.get(0);

        driver.navigate().to(accountUpdateProfilePage.getPath() + "?referrer=" + namedClient.getClientId());
        loginPage.login("test-user@localhost", "password");
        Assert.assertTrue(accountUpdateProfilePage.isCurrent());

        accountUpdateProfilePage.openLanguage("Deutsch");
        Assert.assertEquals("Deutsch", accountUpdateProfilePage.getLanguageDropdownText());

        // When a client has a name provided as a variable, the name should be resolved using a localized bundle and available to the back link
        Assert.assertEquals("Zur\u00FCck zu Test App Named - Clientkonto", accountUpdateProfilePage.getBackToApplicationLinkText());
        Assert.assertEquals(namedClient.getBaseUrl(), accountUpdateProfilePage.getBackToApplicationLinkHref());
    }
}
