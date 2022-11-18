/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.console.other;

import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.realm.GeneralSettings;
import org.openqa.selenium.WebElement;

import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.console.page.AdminConsoleRealm.isTabActive;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class ConsoleActiveMenuTest extends AbstractConsoleTest {

    @Page
    GeneralSettings generalSettings;

    @Test
    public void clientsActive() {
        checkActiveTab("client", () -> adminConsoleRealmPage.configure().getClientsTab());
    }

    @Test
    public void clientScopesActive() {
        checkActiveTab("client-scope", () -> adminConsoleRealmPage.configure().getClientScopesTab());
    }

    @Test
    public void rolesActive() {
        checkActiveTab("role", () -> adminConsoleRealmPage.configure().getRolesTab());
    }

    @Test
    public void identityProvidersActive() {
        checkActiveTab("identity-provider", () -> adminConsoleRealmPage.configure().getIdentityProvidersTab());
    }

    @Test
    public void userFederationActive() {
        checkActiveTab("user-storage", () -> adminConsoleRealmPage.configure().getUserFederationTab());
    }

    @Test
    public void authenticationActive() {
        checkActiveTab("authentication", () -> adminConsoleRealmPage.configure().getAuthenticationTab());
    }

    // Manage
    @Test
    public void groupsActive() {
        checkActiveTab("group", () -> adminConsoleRealmPage.manage().getGroupsTab());
    }

    @Test
    public void usersActive() {
        checkActiveTab("user", () -> adminConsoleRealmPage.manage().getUsersTab());
    }

    private void checkActiveTab(String realmName, Supplier<WebElement> getElement) {
        adminConsoleRealmPage.navigateTo();
        adminConsoleRealmPage.assertCurrent();
        adminConsoleRealmPage.configure().realmSettings();

        generalSettings.setRealmName(realmName);
        try {
            generalSettings.save();
            waitForPageToLoad();

            final WebElement tab = getElement.get();
            assertThat(isTabActive(tab), CoreMatchers.is(false));

            AdminConsoleRealm.navigateToTab(tab);
            waitForPageToLoad();
            assertThat(isTabActive(getElement.get()), CoreMatchers.is(true));
        } finally {
            adminConsoleRealmPage.configure().realmSettings();
            generalSettings.setRealmName("test");
            generalSettings.save();
        }
    }
}
