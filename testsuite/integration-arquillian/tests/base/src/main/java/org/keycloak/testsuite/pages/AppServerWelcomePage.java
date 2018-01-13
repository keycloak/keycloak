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
package org.keycloak.testsuite.pages;

import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.adapter.page.AppServerContextRoot;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AppServerWelcomePage extends AppServerContextRoot {

    @Page
    protected OIDCLogin loginPage;

    @FindBy(xpath = "//a[text() = 'Access Control']")
    private WebElement accessControlLink;

    @FindBy(xpath = "//a[text() = 'Manage user profile']")
    private WebElement manageProfileLink;

    @FindBy(xpath = "//div[text() = 'Logout']")
    private WebElement logoutLink;

    @Override
    public boolean isCurrent() {
        return driver.getPageSource().contains("Access Control");
    }

    public void navigateToConsole() {
        WaitUtils.pause(2000);
        URLUtils.navigateToUri(getInjectedUrl().toString() + "/console", true);
        waitForPageToLoad();
    }

    public void login(String username, String password) {
        loginPage.form().waitForLoginButtonPresent();
        loginPage.form().login(username, password);
        waitForPageToLoad();
    }

    public void navigateToAccessControl() {
        accessControlLink.click();
        waitForPageToLoad();
    }

    public void navigateManageProfile() {
        manageProfileLink.click();
        waitForPageToLoad();
    }

    public void logout() {
        logoutLink.click();
        waitForPageToLoad();
    }

    public boolean isLoginPage() {
        return loginPage.isCurrent();
    }
}
