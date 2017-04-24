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

package org.keycloak.testsuite.adapter.nodejs;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.adapter.nodejs.page.NodejsExamplePage;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * This test class expects following:
 * <ul>
 *     <li>The {@value #EXAMPLE_REALM_PROPERTY_NAME} System Property to be set and pointing to a realm configuration file
 *     of the example app from keycloak-nodejs-connect project</li>
 *     <li>The stated example app to be running</li>
 * </ul>
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class NodejsAdapterTest extends AbstractAuthTest {
    private static final String EXAMPLE_REALM_PROPERTY_NAME = "testsuite.adapter.nodejs.example.realm";
    private static RealmRepresentation exampleRealm;

    private static final String USER = "user";
    private static final String PASSWORD = "password";

    @Page
    private NodejsExamplePage nodejsExamplePage;

    @BeforeClass
    public static void beforeNodejsAdapterTestClass() {
        String exampleRealmPath = System.getProperty(EXAMPLE_REALM_PROPERTY_NAME);
        if (exampleRealmPath == null) {
            throw new IllegalStateException(EXAMPLE_REALM_PROPERTY_NAME + " property must be set");
        }

        exampleRealm = loadRealm(new File(exampleRealmPath));
    }

    @Before
    public void beforeNodejsAdapterTest() {
        nodejsExamplePage.navigateTo();
        driver.manage().deleteAllCookies();
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(exampleRealm.getRealm());
        testRealmLoginPage.setAuthRealm(testRealmPage);
        testRealmAccountPage.setAuthRealm(testRealmPage);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(exampleRealm);
    }

    @Test
    public void simpleLoginTest() {
        nodejsExamplePage.navigateTo();
        assertCurrentUrlEquals(nodejsExamplePage);

        nodejsExamplePage.clickLogin();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmLoginPage);
        testRealmLoginPage.form().login(USER, PASSWORD);

        assertTrue("Should be redirected back to the secured page after the login", nodejsExamplePage.isOnLoginSecuredPage());
        String output = nodejsExamplePage.getOutput();
        assertFalse("Output should not be empty", output.isEmpty());

        nodejsExamplePage.clickLogin();
        assertTrue("Should be already logged in", nodejsExamplePage.isOnLoginSecuredPage());
        assertEquals("Authentication responses should be the same", output, nodejsExamplePage.getOutput());

        nodejsExamplePage.clickLogout();
        assertCurrentUrlEquals(nodejsExamplePage);

        nodejsExamplePage.clickLogin();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmLoginPage);
    }

    @Test
    public void timeoutsTest() {
        // in seconds
        final int ssoTimeout = 15;
        final int tokenTimeout = 10;

        // change the realm's timeouts
        RealmRepresentation realmUpdate = new RealmRepresentation();
        realmUpdate.setSsoSessionIdleTimeout(ssoTimeout);
        realmUpdate.setAccessTokenLifespan(tokenTimeout);
        adminClient.realm(exampleRealm.getRealm()).update(realmUpdate);



        // test access token lifespan
        nodejsExamplePage.clickLogin();
        testRealmLoginPage.form().login(USER, PASSWORD);
        assertTrue("Should be logged in", nodejsExamplePage.isOnLoginSecuredPage());
        String output = nodejsExamplePage.getOutput();

        pause(tokenTimeout * 1000);

        nodejsExamplePage.clickLogin();
        assertTrue("Should be still logged in", nodejsExamplePage.isOnLoginSecuredPage());
        assertNotEquals("Authentication responses should be different", output, nodejsExamplePage.getOutput()); // token should be refreshed



        // test SSO timeout
        pause(ssoTimeout * 1000);
        nodejsExamplePage.clickLogin();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmLoginPage);   // there should be an attempt for token refresh
                                                                    // but SSO session should be already expired
    }

    // KEYCLOAK-3284
    @Test
    public void sessionTest() {
        nodejsExamplePage.clickLogin();
        testRealmLoginPage.form().login(USER, PASSWORD);
        assertTrue("Should be logged in", nodejsExamplePage.isOnLoginSecuredPage());

        testRealmAccountPage.navigateTo();
        assertCurrentUrlEquals(testRealmAccountPage); // should be already logged in

        testRealmAccountPage.logOut();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmLoginPage);

        nodejsExamplePage.navigateTo();
        nodejsExamplePage.clickLogin();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmLoginPage); // should be logged out
    }
}
