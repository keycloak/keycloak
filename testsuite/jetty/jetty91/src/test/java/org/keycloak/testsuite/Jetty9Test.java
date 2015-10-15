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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.adapter.AdapterTestStrategy;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Jetty9Test {
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            AdapterTestStrategy.baseAdapterTestInitialization(session, manager, adminRealm, getClass());
        }
    };

    public static Server server = null;


    @BeforeClass
    public static void initJetty() throws Exception {
        server = new Server(8082);
        List<Handler> list = new ArrayList<Handler>();
        System.setProperty("app.server.base.url", "http://localhost:8082");
        System.setProperty("my.host.name", "localhost");
        URL dir = Jetty9Test.class.getResource("/adapter-test/demorealm.json");
        File base = new File(dir.getFile()).getParentFile();
        list.add(new WebAppContext(new File(base, "customer-portal").toString(), "/customer-portal"));
        list.add(new WebAppContext(new File(base, "customer-db").toString(), "/customer-db"));
        list.add(new WebAppContext(new File(base, "customer-db-error-page").toString(), "/customer-db-error-page"));
        list.add(new WebAppContext(new File(base, "product-portal").toString(), "/product-portal"));
        list.add(new WebAppContext(new File(base, "session-portal").toString(), "/session-portal"));
        list.add(new WebAppContext(new File(base, "input-portal").toString(), "/input-portal"));
        list.add(new WebAppContext(new File(base, "secure-portal").toString(), "/secure-portal"));



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

    @Rule
    public AdapterTestStrategy testStrategy = new AdapterTestStrategy("http://localhost:8081/auth", "http://localhost:8082", keycloakRule, true);

    @Test
    public void testSavedPostRequest() throws Exception {
        testStrategy.testSavedPostRequest();
    }

    @Test
    public void testLoginSSOAndLogout() throws Exception {
        testStrategy.testLoginSSOAndLogout();
    }

    @Test
    public void testServletRequestLogout() throws Exception {
        testStrategy.testServletRequestLogout();
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
     * KEYCLOAK-1368
     * @throws Exception
     */
    @Test
    public void testNullBearerTokenCustomErrorPage() throws Exception {
        testStrategy.testNullBearerTokenCustomErrorPage();
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

    /**
     * KEYCLOAK-942
     */
    @Test
    public void testAdminApplicationLogout() throws Throwable {
        testStrategy.testAdminApplicationLogout();
    }

    /**
     * KEYCLOAK-1216
     */
    @Test
    public void testAccountManagementSessionsLogout() throws Throwable {
        testStrategy.testAccountManagementSessionsLogout();
    }
}
