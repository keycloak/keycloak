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
package org.keycloak.testsuite.pages;

import org.keycloak.services.resources.account.DeprecatedAccountFormService;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountPasswordPage extends AbstractAccountPage {

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-new")
    private WebElement newPasswordInput;

    @FindBy(id = "password-confirm")
    private WebElement passwordConfirmInput;

    @FindBy(className = "btn-primary")
    private WebElement submitButton;

    private String realmName = "test";

    public void changePassword(String password, String newPassword, String passwordConfirm) {
        passwordInput.sendKeys(password);
        newPasswordInput.sendKeys(newPassword);
        passwordConfirmInput.sendKeys(passwordConfirm);

        submitButton.click();
    }

    public void changePassword(String newPassword, String passwordConfirm) {
        newPasswordInput.sendKeys(newPassword);
        passwordConfirmInput.sendKeys(passwordConfirm);

        submitButton.click();
    }

    public boolean isCurrent() {
        return driver.getTitle().contains("Account Management") && driver.getCurrentUrl().split("\\?")[0].endsWith("/account/password");
    }

    public void open() {
        driver.navigate().to(getPath());
    }

    public void realm(String realmName) {
        this.realmName = realmName;
    }

    public String getPath() {
        return DeprecatedAccountFormService.passwordUrl(UriBuilder.fromUri(getAuthServerRoot())).build(this.realmName).toString();
    }
}
