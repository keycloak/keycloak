/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.x509;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.testsuite.util.PhantomJSBrowser;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 8/12/2016
 */

public class X509BrowserLoginSubjectAltNameTest extends AbstractX509AuthenticationTest {

    @Drone
    @PhantomJSBrowser
    private WebDriver phantomJS;

    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(phantomJS);
    }

    @BeforeClass
    public static void onBeforeTestClass() {
        configurePhantomJS("/ca.crt", "/certs/clients/test-user-san@localhost.cert.pem",
                "/certs/clients/test-user@localhost.key.pem", "password");
    }

    @Test
    public void loginAsUserFromCertSANEmail() {
        x509BrowserLogin(createLoginSubjectAltNameEmail2UserAttributeConfig(), userId, "test-user@localhost", "test-user-altmail@localhost");
    }

    @Test
    public void loginAsUserFromCertSANUpn() {
        x509BrowserLogin(createLoginSubjectAltNameOtherName2UserAttributeConfig(), userId, "test-user@localhost", "test_upn_name@localhost");
    }
}