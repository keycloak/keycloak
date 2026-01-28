/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.adapter.page;

import org.jboss.logging.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * @author Alexander Schwartz
 */
public class LogScreenContents {

    private static final Logger log = Logger.getLogger(LogScreenContents.class);

    public static <T extends Throwable> T fail(WebDriver webDriver, String message, T t) {
        String screenShotBase64 = null;
        if (webDriver instanceof TakesScreenshot) {
            screenShotBase64 = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BASE64);
        }
        log.error(message + ", url '" + webDriver.getCurrentUrl() + "', title: " + webDriver.getTitle() + ", source: " + webDriver.getPageSource() +
                (screenShotBase64 != null ? ", screenshot: " + screenShotBase64 : ""));
        return t;
    }
}
