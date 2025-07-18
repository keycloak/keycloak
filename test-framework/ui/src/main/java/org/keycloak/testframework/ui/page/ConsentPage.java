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

package org.keycloak.testframework.ui.page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ConsentPage extends AbstractPage {

    @FindBy(id = "kc-login")
    private WebElement submitButton;

    @FindBy(id = "kc-cancel")
    private WebElement cancelButton;

    public ConsentPage(WebDriver driver) { super(driver); }

    public void confirm() {
        submitButton.click();
    }

    public void cancel() {
        cancelButton.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-oauth-grant";
    }
}
