/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.webauthn.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElementOrNull;

/**
 * Helper class for getting available authenticators on WebAuthnLogin page
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnAuthenticatorsList {

    @FindBy(xpath = "//div[contains(@id,'kc-webauthn-authenticator-item-')]")
    private List<WebElement> authenticators;

    public List<WebAuthnAuthenticatorItem> getItems() {
        try {
            List<WebAuthnAuthenticatorItem> items = new ArrayList<>();
            for (int i = 0; i < authenticators.size(); i++) {
                WebElement auth = authenticators.get(i);
                final String nameId = "kc-webauthn-authenticator-label-" + i;
                String name = getTextFromElementOrNull(() -> auth.findElement(By.id(nameId)));
                final String createdAtId = "kc-webauthn-authenticator-created-" + i;
                String createdAt = getTextFromElementOrNull(() -> auth.findElement(By.id(createdAtId)));
                final String createdAtLabelId = "kc-webauthn-authenticator-createdlabel-" + i;
                String createdAtLabel = getTextFromElementOrNull(() -> auth.findElement(By.id(createdAtLabelId)));
                final String transportId = "kc-webauthn-authenticator-transport-" + i;
                String transport = getTextFromElementOrNull(() -> auth.findElement(By.id(transportId)));
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
