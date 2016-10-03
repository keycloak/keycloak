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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

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

//    public RealmResource realmResource() {
//        return realmsResource().realm(getConsoleRealm());
//    }

    public class ConfigureMenu {

        @FindBy(partialLinkText = "Realm Settings")
        private WebElement realmSettingsLink;
        @FindBy(partialLinkText = "Clients")
        private WebElement clientsLink;
        @FindBy(partialLinkText = "Roles")
        private WebElement rolesLink;
        @FindBy(partialLinkText = "Identity Providers")
        private WebElement identityProvidersLink;
        @FindBy(partialLinkText = "User Federation")
        private WebElement userFederationLink;
        @FindBy(partialLinkText = "Authentication")
        private WebElement authenticationLink;

        public void realmSettings() {
            realmSettingsLink.click();
        }

        public void clients() {
            clientsLink.click();
        }

        public void roles() {
            rolesLink.click();
        }

        public void identityProviders() {
            identityProvidersLink.click();
        }

        public void userFederation() {
            userFederationLink.click();
        }

        public void authentication() {
            authenticationLink.click();
        }

    }

    @FindBy(xpath = "//div[./h2[text()='Manage']]")
    protected ManageMenu manageMenu;

    public ManageMenu manage() {
        waitUntilElement(By.xpath("//div[./h2[text()='Manage']]")).is().present();
        return manageMenu;
    }

    public class ManageMenu {

        @FindBy(partialLinkText = "Users")
        private WebElement usersLink;
        @FindBy(partialLinkText = "Sessions")
        private WebElement sessionsLink;
        @FindBy(partialLinkText = "Events")
        private WebElement eventsLink;

        public void users() {
            usersLink.click();
        }

        public void sessions() {
            sessionsLink.click();
        }

        public void events() {
            eventsLink.click();
        }
    }

}
