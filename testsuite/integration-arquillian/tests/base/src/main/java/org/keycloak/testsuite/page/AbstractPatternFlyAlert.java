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

package org.keycloak.testsuite.page;

import java.time.Duration;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;
import static org.keycloak.testsuite.util.UIUtils.doesElementClassContain;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;
import static org.keycloak.testsuite.util.WaitUtils.PAGELOAD_TIMEOUT_MILLIS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author tkyjovsk
 */
@Deprecated
public abstract class AbstractPatternFlyAlert {
    public static final String ALERT_CLASS_NAME = "pf-v5-c-alert";

    protected final Logger log = Logger.getLogger(this.getClass());

    @FindBy(className = ALERT_CLASS_NAME)
    protected WebElement alertRoot;

    @Drone
    protected WebDriver driver;

    public boolean isDisplayed() {
        return isElementVisible(alertRoot);
    }

    public static void waitUntilDisplayed() {
       waitUntilDisplayedOrHidden(true);
    }

    public static void waitUntilHidden() {
       waitUntilDisplayedOrHidden(false);
    }

    private static void waitUntilDisplayedOrHidden(boolean displayed) {
        ExpectedCondition condition = ExpectedConditions.visibilityOfElementLocated(By.className(ALERT_CLASS_NAME));
        condition = displayed ? condition : ExpectedConditions.not(condition);
        new WebDriverWait(getCurrentDriver(), Duration.ofMillis(PAGELOAD_TIMEOUT_MILLIS)).until(condition);
    }

    public String getText() {
        return getTextFromElement(alertRoot);
    }

    public boolean isSuccess() {
        return checkAlertType("success");
    }

    public void assertDisplayed() {
        assertTrue("Alert should displayed", isDisplayed());
    }

    public void assertNotDisplayed() {
        assertFalse("Alert shouldn't be displayed", isDisplayed());
    }

    public void assertSuccess() {
        assertSuccess(null);
    }

    public void assertSuccess(String expectedText) {
        assertDisplayed();
        assertTrue("Alert type should be success", isSuccess());
        if (expectedText != null) assertEquals(expectedText, getText());
    }

    protected boolean checkAlertType(String type) {
        return doesElementClassContain(alertRoot, "alert-" + type);
    }

}
