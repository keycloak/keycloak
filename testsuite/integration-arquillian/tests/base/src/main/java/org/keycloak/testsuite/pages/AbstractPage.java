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

package org.keycloak.testsuite.pages;

import java.time.Duration;

import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 *
 * THIS ABSTRACT PAGE WON'T BE MIGRATED TO THE NEW TEST FRAMEWORK!
 */
@Deprecated(forRemoval = true)
public abstract class AbstractPage {

    @ArquillianResource
    protected WebDriver driver;

    @ArquillianResource
    protected OAuthClient oauth;

    public void assertCurrent() {
        waitForPage(this);
    }

    public abstract String getExpectedPageId();

    public void setDriver(WebDriver driver) {
        this.driver = driver ;
        oauth.setDriver(driver);
    }

    private void waitForPage(AbstractPage page) {
        String expectedPageId = page.getExpectedPageId();
        try {
            createDefaultWait().until(d -> isCurrentPageId(expectedPageId));
        } catch (TimeoutException e) {
            Assertions.fail("Expected page '" + expectedPageId + "' to be loaded, but currently on page '" + getCurrentPageId() + "' after timeout");
        }
    }

    private boolean isCurrentPageId(String expectedPageId) {
        try {
            return expectedPageId.equals(getCurrentPageId());
        } catch (StaleElementReferenceException e) {
            return false;
        } catch (WebDriverException e) {
            // ChromeDriver reports this instead of a stale element when the document is swapped during a navigation
            if (e.getMessage() != null && e.getMessage().contains("does not belong to the document")) {
                return false;
            }
            throw e;
        }
    }

    private WebDriverWait createDefaultWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(5), Duration.ofMillis(50));
    }

    private String getCurrentPageId() {
        return driver.findElement(By.xpath("//body")).getAttribute("data-page-id");
    }
}
