/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.admin.fragment;

import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.waitModel;
import static org.keycloak.testsuite.admin.util.SeleniumUtils.waitGuiForElement;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 *
 * @author Petr Mensik
 */
public class Navigation {

    @Drone
    private WebDriver driver;

    @FindByJQuery("a:contains('Settings')")
    private WebElement settingsLink;

    @FindByJQuery("a:contains('Users')")
    private WebElement usersLink;

    @FindByJQuery("a:contains('Roles')")
    private WebElement rolesLink;

    @FindByJQuery("a:contains('Clients')")
    private WebElement clientsLink;

    @FindByJQuery("a:contains('OAuth')")
    private WebElement oauthLink;

    @FindByJQuery("a:contains('Tokens')")
    private WebElement tokensLink;

    @FindByJQuery("a:contains('Sessions')")
    private WebElement sessionLink;

    @FindByJQuery("a:contains('Security Defenses')")
    private WebElement securityLink;

    @FindByJQuery("a:contains('Events')")
    private WebElement eventsLink;

    @FindByJQuery("a:contains('Login')")
    private WebElement loginLink;

    @FindByJQuery("a:contains('Themes')")
    private WebElement themesLink;

    @FindByJQuery("a:contains('Role Mappings')")
    private WebElement usersRoleMappings;

    @FindByJQuery("a:contains('Add Realm')")
    private WebElement addRealm;

    @FindByJQuery("a:contains('Authentication')")
    private WebElement authentication;

    @FindByJQuery("a:contains('Password Policy')")
    private WebElement passwordPolicy;

    @FindByJQuery("a:contains('Attributes')")
    private WebElement attributes;

    @FindBy(css = "div h1")
    private WebElement currentHeader;

    public void selectRealm(String realmName) {
        driver.findElement(By.linkText(realmName)).click();
    }

    public void settings() {
        openPage(settingsLink, "Master");
    }

    public void users() {
        openPage(usersLink, "Users");
    }

    public void roles() {
        openPage(rolesLink, "Roles");
    }

    public void clients() {
        openPage(clientsLink, "Clients");
    }

    public void oauth() {
        openPage(oauthLink, "OAuth Clients");
    }

    public void tokens() {
        openPage(tokensLink, "Master");
    }

    public void sessions() {
        openPage(sessionLink, "Sessions");
    }

    public void security() {
        openPage(securityLink, "Master");
    }

    public void events() {
        openPage(eventsLink, "Events");
    }

    public void login() {
        openPage(loginLink, "Master");
    }

    public void themes() {
        openPage(themesLink, "Master");
    }

    public void roleMappings(String username) {
        String usernameCapitalized = Character.toUpperCase(username.charAt(0))
                + username.substring(1);
        openPage(usersRoleMappings, usernameCapitalized);
    }

    public void addRealm() {
        openPage(addRealm, "Add Realm");
    }

    public void passwordPolicy() {
        openPage(authentication, "Authentication");
        openPage(passwordPolicy, "Authentication");
    }

    public void attributes() {
        openPage(attributes, "Attributes");
    }

    private void openPage(WebElement page, String headerText) {
        waitGuiForElement(page);
        page.click();
        waitModel().until().element(currentHeader).text().contains(headerText);
    }
}
