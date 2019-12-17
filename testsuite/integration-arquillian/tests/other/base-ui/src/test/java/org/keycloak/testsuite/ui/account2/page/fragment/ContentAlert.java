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

import org.jboss.arquillian.graphene.fragment.Root;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.UIUtils.doesElementClassContain;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElementIsNotPresent;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ContentAlert {
    private static final String ROOT_ID = "content-alert";

    @FindBy(id = ROOT_ID)
    private AlertElement alertElement;

    public boolean isDisplayed() {
        try {
            return alertElement.getRoot().isDisplayed();
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
            waitUntilElementIsNotPresent(By.id(ROOT_ID));
        }
        catch (TimeoutException e) {
            throw new AssertionError("Alert is still displayed", e);
        }
    }

    public String getMessage() {
        return getTextFromElement(alertElement.getMessageElement());
    }

    public void close() {
        alertElement.getCloseBtn().click();
        assertIsNotDisplayed();
    }

    protected void assertAlertType(String type) {
        assertTrue("Alert is not " + type, doesElementClassContain(alertElement.getRoot(), type));
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

    /**
     * Elements are placed into a separate class to leverage Page Fragment functionality so that all elements are found
     * under the Root element.
     */
    private class AlertElement {
        @Root
        private WebElement root;

        @FindBy(className = "pf-c-alert__description")
        private WebElement messageElement;

        @FindBy(id = "content-alert-close")
        private WebElement closeBtn;

        public WebElement getRoot() {
            return root;
        }

        public WebElement getMessageElement() {
            return messageElement;
        }

        public WebElement getCloseBtn() {
            return closeBtn;
        }
    }
}
