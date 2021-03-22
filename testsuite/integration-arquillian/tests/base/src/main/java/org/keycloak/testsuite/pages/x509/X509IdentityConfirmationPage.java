/*
 * Copyright 2016 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.pages.x509;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.pages.AbstractPage;
import org.keycloak.testsuite.pages.LanguageComboboxAwarePage;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/24/2016
 */

public class X509IdentityConfirmationPage extends LanguageComboboxAwarePage {

    @ArquillianResource
    protected OAuthClient oauth;

    @FindBy(id = "username")
    private WebElement usernameText;

    @FindBy(name = "login")
    private WebElement confirmButton;

    @FindBy(name = "cancel")
    private WebElement ignoreButton;

    @FindBy(className = "alert-error")
    private WebElement loginErrorMessage;

    @FindBy(className = "alert-warning")
    private WebElement loginWarningMessage;

    @FindBy(className = "alert-success")
    private WebElement loginSuccessMessage;

    @FindBy(className = "alert-info")
    private WebElement loginInfoMessage;

    @FindBy(id = "counter")
    private WebElement loginDelayCounter;

    @FindBy(id = "certificate_subjectDN")
    private WebElement certificateSubjectDistinguishedName;

    public void confirm() {
        confirmButton.click();
    }

    public String getLoginDelayCounterText() {return loginDelayCounter.getText(); }

    public String getSubjectDistinguishedNameText() { return certificateSubjectDistinguishedName.getText(); }

    public String getUsernameText() { return usernameText.getText(); }

    public void ignore() {
        ignoreButton.click();
    }

    public String getError() {
        return loginErrorMessage != null ? loginErrorMessage.getText() : null;
    }

    public boolean isCurrent() {
        return driver.getTitle().equals("Log in to test") || driver.getTitle().equals("Anmeldung bei test");
    }

    @Override
    public void open() {
        oauth.openLoginForm();
        assertCurrent();
    }
}
