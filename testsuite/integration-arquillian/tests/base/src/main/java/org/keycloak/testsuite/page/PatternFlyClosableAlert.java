/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.page;

import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class PatternFlyClosableAlert extends AbstractPatternFlyAlert {

    @FindBy(xpath = ".//button[@class='close']")
    protected WebElement closeButton;

    public boolean isInfo() {
        return checkAlertType("info");
    }

    public boolean isWarning() {
        return checkAlertType("waring");
    }

    public boolean isDanger() {
        return checkAlertType("danger");
    }

    @Override
    public void assertSuccess(String expectedText) {
        super.assertSuccess(expectedText);
        close();
    }

    public void assertInfo() {
        assertInfo(null);
    }

    public void assertInfo(String expectedText) {
        assertDisplayed();
        assertTrue("Alert type should be info", isInfo());
        if (expectedText != null) assertEquals(expectedText, getText());
        close();
    }

    public void assertWarning() {
        assertWarning(null);
    }

    public void assertWarning(String expectedText) {
        assertDisplayed();
        assertTrue("Alert type should be warning", isWarning());
        if (expectedText != null) assertEquals(expectedText, getText());
        close();
    }

    public void assertDanger() {
        assertDanger(null);
    }

    public void assertDanger(String expectedText) {
        assertDisplayed();
        assertTrue("Alert type should be danger", isDanger());
        if (expectedText != null) assertEquals(expectedText, getText());
        close();
    }

    public void close() {
        closeButton.click();
        WaitUtils.pause(500); // Sometimes, when a test is too fast,
                                    // one of the consecutive alerts is not displayed;
                                    // to prevent this we need to slow down a bit
    }

}
