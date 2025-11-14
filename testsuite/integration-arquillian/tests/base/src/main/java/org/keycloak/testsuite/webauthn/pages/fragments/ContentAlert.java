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

package org.keycloak.testsuite.webauthn.pages.fragments;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.doesElementClassContain;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElementIsNotPresent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 *
 * Page fragments seem not to be working after migration from CGlib to ByteBuddy in Graphene
 */
public class ContentAlert {
    private static final String ROOT_ID = "//ul[@data-testid='global-alerts']/li[1]//div";

    //The first alert from the alert group is what we are interested in.
    @FindBy(xpath = ROOT_ID)
    private WebElement alertElementRoot;

    @FindBy(className = ROOT_ID + "[@class='pf-v5-c-alert__title']")
    private WebElement messageElement;

    @FindBy(className = ROOT_ID + "[@class='pf-v5-c-alert__action']")
    private WebElement closeBtn;

    public boolean isDisplayed() {
        try {
            return alertElementRoot.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public void assertIsDisplayed() {
        assertTrue("Alert is not displayed", isDisplayed());
    }

    public void assertIsNotDisplayed() {
        try {
            waitUntilElementIsNotPresent(By.xpath(ROOT_ID));
        } catch (TimeoutException e) {
            throw new AssertionError("Alert is still displayed", e);
        }
    }

    public String getMessage() {
        return getTextFromElement(messageElement);
    }

    public void close() {
        closeBtn.click();
        assertIsNotDisplayed();
    }

    protected void assertAlertType(String type) {
        assertTrue("Alert is not " + type, doesElementClassContain(alertElementRoot, type));
    }

    protected void assertMessage(String expectedMessage) {
        assertEquals(expectedMessage, getMessage());
    }

    public void assertSuccess() {
        assertAlertType("success");
    }

    public void assertSuccess(String expectedMessage) {
        assertSuccess();
        assertMessage(expectedMessage);
    }

    public void assertDanger() {
        assertAlertType("danger");
    }

    public void assertDanger(String expectedMessage) {
        assertDanger();
        assertMessage(expectedMessage);
    }

    public void assertWarning() {
        assertAlertType("warning");
    }

    public void assertWarning(String expectedMessage) {
        assertWarning();
        assertMessage(expectedMessage);
    }

    public void assertInfo() {
        assertAlertType("info");
    }

    public void assertInfo(String expectedMessage) {
        assertInfo();
        assertMessage(expectedMessage);
    }
}
