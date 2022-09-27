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

package org.keycloak.testsuite.ui.account2.page.fragment;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ContinueCancelModal {
    public static final String ROOT_XPATH = "//div[@role='dialog']";

    @Drone
    private WebDriver driver;

    @FindBy(xpath = ROOT_XPATH)
    private WebElement root;

    @FindBy(xpath = ROOT_XPATH + "//*[@id='modal-confirm']")
    private WebElement confirmBtn;
    @FindBy(xpath = ROOT_XPATH + "//*[@id='modal-cancel']")
    private WebElement cancelBtn;

    public boolean isDisplayed() {
        return isElementVisible(root);
    }

    public void assertIsDisplayed() {
        assertTrue("Modal dialog should be displayed", isDisplayed());
    }

    public void assertIsNotDisplayed() {
        assertFalse("Modal dialog should not be displayed", isDisplayed());
    }

    public void clickConfirm() {
        clickLink(confirmBtn);
    }

    public void clickCancel() {
        clickLink(cancelBtn);
    }
}
