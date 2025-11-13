/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * Page object for OAuth2 Device Authorization confirmation page (login-oauth2-device-confirm.ftl)
 *
 */
public class OAuth2DeviceConfirmPage extends LanguageComboboxAwarePage {

    @FindBy(css = "[name=\"accept\"]")
    private WebElement acceptButton;

    @FindBy(css = "[name=\"cancel\"]")
    private WebElement cancelButton;

    public void accept() {
        clickLink(acceptButton);
    }

    public void cancel() {
        clickLink(cancelButton);
    }

    @Override
    public boolean isCurrent() {
        String currentUrl = driver.getCurrentUrl();
        return currentUrl.contains("execution=DEVICE_CONFIRM")
            || PageUtils.getPageTitle(driver).toLowerCase().contains("device");
    }

    public void open() {
        throw new UnsupportedOperationException("OAuth2 Device Confirm page cannot be opened directly. " +
                "It is accessed through the device authorization flow.");
    }
}
