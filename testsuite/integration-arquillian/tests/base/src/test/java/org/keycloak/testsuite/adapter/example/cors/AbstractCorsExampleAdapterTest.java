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

package org.keycloak.testsuite.adapter.example.cors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.AngularCorsProductTestApp;
import org.keycloak.testsuite.adapter.page.CorsDatabaseServiceTestApp;
import org.keycloak.testsuite.auth.page.account.Account;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * Created by fkiss.
 */
public abstract class AbstractCorsExampleAdapterTest extends AbstractExampleAdapterTest {

    public static final String CORS = "cors";
    public static final String AUTH_SERVER_HOST = "localhost-auth";
    private static String hostBackup;

    @Page
    private AngularCorsProductTestApp angularCorsProductPage;

    @Page
    private Account testRealmAccount;

    @Deployment(name = AngularCorsProductTestApp.DEPLOYMENT_NAME)
    private static WebArchive angularCorsProductExample() throws IOException {
        return exampleDeployment(AngularCorsProductTestApp.CLIENT_ID);
    }

    @Deployment(name = CorsDatabaseServiceTestApp.DEPLOYMENT_NAME)
    private static WebArchive corsDatabaseServiceExample() throws IOException {
        return exampleDeployment(CorsDatabaseServiceTestApp.CLIENT_ID);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(
                loadRealm(new File(EXAMPLES_HOME_DIR + "/cors/cors-realm.json")));
    }

    static{
        hostBackup = System.getProperty("auth.server.host", "localhost");
        System.setProperty("auth.server.host", AUTH_SERVER_HOST);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(CORS);
        testRealmLoginPage.setAuthRealm(CORS);
        testRealmAccount.setAuthRealm(CORS);
    }

    @Test
    public void angularCorsProductTest() {
        angularCorsProductPage.navigateTo();
        testRealmLoginPage.form().login("bburke@redhat.com", "password");

        assertCurrentUrlStartsWith(angularCorsProductPage);
        angularCorsProductPage.reloadData();
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("iphone");
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("ipad");
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("ipod");

        angularCorsProductPage.loadRoles();
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("user");

        angularCorsProductPage.addRole();
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("stuff");

        angularCorsProductPage.deleteRole();
        waitUntilElement(angularCorsProductPage.getOutput()).text().not().contains("stuff");

        angularCorsProductPage.loadAvailableSocialProviders();
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("twitter");
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("google");
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("linkedin");
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("facebook");
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("stackoverflow");
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("github");
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("microsoft");

        angularCorsProductPage.loadPublicRealmInfo();
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("Realm name: cors");

        angularCorsProductPage.loadVersion();
        waitUntilElement(angularCorsProductPage.getOutput()).text().contains("Keycloak version: " + System.getProperty("project.version"));

    }

    @AfterClass
    public static void afterCorsTest() {
        System.setProperty("auth.server.host", hostBackup);
    }
}