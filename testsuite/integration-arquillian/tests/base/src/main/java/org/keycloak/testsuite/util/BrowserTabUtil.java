/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import com.gargoylesoftware.htmlunit.WebClient;
import org.jboss.arquillian.drone.webdriver.htmlunit.DroneHtmlUnitDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Helper class for managing tabs in browser.
 * Tabs are indexed from 0. (f.e. first tab has index 0)
 *
 * <p>Note: For one particular WebDriver has to exist only one BrowserTabUtil instance. (Right order of tabs)</p>
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class BrowserTabUtil implements AutoCloseable {

    private WebDriver driver;
    private JavascriptExecutor jsExecutor;
    private List<String> tabs;
    private static List<BrowserTabUtil> instances;

    private BrowserTabUtil(WebDriver driver) {
        this.driver = driver;

        if (driver instanceof JavascriptExecutor) {
            this.jsExecutor = (JavascriptExecutor) driver;
        } else {
            throw new RuntimeException("WebDriver must be instance of JavascriptExecutor");
        }

        // HtmlUnit doesn't work very well with JS and it's recommended to use this settings.
        // HtmlUnit validates all scripts and then fails. It turned off the validation.
        if (driver instanceof HtmlUnitDriver) {
            WebClient client = ((DroneHtmlUnitDriver) driver).getWebClient();
            client.getOptions().setThrowExceptionOnScriptError(false);
            client.getOptions().setThrowExceptionOnFailingStatusCode(false);
        }

        tabs = new ArrayList<>(driver.getWindowHandles());
    }

    public static BrowserTabUtil getInstanceAndSetEnv(WebDriver driver) {
        if (instances == null) {
            instances = new ArrayList<>();
        }

        BrowserTabUtil instance = instances.stream()
                .filter(inst -> inst.getDriver().toString().equals(driver.toString()))
                .findFirst()
                .orElse(null);

        if (instance == null) {
            instance = new BrowserTabUtil(driver);
            instances.add(instance);
        }
        return instance;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public String getActualWindowHandle() {
        return driver.getWindowHandle();
    }

    public void switchToTab(String windowHandle) {
        driver.switchTo().window(windowHandle);
        WaitUtils.waitForPageToLoad();
    }

    public void switchToTab(int index) {
        assertValidIndex(index);
        switchToTab(tabs.get(index));
    }

    public void newTab(String url) {
        jsExecutor.executeScript("window.open(arguments[0]);", url);

        final Set<String> handles = driver.getWindowHandles();
        final String tabHandle = handles.stream()
                .filter(tab -> !tabs.contains(tab))
                .findFirst()
                .orElse(null);

        if (handles.size() > tabs.size() + 1) {
            throw new RuntimeException("Too many window handles. You can only create a new one by this method.");
        }

        if (tabHandle == null) {
            throw new RuntimeException("Creating the new tab failed.");
        }

        tabs.add(tabHandle);
        switchToTab(tabHandle);
    }

    public void closeTab(int index) {
        assertValidIndex(index);

        if (index == 0 || getCountOfTabs() == 1)
            throw new RuntimeException("You must not close the original tab.");

        switchToTab(index);
        driver.close();

        tabs.remove(index);
        switchToTab(index - 1);
    }

    public int getCountOfTabs() {
        return tabs.size();
    }

    public void destroy() {
        for (int i = 1; i < getCountOfTabs(); i++) {
            closeTab(i);
        }
        instances.removeIf(inst -> inst.getDriver().toString().equals(driver.toString()));
    }

    private boolean validIndex(int index) {
        return (index >= 0 && tabs != null && index < tabs.size());
    }

    private void assertValidIndex(int index) {
        if (!validIndex(index))
            throw new IndexOutOfBoundsException("Invalid index of tab.");
    }

    @Override
    public void close() {
        destroy();
    }
}