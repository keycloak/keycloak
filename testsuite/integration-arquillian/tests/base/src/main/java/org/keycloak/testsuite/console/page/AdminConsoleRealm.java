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

package org.keycloak.testsuite.console.page;

import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.Optional;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 *
 * @author tkyjovsk
 */
public class AdminConsoleRealm extends AdminConsoleRealmsRoot {

    public static final String CONSOLE_REALM = "consoleRealm";

    public AdminConsoleRealm() {
        setUriParameter(CONSOLE_REALM, TEST);
    }

    public AdminConsoleRealm setConsoleRealm(String realm) {
        setUriParameter(CONSOLE_REALM, realm);
        return this;
    }

    public String getConsoleRealm() {
        return getUriParameter(CONSOLE_REALM).toString();
    }

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + CONSOLE_REALM + "}";
    }

    @FindBy(xpath = "//div[./h2[text()='Configure']]")
    private ConfigureMenu configureMenu;

    public ConfigureMenu configure() {
        waitUntilElement(By.xpath("//div[./h2[text()='Configure']]")).is().present();
        return configureMenu;
    }

    public static class ConfigureMenu {

        @FindBy(partialLinkText = "Realm Settings")
        private WebElement realmSettingsLink;
        @FindBy(partialLinkText = "Clients")
        private WebElement clientsLink;
        @FindBy(partialLinkText = "Client Scopes")
        private WebElement clientScopesLink;
        @FindBy(partialLinkText = "Roles")
        private WebElement rolesLink;
        @FindBy(partialLinkText = "Identity Providers")
        private WebElement identityProvidersLink;
        @FindBy(partialLinkText = "User Federation")
        private WebElement userFederationLink;
        @FindBy(partialLinkText = "Authentication")
        private WebElement authenticationLink;

        public void realmSettings() {
            navigateToTab(realmSettingsLink);
        }

        public void clients() {
            navigateToTab(clientsLink);
        }

        public void clientScopesLink() {
            navigateToTab(clientScopesLink);
        }

        public void roles() {
            navigateToTab(rolesLink);
        }

        public void identityProviders() {
            navigateToTab(identityProvidersLink);
        }

        public void userFederation() {
            navigateToTab(userFederationLink);
        }

        public void authentication() {
            navigateToTab(authenticationLink);
        }

        // Elements
        public WebElement getRealmSettingsTab() {
            return realmSettingsLink;
        }

        public WebElement getClientsTab() {
            return clientsLink;
        }

        public WebElement getClientScopesTab() {
            return clientScopesLink;
        }

        public WebElement getRolesTab() {
            return rolesLink;
        }

        public WebElement getUserFederationTab() {
            return userFederationLink;
        }

        public WebElement getIdentityProvidersTab() {
            return identityProvidersLink;
        }

        public WebElement getAuthenticationTab() {
            return authenticationLink;
        }
    }

    @FindBy(xpath = "//div[./h2[text()='Manage']]")
    protected ManageMenu manageMenu;

    public ManageMenu manage() {
        waitUntilElement(By.xpath("//div[./h2[text()='Manage']]")).is().present();
        return manageMenu;
    }

    public static class ManageMenu {
        @FindBy(partialLinkText = "Groups")
        private WebElement groupsLink;
        @FindBy(partialLinkText = "Users")
        private WebElement usersLink;
        @FindBy(partialLinkText = "Sessions")
        private WebElement sessionsLink;
        @FindBy(partialLinkText = "Events")
        private WebElement eventsLink;
        @FindBy(partialLinkText = "Import")
        private WebElement importLink;
        @FindBy(partialLinkText = "Export")
        private WebElement exportLink;

        public void groups() {
            navigateToTab(groupsLink);
        }

        public void users() {
            navigateToTab(usersLink);
        }

        public void sessions() {
            navigateToTab(sessionsLink);
        }

        public void events() {
            navigateToTab(eventsLink);
        }

        public void importTab() {
            navigateToTab(importLink);
        }

        public void exportTab() {
            navigateToTab(exportLink);
        }

        // Elements
        public WebElement getGroupsTab() {
            return groupsLink;
        }

        public WebElement getUsersTab() {
            return groupsLink;
        }

        public WebElement getSessionsTab() {
            return groupsLink;
        }

        public WebElement getEventsTab() {
            return groupsLink;
        }

        public WebElement getImportTab() {
            return groupsLink;
        }

        public WebElement getExportTab() {
            return groupsLink;
        }
    }

    public static void navigateToTab(WebElement tab) {
        if (tab == null) return;
        UIUtils.clickLink(tab);
    }

    public static boolean isTabActive(WebElement tab) {
        try {
            final WebElement parent = tab != null ? tab.findElement(By.xpath("./..")) : null;

            return parent != null && Optional.ofNullable(parent.getAttribute("class"))
                    .map(f -> f.equals("active"))
                    .orElse(false);
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
