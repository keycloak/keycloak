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

package org.keycloak.testsuite.admin.page.account;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.admin.model.Account;
import org.keycloak.testsuite.admin.page.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Petr Mensik
 */
public class AccountPage extends AbstractPage {

    @FindBy(id = "username")
    private WebElement username;

    @FindBy(id = "email")
    private WebElement email;

    @FindBy(id = "lastName")
    private WebElement lastName;

    @FindBy(id = "firstName")
    private WebElement firstName;

    @FindByJQuery("button[value='Save']")
    private WebElement save;
	
	@FindByJQuery(".nav li:eq(0) a")
    private WebElement keyclockConsole;

    @FindByJQuery(".nav li:eq(1) a")
    private WebElement signOutLink;

    @FindByJQuery(".bs-sidebar ul li:eq(0) a")
    private WebElement accountLink;

    @FindByJQuery(".bs-sidebar ul li:eq(1) a")
    private WebElement passwordLink;

    @FindByJQuery(".bs-sidebar ul li:eq(2) a")
    private WebElement authenticatorLink;

    @FindByJQuery(".bs-sidebar ul li:eq(3) a")
    private WebElement sessionsLink;


    public Account getAccount() {
        return new Account(username.getAttribute("value"), email.getAttribute("value"), lastName.getAttribute("value"), firstName.getAttribute("value"));
    }

    public void setAccount(Account account) {
        email.clear();
        email.sendKeys(account.getEmail());
        lastName.clear();
        lastName.sendKeys(account.getLastName());
        firstName.clear();
        firstName.sendKeys(account.getFirstName());
    }

    public void save() {
        save.click();
    }
	
	 public void keycloakConsole() {
        keyclockConsole.click();
    }

    public void signOut() {
        signOutLink.click();
    }

    public void account() {
        accountLink.click();
    }

    public void password() {
        passwordLink.click();
    }

    public void authenticator() {
        authenticatorLink.click();
    }

    public void sessions() {
        sessionsLink.click();
    }
}
