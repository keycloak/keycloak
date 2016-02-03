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
package org.keycloak.testsuite;

import org.apache.catalina.startup.Tomcat;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.keycloaksaml.SamlAdapterTestStrategy;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.net.URL;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TomcatSamlTest {
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            SamlAdapterTestStrategy.baseAdapterTestInitialization(session, manager, adminRealm, getClass());
       }
    };

    static Tomcat tomcat = null;

    @BeforeClass
    public static void initTomcat() throws Exception {
        tomcat = new Tomcat();
        String baseDir = TomcatTest.getBaseDirectory();
        tomcat.setBaseDir(baseDir);
        tomcat.setPort(8082);

        System.setProperty("app.server.base.url", "http://localhost:8082");
        System.setProperty("my.host.name", "localhost");
        URL dir = TomcatSamlTest.class.getResource("/keycloak-saml/testsaml.json");
        File base = new File(dir.getFile()).getParentFile();
        tomcat.addWebapp("/sales-post", new File(base, "simple-post").toString());
        tomcat.addWebapp("/sales-post2", new File(base, "simple-post2").toString());
        tomcat.addWebapp("/input-portal", new File(base, "simple-input").toString());
        tomcat.addWebapp("/sales-post-sig", new File(base, "signed-post").toString());
        tomcat.addWebapp("/sales-post-sig-email", new File(base, "signed-post-email").toString());
        tomcat.addWebapp("/sales-post-sig-transient", new File(base, "signed-post-transient").toString());
        tomcat.addWebapp("/sales-post-sig-persistent", new File(base, "signed-post-persistent").toString());
        tomcat.addWebapp("/sales-metadata", new File(base, "signed-metadata").toString());
        tomcat.addWebapp("/employee-sig", new File(base, "signed-get").toString());
        tomcat.addWebapp("/employee2", new File(base, "mappers").toString());
        tomcat.addWebapp("/employee-sig-front", new File(base, "signed-front-get").toString());
        tomcat.addWebapp("/bad-client-sales-post-sig", new File(base, "bad-client-signed-post").toString());
        tomcat.addWebapp("/bad-realm-sales-post-sig", new File(base, "bad-realm-signed-post").toString());
        tomcat.addWebapp("/sales-post-enc", new File(base, "encrypted-post").toString());
        SamlAdapterTestStrategy.uploadSP("http://localhost:8081/auth");


        tomcat.start();
        //tomcat.getServer().await();
    }

    @AfterClass
    public static void shutdownTomcat() throws Exception {
        tomcat.stop();
        tomcat.destroy();
    }

    @Rule
    public SamlAdapterTestStrategy testStrategy = new SamlAdapterTestStrategy("http://localhost:8081/auth", "http://localhost:8082", keycloakRule);

    @Test
    public void testSavedPostRequest() throws Exception {
        testStrategy.testSavedPostRequest();
    }
    @Test
    public void testPostSimpleLoginLogoutIdpInitiatedRedirectTo() {
        testStrategy.testPostSimpleLoginLogoutIdpInitiatedRedirectTo();
    }


    @Test
    public void testErrorHandling() throws Exception {
        testStrategy.testErrorHandling();
    }

    @Test
    public void testPostSimpleLoginLogout() {
        testStrategy.testPostSimpleLoginLogout();
    }

    @Test
    public void testPostSimpleLoginLogoutIdpInitiated() {
        testStrategy.testPostSimpleLoginLogoutIdpInitiated();
    }

    @Test
    public void testPostSignedLoginLogout() {
        testStrategy.testPostSignedLoginLogout();
    }

    @Test
    public void testPostSignedLoginLogoutTransientNameID() {
        testStrategy.testPostSignedLoginLogoutTransientNameID();
    }

    @Test
    public void testPostSignedLoginLogoutPersistentNameID() {
        testStrategy.testPostSignedLoginLogoutPersistentNameID();
    }

    @Test
    public void testPostSignedLoginLogoutEmailNameID() {
        testStrategy.testPostSignedLoginLogoutEmailNameID();
    }

    @Test
    public void testAttributes() throws Exception {
        testStrategy.testAttributes();
    }

    @Test
    public void testRedirectSignedLoginLogout() {
        testStrategy.testRedirectSignedLoginLogout();
    }

    @Test
    public void testRedirectSignedLoginLogoutFrontNoSSO() {
        testStrategy.testRedirectSignedLoginLogoutFrontNoSSO();
    }

    @Test
    public void testRedirectSignedLoginLogoutFront() {
        testStrategy.testRedirectSignedLoginLogoutFront();
    }

    @Test
    public void testPostEncryptedLoginLogout() {
        testStrategy.testPostEncryptedLoginLogout();
    }

    @Test
    public void testPostBadClientSignature() {
        testStrategy.testPostBadClientSignature();
    }

    @Test
    public void testPostBadRealmSignature() {
        testStrategy.testPostBadRealmSignature();
    }

    @Test
    public void testPostSimpleUnauthorized() {
        testStrategy.testPostSimpleUnauthorized( new SamlAdapterTestStrategy.CheckAuthError() {
            @Override
            public void check(WebDriver driver) {
                Assert.assertTrue(driver.getPageSource().contains("forbidden"));
            }
        });
    }

    @Test
    public void testMetadataPostSignedLoginLogout() throws Exception {
        testStrategy.testMetadataPostSignedLoginLogout();
    }

}
