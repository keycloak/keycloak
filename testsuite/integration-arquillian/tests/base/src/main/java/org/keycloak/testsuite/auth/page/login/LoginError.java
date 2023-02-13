/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.auth.page.login;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class LoginError extends LoginBase {
    @FindBy(xpath = "//div[@id='kc-error-message']/p[@class='instruction']")
    private WebElement errorMessage;

    @FindBy(id = "backToApplication")
    private WebElement backToApplicationLink;

    public String getErrorMessage() {
        return getTextFromElement(errorMessage);
    }

    public void backToApplication() {
        clickLink(backToApplicationLink);
    }

    @Override
    public boolean isCurrent() {
        return getTitleText().equals("We are sorry...");
    }
}
