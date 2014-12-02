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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OpenIDConnectService;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.adapter.AdapterTestStrategy;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.testutils.KeycloakServer;
import org.openqa.selenium.WebDriver;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.security.Principal;
import java.util.regex.Matcher;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TomcatTest {
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            RealmRepresentation representation = KeycloakServer.loadJson(getClass().getResourceAsStream("/adapter-test/demorealm.json"), RealmRepresentation.class);
            RealmModel realm = manager.importRealm(representation);
       }
    };

    static TomcatServer tomcat = null;

    @BeforeClass
    public static void initTomcat() throws Exception {
        URL dir = TomcatTest.class.getResource("/adapter-test/demorealm.json");
        String baseDir = new File(dir.getFile()).getParentFile().toString();
        System.out.println("Tomcat basedir: " + baseDir);
        tomcat = new TomcatServer(8082, baseDir);
        System.setProperty("app.server.base.url", "http://localhost:8082");
        System.setProperty("my.host.name", "localhost");
        tomcat.deploy("/customer-portal", "customer-portal");
        tomcat.deploy("/customer-db", "customer-db");
        tomcat.deploy("/product-portal", "product-portal");
        tomcat.deploy("/secure-portal", "secure-portal");
        tomcat.deploy("/session-portal", "session-portal");


        tomcat.start();
        //tomcat.getServer().await();
    }

    @AfterClass
    public static void shutdownTomcat() throws Exception {
        tomcat.stop();
    }

    @Rule
    public AdapterTestStrategy testStrategy = new AdapterTestStrategy("http://localhost:8081/auth", "http://localhost:8082", keycloakRule);

    @Test
    public void testLoginSSOAndLogout() throws Exception {
        testStrategy.testLoginSSOAndLogout();
    }

    @Test
    public void testServletRequestLogout() throws Exception {
        // can't test this.  Servlet 2.5 doesn't have logout()
        //testStrategy.testServletRequestLogout();
    }

    @Test
    public void testLoginSSOIdle() throws Exception {
        testStrategy.testLoginSSOIdle();

    }

    @Test
    public void testLoginSSOIdleRemoveExpiredUserSessions() throws Exception {
        testStrategy.testLoginSSOIdleRemoveExpiredUserSessions();
    }

    @Test
    public void testLoginSSOMax() throws Exception {
        testStrategy.testLoginSSOMax();
    }

    /**
     * KEYCLOAK-518
     * @throws Exception
     */
    @Test
    public void testNullBearerToken() throws Exception {
        testStrategy.testNullBearerToken();
    }

    /**
     * KEYCLOAK-518
     * @throws Exception
     */
    @Test
    public void testBadUser() throws Exception {
        testStrategy.testBadUser();
    }

    @Test
    public void testVersion() throws Exception {
        testStrategy.testVersion();
    }


    /**
     * KEYCLOAK-732
     *
     * @throws Throwable
     */
    @Test
    public void testSingleSessionInvalidated() throws Throwable {
        testStrategy.testSingleSessionInvalidated();
    }

    /**
     * KEYCLOAK-741
     */
    @Test
    public void testSessionInvalidatedAfterFailedRefresh() throws Throwable {
        testStrategy.testSessionInvalidatedAfterFailedRefresh();

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
