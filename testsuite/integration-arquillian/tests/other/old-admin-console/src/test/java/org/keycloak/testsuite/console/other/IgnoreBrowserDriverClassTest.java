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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.util.BrowserDriverUtil.isDriverChrome;
import static org.keycloak.testsuite.util.BrowserDriverUtil.isDriverFirefox;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@IgnoreBrowserDrivers({
        @IgnoreBrowserDriver(FirefoxDriver.class),
        @IgnoreBrowserDriver(value = ChromeDriver.class, negate = true)})
public class IgnoreBrowserDriverClassTest extends AbstractConsoleTest {

    @Test
    public void ignoreFirefoxAndNotChrome() {
        assertThat(isDriverChrome(driver), is(true));
        assertThat(isDriverFirefox(driver), is(false));
    }

    @Test
    @IgnoreBrowserDriver(ChromeDriver.class)
    public void ignoreChrome() {
        assertThat(isDriverChrome(driver), is(false));
    }
}
