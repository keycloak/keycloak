/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.UIUtils;

import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpLinkActionPage extends AbstractPage {

    @FindBy(id = "kc-continue")
    private WebElement submitButton;

    @FindBy(id = "kc-cancel")
    private WebElement cancelButton;

    @FindBy(id = "kc-link-text")
    private WebElement message;

    @Override
    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).startsWith("Linking ");
    }

    public void confirm() {
        UIUtils.clickLink(submitButton);
    }
    public void cancel() {
        UIUtils.clickLink(cancelButton);
    }

    public void assertIdpInMessage(String expectedIdpDisplayName) {
        Assert.assertEquals("Do you want to link your account with " + expectedIdpDisplayName + "?", message.getText());
    }
}
