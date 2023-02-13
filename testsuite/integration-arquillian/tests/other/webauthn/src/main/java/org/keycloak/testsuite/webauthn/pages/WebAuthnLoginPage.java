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

package org.keycloak.testsuite.webauthn.pages;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.pages.LanguageComboboxAwarePage;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Page shown during WebAuthn login. Page is useful with Chrome testing API
 */
public class WebAuthnLoginPage extends LanguageComboboxAwarePage {

    @FindBy(id = "authenticateWebAuthnButton")
    private WebElement authenticateButton;

    @FindBy(id = "kc-webauthn-authenticator-label")
    private List<WebElement> authenticatorsLabels;

    @Page
    private WebAuthnAuthenticatorsList authenticators;

    public void clickAuthenticate() {
        WaitUtils.waitUntilElement(authenticateButton).is().clickable();
        authenticateButton.click();
    }

    public boolean isCurrent() {
        try {
            authenticateButton.getText();
            return driver.getPageSource().contains("navigator.credentials.get");
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public WebAuthnAuthenticatorsList getAuthenticators() {
        return authenticators;
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }

}
