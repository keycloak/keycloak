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
