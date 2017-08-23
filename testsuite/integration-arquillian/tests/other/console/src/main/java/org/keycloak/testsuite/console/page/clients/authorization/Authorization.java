/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.console.page.clients.authorization;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.clients.Client;
import org.keycloak.testsuite.console.page.clients.authorization.permission.Permissions;
import org.keycloak.testsuite.console.page.clients.authorization.policy.Policies;
import org.keycloak.testsuite.console.page.clients.authorization.resource.Resources;
import org.keycloak.testsuite.console.page.clients.authorization.scope.Scopes;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.navigateToLink;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class Authorization extends Client {

    @FindBy(id = "authz-tabs")
    protected AuthorizationTabLinks authorizationTabLinks;

    @Page
    private AuthorizationSettingsForm authorizationSettingsForm;

    @Page
    private Resources resources;

    @Page
    private Scopes scopes;

    @Page
    private Permissions permissions;

    @Page
    private Policies policies;

    public AuthorizationSettingsForm settings() {
        return authorizationSettingsForm;
    }

    public AuthorizationTab authorizationTabs() {
        return new AuthorizationTab(authorizationTabLinks);
    }

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/authz/resource-server";
    }

    public class AuthorizationTab {

        private final AuthorizationTabLinks links;

        public AuthorizationTab(AuthorizationTabLinks links) {
            this.links = links;
        }

        public Resources resources() {
            links.resources();
            return resources;
        }

        public Scopes scopes() {
            links.scopes();
            return scopes;
        }

        public Permissions permissions() {
            links.permissions();
            return permissions;
        }

        public Policies policies() {
            links.policies();
            return policies;
        }
    }

    public class AuthorizationTabLinks {

        @Root
        private WebElement root;

        @Drone
        private WebDriver driver;

        @FindBy(linkText = "Settings")
        private WebElement settingsLink;

        @FindBy(linkText = "Resources")
        private WebElement resourcesLink;

        @FindBy(linkText = "Authorization Scopes")
        private WebElement scopesLink;

        @FindBy(linkText = "Permissions")
        private WebElement permissionsLink;

        @FindBy(linkText = "Policies")
        private WebElement policiesLink;

        public void settings() {
            //clickLink(settingsLink);
            navigateToLink(settingsLink); // for some reason, GeckoDriver is currently having problems clicking on those tabs
        }

        public void resources() {
            //clickLink(resourcesLink);
            navigateToLink(resourcesLink);
        }

        private void scopes() {
            //clickLink(scopesLink);
            navigateToLink(scopesLink);
        }

        private void permissions() {
            //clickLink(permissionsLink);
            navigateToLink(permissionsLink);
        }

        private void policies() {
            //clickLink(policiesLink);
            navigateToLink(policiesLink);
        }
    }
}
