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
package org.keycloak.testsuite.auth.page.login;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;


/**
 * @author vramik
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ResetCredentials extends LoginActions {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("reset-credentials");
    }

    @FindBy(id = "username")
    private WebElement usernameOrEmailInput;

    @FindBy(xpath = "//a[contains(., 'Back to Login')]")
    private WebElement backToLoginLink;

    @FindBy(id = "kc-info")
    private WebElement info;
    
    public void resetCredentials(String usernameOrEmail) {
        setTextInputValue(usernameOrEmailInput, usernameOrEmail);
        submit();
    }

    public void backToLogin() {
        clickLink(backToLoginLink);
    }

    public String getInfoMessage() {
        return getTextFromElement(info);
    }
}
