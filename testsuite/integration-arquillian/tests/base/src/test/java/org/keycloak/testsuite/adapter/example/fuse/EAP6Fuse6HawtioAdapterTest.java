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
package org.keycloak.testsuite.adapter.example.fuse;

import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.URLAssert.waitUntilUrlStartsWith;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

import java.io.IOException;
import java.util.List;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.HawtioPage;
import org.keycloak.testsuite.arquillian.AppServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.arquillian.containers.SelfManagedAppContainerLifecycle;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.WaitUtils;

import org.openqa.selenium.WebDriver;

/**
 * @author mhajas
 */
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
public class EAP6Fuse6HawtioAdapterTest extends AbstractExampleAdapterTest implements SelfManagedAppContainerLifecycle {

    @ArquillianResource
    private ContainerController controller;

    @Drone
    @JavascriptBrowser
    protected WebDriver jsDriver;

    @Page
    @JavascriptBrowser
    private HawtioPage hawtioPage;

    @Page
    @JavascriptBrowser
    private OIDCLogin testRealmLoginPageFuse;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/hawtio-realm/demorealm.json"));
    }

    @BeforeClass
    public static void enabled() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
        ContainerAssume.assumeNotAppServerSSL();
        ContainerAssume.assumeAuthServerSSL();
    }

    @Before
    public void addJSDriver() {
        DroneUtils.addWebDriver(jsDriver);
    }

    @Before
    @Override
    public void startServer() {
        try {
            AppServerTestEnricher.prepareServerDir("standalone-fuse");
        } catch (IOException ex) {
            throw new RuntimeException("Wasn't able to prepare server dir.", ex);
        }

        controller.start(testContext.getAppServerInfo().getQualifier());
    }

    @After
    @Override
    public void stopServer() {
        controller.stop(testContext.getAppServerInfo().getQualifier());
    }

    @Test
    public void hawtioLoginAndLogoutTest() {
        testRealmLoginPageFuse.setAuthRealm(DEMO);

        log.debug("Go to hawtioPage");
        hawtioPage.navigateTo();
        WaitUtils.waitForPageToLoad();

        log.debug("log in");
        waitUntilUrlStartsWith(testRealmLoginPageFuse.toString(), 60);
        testRealmLoginPageFuse.form().login("root", "password");

        waitUntilUrlStartsWith(hawtioPage.toString() + "/welcome", 180);

        hawtioPage.logout(jsDriver);
        WaitUtils.waitForPageToLoad();
        
        assertCurrentUrlStartsWith(testRealmLoginPageFuse);

        hawtioPage.navigateTo();
        WaitUtils.waitForPageToLoad();
        assertCurrentUrlStartsWith(testRealmLoginPageFuse);
    }
}
