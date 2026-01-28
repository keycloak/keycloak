/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Determine which WebDriver is used
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class BrowserDriverUtil {

    public static boolean isDriverInstanceOf(WebDriver driver, Class<? extends WebDriver> clazz) {
        return clazz.isAssignableFrom(driver.getClass());
    }

    public static boolean isDriverChrome(WebDriver driver) {
        return isDriverInstanceOf(driver, ChromeDriver.class);
    }

    public static boolean isDriverFirefox(WebDriver driver) {
        return isDriverInstanceOf(driver, FirefoxDriver.class);
    }
}
