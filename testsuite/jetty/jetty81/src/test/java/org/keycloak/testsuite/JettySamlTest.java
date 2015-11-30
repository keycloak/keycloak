/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.keycloak.testsuite.adapter.AdapterTestStrategy;
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
                Assert.assertTrue(driver.getPageSource().contains("Error 403 !role"));
            }
        });
    }

    @Test
    public void testMetadataPostSignedLoginLogout() throws Exception {
        testStrategy.testMetadataPostSignedLoginLogout();
    }



}
