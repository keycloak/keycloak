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
package org.keycloak.testsuite.auth.page.account;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.page.PageWithLogOutAction;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 * @author tkyjovsk
 */
public class AccountManagement extends AuthRealm implements PageWithLogOutAction {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("account");
    }

    @FindBy(xpath = "//a[@id='referer']")
    private WebElement backToRefererLink;

    @FindBy(linkText = "Sign out")
    private WebElement signOutLink;

    @FindBy(linkText = "Account")
    private WebElement accountLink;

    @FindBy(linkText = "Password")
    private WebElement passwordLink;

    @FindBy(linkText = "Authenticator")
    private WebElement authenticatorLink;

    @FindBy(linkText = "Sessions")
    private WebElement sessionsLink;

    @FindBy(linkText = "Applications")
    private WebElement applicationsLink;

    @FindBy(linkText = "Federated Identity")
    private WebElement federatedIdentityLink;

    @FindByJQuery("button[value='Save']")
    private WebElement save;

    @FindBy(xpath = "//div[@id='kc-error-message']/p")
    private WebElement error;

    public String getErrorMessage() {
        waitUntilElement(error, "Error message should be present").is().present();
        return error.getText();
    }

    public void backToReferer() {
        backToRefererLink.click();
    }

    public void signOut() {
        signOutLink.click();
        waitForPageToLoad();
    }
    
    @Override
    public void logOut() {
        signOut();
    }
    
    public void account() {
        accountLink.click();
        waitForPageToLoad();
    }

    public void password() {
        passwordLink.click();
        waitForPageToLoad();
    }

    public void authenticator() {
        authenticatorLink.click();
        waitForPageToLoad();
    }

    public void sessions() {
        sessionsLink.click();
        waitForPageToLoad();
    }

    public void applications() {
        applicationsLink.click();
        waitForPageToLoad();
    }

    public void federatedIdentity() {
        federatedIdentityLink.click();
        waitForPageToLoad();
    }

    public void save() {
        save.click();
        waitForPageToLoad();
    }

//    public RealmResource realmResource() {
//        return keycloak().realm(getAuthRealm());
//    }

}
