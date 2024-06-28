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

package org.keycloak.test.page;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 */
public class IndexPage {

    public static final String UNAUTHORIZED = "401 Unauthorized";

    @FindBy(name = "loginBtn")
    private WebElement loginButton;

    @FindBy(name = "logoutBtn")
    private WebElement logoutButton;

    @FindBy(name = "adminBtn")
    private WebElement adminButton;

    @FindBy(name = "publicBtn")
    private WebElement publicButton;

    @FindBy(name = "securedBtn")
    private WebElement securedBtn;

    public void clickLogin() {
        loginButton.click();
    }

    public void clickLogout() {
        logoutButton.click();
    }

    public void clickAdmin() {
        adminButton.click();
    }

    public void clickPublic() {
        publicButton.click();
    }

    public void clickSecured() {
        securedBtn.click();
    }
}
