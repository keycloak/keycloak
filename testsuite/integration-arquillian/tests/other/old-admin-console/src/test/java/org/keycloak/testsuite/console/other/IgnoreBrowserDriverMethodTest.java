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

package org.keycloak.testsuite.console.other;

import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDrivers;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.util.BrowserDriverUtil.isDriverChrome;
import static org.keycloak.testsuite.util.BrowserDriverUtil.isDriverFirefox;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class IgnoreBrowserDriverMethodTest extends AbstractConsoleTest {

    @Test
    @IgnoreBrowserDriver(FirefoxDriver.class)
    public void allExceptFirefox() {
        assertThat(isDriverFirefox(driver), is(false));
    }

    @Test
    @IgnoreBrowserDriver(value = FirefoxDriver.class, negate = true)
    public void onlyFirefox() {
        assertThat(isDriverFirefox(driver), is(true));
    }

    @Test
    @IgnoreBrowserDriver(ChromeDriver.class)
    public void allExceptChrome() {
        assertThat(isDriverChrome(driver), is(false));
    }

    @Test
    @IgnoreBrowserDriver(value = ChromeDriver.class, negate = true)
    public void onlyChrome() {
        assertThat(isDriverChrome(driver), is(true));
    }

    @Test
    @IgnoreBrowserDrivers({
            @IgnoreBrowserDriver(FirefoxDriver.class),
            @IgnoreBrowserDriver(ChromeDriver.class)
    })
    public void ignoreChromeAndFirefox() {
        assertThat(isDriverChrome(driver), is(false));
        assertThat(isDriverFirefox(driver), is(false));
    }

    @Test
    public void executeWithEachDriver() {
        assertThat(driver, notNullValue());
    }
}
