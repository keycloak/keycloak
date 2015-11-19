/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.util;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.jboss.arquillian.graphene.Graphene.waitAjax;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public final class WaitUtils {

    public static final String PAGELOAD_TIMEOUT_PROP = "pageload.timeout";
    public static final String IMPLICIT_TIMEOUT_PROP = "implicit.timeout";
    public static final String SCRIPT_TIMEOUT_PROP = "script.timeout";
    public static final String POLLING_INTERVAL_PROP = "polling.interval";

    public static final Integer PAGELOAD_TIMEOUT = Integer.parseInt(System.getProperty(PAGELOAD_TIMEOUT_PROP, "5000"));
    public static final Integer IMPLICIT_TIMEOUT = Integer.parseInt(System.getProperty(IMPLICIT_TIMEOUT_PROP, "3000"));
    public static final Integer SCRIPT_TIMEOUT = Integer.parseInt(System.getProperty(SCRIPT_TIMEOUT_PROP, "3000"));

    public static final Integer POLLING_INTERVAL = Integer.parseInt(System.getProperty(POLLING_INTERVAL_PROP, "1000"));

    public static void waitAjaxForElement(WebElement element) {
        waitAjax().withTimeout(SCRIPT_TIMEOUT, TimeUnit.MILLISECONDS).pollingEvery(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                .until().element(element).is().present();
    }

    public static void waitAjaxForElementNotPresent(WebElement element) {
        waitAjax().withTimeout(SCRIPT_TIMEOUT, TimeUnit.MILLISECONDS).pollingEvery(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                .until().element(element).is().not().present();
    }

    public static void waitAjaxForElementNotVisible(WebElement element) {
        waitAjax().withTimeout(SCRIPT_TIMEOUT, TimeUnit.MILLISECONDS).pollingEvery(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                .until().element(element).is().not().visible();
    }

    public static void waitGuiForElement(By element, String message) {
        waitGui().withTimeout(IMPLICIT_TIMEOUT, TimeUnit.MILLISECONDS).pollingEvery(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                .until(message).element(element).is().present();
    }

    public static void waitGuiForElement(By element) {
        waitGuiForElement(element, null);
    }

    public static void waitGuiForElement(WebElement element) {
        waitGuiForElementPresent(element, null);
    }

    public static void waitGuiForElementPresent(WebElement element, String message) {
        waitGui().withTimeout(IMPLICIT_TIMEOUT, TimeUnit.MILLISECONDS).pollingEvery(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                .until(message).element(element).is().present();
    }

    public static void waitGuiForElementNotPresent(WebElement element) {
        waitGui().withTimeout(IMPLICIT_TIMEOUT, TimeUnit.MILLISECONDS).pollingEvery(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                .until().element(element).is().not().present();
    }

    public static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Logger.getLogger(WaitUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
