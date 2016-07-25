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
package org.keycloak.testsuite.util;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.jboss.arquillian.graphene.wait.ElementBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.jboss.arquillian.graphene.Graphene.waitModel;
import static org.openqa.selenium.support.ui.ExpectedConditions.*;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class WaitUtils {

    protected final static org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(WaitUtils.class);

    public static final String PAGELOAD_TIMEOUT_PROP = "pageload.timeout";

    public static final Integer PAGELOAD_TIMEOUT_MILLIS = Integer.parseInt(System.getProperty(PAGELOAD_TIMEOUT_PROP, "10000"));

    public static final int IMPLICIT_ELEMENT_WAIT_MILLIS = 750;

    // Should be no longer necessary for finding elements since we have implicit wait
    public static ElementBuilder<Void> waitUntilElement(By by) {
        return waitGui().until().element(by);
    }

    // Should be no longer necessary for finding elements since we have implicit wait
    public static ElementBuilder<Void> waitUntilElement(WebElement element) {
        return waitGui().until().element(element);
    }

    // Should be no longer necessary for finding elements since we have implicit wait
    public static ElementBuilder<Void> waitUntilElement(WebElement element, String failMessage) {
        return waitGui().until(failMessage).element(element);
    }

    public static void waitUntilElementIsNotPresent(WebDriver driver, By locator) {
        waitUntilElementIsNotPresent(driver, driver.findElement(locator));
    }

    public static void waitUntilElementIsNotPresent(WebDriver driver, WebElement element) {
        (new WebDriverWait(driver, IMPLICIT_ELEMENT_WAIT_MILLIS))
                .until(invisibilityOfAllElements(Collections.singletonList(element)));
    }

    public static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Logger.getLogger(WaitUtils.class.getName()).log(Level.SEVERE, null, ex);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Waits for page to finish any pending redirects, REST API requests etc.
     * Because Keycloak's Admin Console is a single-page application, we need to take extra steps to ensure
     * the page is fully loaded
     *
     * @param driver
     */
    public static void waitForPageToLoad(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, PAGELOAD_TIMEOUT_MILLIS / 1000);

        try {
            wait.until(not(urlContains("redirect_fragment")));

            // Checks if the document is ready and asks AngularJS, if present, whether there are any REST API requests
            // in progress
            wait.until(javaScriptThrowsNoExceptions(
                "if (document.readyState !== 'complete' " +
                    "|| (typeof angular !== 'undefined' && angular.element(document.body).injector().get('$http').pendingRequests.length !== 0)) {" +
                        "throw \"Not ready\";" +
                "}"));
        }
        catch (TimeoutException e) {
            // Sometimes, for no obvious reason, the browser/JS doesn't set document.readyState to 'complete' correctly
            // but that's no reason to let the test fail; after the timeout the page is surely fully loaded
            log.warn("waitForPageToLoad time exceeded!");
        }
    }

    public static void waitForModalFadeIn(WebDriver driver) {
        pause(500); // TODO: Find out how to do in more 'elegant' way, e.g. like in the waitForModalFadeOut
    }

    public static void waitForModalFadeOut(WebDriver driver) {
        waitUntilElementIsNotPresent(driver, By.className("modal-backdrop"));
    }

}
