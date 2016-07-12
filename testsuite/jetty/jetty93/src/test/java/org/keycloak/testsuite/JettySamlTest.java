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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
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
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JettySamlTest {
    @Rule
    public SamlAdapterTestStrategy testStrategy = new SamlAdapterTestStrategy("http://localhost:8081/auth", "http://localhost:8082", keycloakRule);
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            SamlAdapterTestStrategy.baseAdapterTestInitialization(session, manager, adminRealm, getClass());
        }
    };

    public static Server server = null;


    @BeforeClass
    public static void initJetty() throws Exception {
        server = new Server(8082);
        List<Handler> list = new ArrayList<Handler>();
        System.setProperty("app.server.base.url", "http://localhost:8082");
        System.setProperty("my.host.name", "localhost");
        URL dir = JettySamlTest.class.getResource("/keycloak-saml/testsaml.json");
        File base = new File(dir.getFile()).getParentFile();
        //list.add(new WebAppContext(new File(base, "customer-portal").toString(), "/customer-portal"));
        list.add(new WebAppContext(new File(base, "simple-post").toString(), "/sales-post"));
        list.add(new WebAppContext(new File(base, "simple-post2").toString(), "/sales-post2"));
        list.add(new WebAppContext(new File(base, "simple-input").toString(), "/input-portal"));
        list.add(new WebAppContext(new File(base, "signed-post").toString(), "/sales-post-sig"));
        list.add(new WebAppContext(new File(base, "signed-post-email").toString(), "/sales-post-sig-email"));
        list.add(new WebAppContext(new File(base, "signed-post-transient").toString(), "/sales-post-sig-transient"));
        list.add(new WebAppContext(new File(base, "signed-post-persistent").toString(), "/sales-post-sig-persistent"));
        list.add(new WebAppContext(new File(base, "signed-metadata").toString(), "/sales-metadata"));
        list.add(new WebAppContext(new File(base, "signed-get").toString(), "/employee-sig"));
        list.add(new WebAppContext(new File(base, "mappers").toString(), "/employee2"));
        list.add(new WebAppContext(new File(base, "signed-front-get").toString(), "/employee-sig-front"));
        list.add(new WebAppContext(new File(base, "bad-client-signed-post").toString(), "/bad-client-sales-post-sig"));
        list.add(new WebAppContext(new File(base, "bad-realm-signed-post").toString(), "/bad-realm-sales-post-sig"));
        list.add(new WebAppContext(new File(base, "encrypted-post").toString(), "/sales-post-enc"));
        SamlAdapterTestStrategy.uploadSP("http://localhost:8081/auth");



        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(list.toArray(new Handler[list.size()]));
        server.setHandler(handlers);

        server.start();
    }



    @AfterClass
    public static void shutdownJetty() throws Exception {
        try {
            server.stop();
            server.destroy();
            Thread.sleep(100);
        } catch (Exception e) {}
    }

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
        testStrategy.testPostBadRealmSignature( );
    }

    @Test
    public void testPostSimpleUnauthorized() {
        testStrategy.testPostSimpleUnauthorized( new SamlAdapterTestStrategy.CheckAuthError() {
            @Override
            public void check(WebDriver driver) {
                Assert.assertTrue(driver.getPageSource().contains("Error 403 !role"));
            }
        });
    }

    @Test
    public void testMetadataPostSignedLoginLogout() throws Exception {
        testStrategy.testMetadataPostSignedLoginLogout();
    }



}
