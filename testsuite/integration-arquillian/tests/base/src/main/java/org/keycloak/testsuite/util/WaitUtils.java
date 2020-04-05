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

import org.jboss.arquillian.graphene.wait.ElementBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;
import static org.openqa.selenium.support.ui.ExpectedConditions.javaScriptThrowsNoExceptions;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlToBe;

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

    public static final int IMPLICIT_ELEMENT_WAIT_MILLIS = 1500; // high value means more stable but slower tests; it needs to be balanced

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

    public static void waitUntilElementIsNotPresent(By locator) {
        waitUntilElement(locator).is().not().present();
    }

    public static void waitUntilElementIsNotPresent(WebElement element) {
        waitUntilElement(element).is().not().present();
//        (new WebDriverWait(driver, IMPLICIT_ELEMENT_WAIT_MILLIS))
//                .until(invisibilityOfAllElements(Collections.singletonList(element)));
    }

    public static void waitUntilElementClassContains(WebElement element, String value) {
        new WebDriverWait(getCurrentDriver(), 1).until(
                ExpectedConditions.attributeContains(element, "class", value)
        );
    }

    public static void pause(long millis) {
        if (millis > 0) {
            log.info("Wait: " + millis + "ms");
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                Logger.getLogger(WaitUtils.class.getName()).log(Level.SEVERE, null, ex);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Waits for page to finish any pending redirects, REST API requests etc.
     * Because Keycloak's Admin Console is a single-page application, we need to
     * take extra steps to ensure the page is fully loaded
     */
    public static void waitForPageToLoad() {
        WebDriver driver = getCurrentDriver();

        if (driver instanceof HtmlUnitDriver) {
            return; // not needed
        }

        String currentUrl = null;

        // Ensure the URL is "stable", i.e. is not changing anymore; if it'd changing, some redirects are probably still in progress
        for (int maxRedirects = 4; maxRedirects > 0; maxRedirects--) {
            currentUrl = driver.getCurrentUrl();
            FluentWait<WebDriver> wait = new FluentWait<>(driver).withTimeout(Duration.ofMillis(250));
            try {
                wait.until(not(urlToBe(currentUrl)));
            }
            catch (TimeoutException e) {
                break; // URL has not changed recently - ok, the URL is stable and page is current
            }
            if (maxRedirects == 1) {
                log.warn("URL seems unstable! (Some redirect are probably still in progress)");
            }
        }

        WebDriverWait wait = new WebDriverWait(getCurrentDriver(), PAGELOAD_TIMEOUT_MILLIS / 1000);
        ExpectedCondition waitCondition = null;

        // Different wait strategies for Admin and Account Consoles
        if (currentUrl.matches("^[^\\/]+:\\/\\/[^\\/]+\\/auth\\/admin\\/.*$")) { // Admin Console
            // Checks if the document is ready and asks AngularJS, if present, whether there are any REST API requests in progress
            waitCondition = javaScriptThrowsNoExceptions(
                    "if (document.readyState !== 'complete' "
                    + "|| (typeof angular !== 'undefined' && angular.element(document.body).injector().get('$http').pendingRequests.length !== 0)) {"
                    + "throw \"Not ready\";"
                    + "}");
        }
        else if (
                currentUrl.matches("^[^\\/]+:\\/\\/[^\\/]+\\/auth\\/realms\\/[^\\/]+\\/account\\/.*#/.+$") // check for new Account Console URL
        ) {
            pause(2000); // TODO rework this temporary workaround once KEYCLOAK-11201 and/or KEYCLOAK-8181 are fixed
        }

        if (waitCondition != null) {
            try {
                wait.until(waitCondition);
            } catch (TimeoutException e) {
                log.warn("waitForPageToLoad time exceeded!");
            }
        }
    }

    public static void waitForModalFadeIn() {
        pause(500); // TODO: Find out how to do in more 'elegant' way, e.g. like in the waitForModalFadeOut
    }

    public static void waitForModalFadeOut() {
        waitUntilElementIsNotPresent(By.className("modal-backdrop"));
    }

}
