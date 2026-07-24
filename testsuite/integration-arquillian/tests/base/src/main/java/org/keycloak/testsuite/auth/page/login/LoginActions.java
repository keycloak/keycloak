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

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.testsuite.util.URLUtils;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 *
 * @author tkyjovsk
 */
public class LoginActions extends LoginBase {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("login-actions");
    }

    @FindBy(css = "*[type='submit']")
    private WebElement submitButton;

    @FindBy(css = "*[name='cancel-aia']")
    private WebElement cancelButton;

    public void submit() {
        clickLink(submitButton);
    }

    public void cancel() {
        clickLink(cancelButton);
    }

    @Override
    public boolean isCurrent() {
        return URLUtils.currentUrlStartsWith(toString() + "?"); // ignore the query string
    }
}
