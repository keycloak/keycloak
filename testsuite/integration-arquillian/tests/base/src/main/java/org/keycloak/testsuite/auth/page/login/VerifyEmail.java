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

import org.keycloak.models.UserModel;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class VerifyEmail extends RequiredActions {

    @FindBy(xpath = "//div[@id='kc-content-wrapper']/p[contains(@class, 'instruction')][1]")
    private WebElement instruction;

    @FindBy(xpath = "//div[@id='kc-content-wrapper']/p[contains(@class, 'instruction')][2]/a[text()='Click here']")
    private WebElement resendLink;

    @Override
    public String getActionId() {
        return UserModel.RequiredAction.VERIFY_EMAIL.name();
    }

    public String getInstructionMessage() {
        return getTextFromElement(instruction);
    }

    public void clickResend() {
        clickLink(resendLink);
    }
}
