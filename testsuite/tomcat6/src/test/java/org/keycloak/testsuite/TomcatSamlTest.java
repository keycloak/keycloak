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
import java.util.regex.Matcher;

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

    static TomcatServer tomcat = null;

    @BeforeClass
    public static void initTomcat() throws Exception {
        URL dir = TomcatSamlTest.class.getResource("/keycloak-saml/testsaml.json");
        String baseDir = new File(dir.getFile()).getParentFile().toString();
        System.out.println("Tomcat basedir: " + baseDir);
        tomcat = new TomcatServer(8082, baseDir);
        System.setProperty("app.server.base.url", "http://localhost:8082");
        System.setProperty("my.host.name", "localhost");
        tomcat.deploySaml("/sales-post", "simple-post");
        tomcat.deploySaml("/sales-post-sig", "signed-post");
        tomcat.deploySaml("/sales-post-sig-email", "signed-post-email");
        tomcat.deploySaml("/sales-post-sig-transient", "signed-post-transient");
        tomcat.deploySaml("/sales-post-sig-persistent", "signed-post-persistent");
        tomcat.deploySaml("/sales-metadata", "signed-metadata");
        tomcat.deploySaml("/employee-sig", "signed-get");
        tomcat.deploySaml("/employee2", "mappers");
        tomcat.deploySaml("/employee-sig-front", "signed-front-get");
        tomcat.deploySaml("/bad-client-sales-post-sig", "bad-client-signed-post");
        tomcat.deploySaml("/bad-realm-sales-post-sig", "bad-realm-signed-post");
        tomcat.deploySaml("/sales-post-enc", "encrypted-post");
        SamlAdapterTestStrategy.uploadSP("http://localhost:8081/auth");


        tomcat.start();
        //tomcat.getServer().await();
    }

    @AfterClass
    public static void shutdownTomcat() throws Exception {
        tomcat.stop();
    }

    @Rule
    public SamlAdapterTestStrategy testStrategy = new SamlAdapterTestStrategy("http://localhost:8081/auth", "http://localhost:8082", keycloakRule);

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
    public void testErrorHandling() throws Exception {
        testStrategy.testErrorHandling();
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

    static String getBaseDirectory() {
        String dirPath = null;
        String relativeDirPath = "testsuite" + File.separator + "tomcat6" + File.separator + "target";

        if (System.getProperties().containsKey("maven.home")) {
            dirPath = System.getProperty("user.dir").replaceFirst("testsuite.tomcat6.*", Matcher.quoteReplacement(relativeDirPath));
        } else {
            for (String c : System.getProperty("java.class.path").split(File.pathSeparator)) {
                if (c.contains(File.separator + "testsuite" + File.separator + "tomcat6")) {
                    dirPath = c.replaceFirst("testsuite.tomcat6.*", Matcher.quoteReplacement(relativeDirPath));
                    break;
                }
            }
        }
        String absolutePath = new File(dirPath).getAbsolutePath();
        return absolutePath;
    }
}
