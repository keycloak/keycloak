/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.webauthn.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.testframework.ui.page.AbstractLoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page shown during WebAuthn login. Page is useful with Chrome testing API
 */
public class WebAuthnLoginPage extends AbstractLoginPage {

    @FindBy(id = "authenticateWebAuthnButton")
    private WebElement authenticateButton;

    @FindBy(xpath = "//div[contains(@id,'kc-webauthn-authenticator-label-')]")
    private List<WebElement> authenticatorsLabels;

    @FindBy(xpath = "//div[contains(@id,'kc-webauthn-authenticator-item-')]")
    private List<WebElement> authenticators;

    public WebAuthnLoginPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void clickAuthenticate() {
        authenticateButton.click();
    }

    public List<WebAuthnAuthenticatorItem> getItems() {
        try {
            List<WebAuthnAuthenticatorItem> items = new ArrayList<>();
            for (int i = 0; i < authenticators.size(); i++) {
                WebElement auth = authenticators.get(i);
                final String nameId = "kc-webauthn-authenticator-label-" + i;
                String name = auth.findElement(By.id(nameId)).isDisplayed() ?
                        auth.findElement(By.id(nameId)).getText() : null;
                final String createdAtId = "kc-webauthn-authenticator-created-" + i;
                String createdAt = auth.findElement(By.id(createdAtId)).isDisplayed() ?
                        auth.findElement(By.id(createdAtId)).getText() : null;
                final String createdAtLabelId = "kc-webauthn-authenticator-createdlabel-" + i;
                String createdAtLabel = auth.findElement(By.id(createdAtLabelId)).isDisplayed() ?
                        auth.findElement(By.id(createdAtLabelId)).getText() : null;
                final String transportId = "kc-webauthn-authenticator-transport-" + i;
                String transport = auth.findElement(By.id(transportId)).isDisplayed() ?
                        auth.findElement(By.id(transportId)).getText() : null;
                items.add(new WebAuthnAuthenticatorItem(name, createdAt, createdAtLabel, transport));
            }
            return items;
        } catch (NoSuchElementException e) {
            return Collections.emptyList();
        }
    }

    public int getCount() {
        try {
            return authenticators.size();
        } catch (NoSuchElementException e) {
            return 0;
        }
    }

    public List<String> getLabels() {
        try {
            return getItems().stream()
                    .filter(Objects::nonNull)
                    .map(WebAuthnAuthenticatorItem::getName)
                    .collect(Collectors.toList());
        } catch (NoSuchElementException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public String getExpectedPageId() {
        return "login-webauthn-authenticate";
    }

    public static class WebAuthnAuthenticatorItem {
        private final String name;
        private final String createdAt;
        private final String createdAtLabel;
        private final String transport;

        public WebAuthnAuthenticatorItem(String name, String createdAt, String createdAtLabel, String transport) {
            this.name = name;
            this.createdAt = createdAt;
            this.createdAtLabel = createdAtLabel;
            this.transport = transport;
        }

        public String getName() {
            return name;
        }

        public String getCreatedDate() {
            return createdAt;
        }

        public String getCreatedLabel() {
            return createdAtLabel;
        }

        public String getTransport() {
            return transport;
        }
    }
}
