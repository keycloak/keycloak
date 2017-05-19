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

package org.keycloak.testsuite.model;

import java.util.List;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.testsuite.rule.KeycloakRule;

/**
 * Run test with shared MySQL DB and in cluster:
 *
 * -Dkeycloak.connectionsJpa.url=jdbc:mysql://localhost/keycloak -Dkeycloak.connectionsJpa.driver=com.mysql.jdbc.Driver -Dkeycloak.connectionsJpa.user=keycloak
 * -Dkeycloak.connectionsJpa.password=keycloak -Dkeycloak.connectionsInfinispan.clustered=true
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Ignore
public class ClusterSessionCleanerTest {

    protected static final Logger logger = Logger.getLogger(ClusterSessionCleanerTest.class);

    private static final String REALM_NAME = "test";

    @ClassRule
    public static KeycloakRule server1 = new KeycloakRule();

    @ClassRule
    public static KeycloakRule server2 = new KeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(8082);
        }

        @Override
        protected void importRealm() {
        }

        @Override
        protected void removeTestRealms() {
        }

    };

    @Test
    public void testClusterPeriodicSessionCleanups() throws Exception {
        // Add some userSessions on server1
        KeycloakSession session1 = server1.startSession();
        RealmModel realm1 = session1.realms().getRealmByName(REALM_NAME);
        UserModel user1 = session1.users().getUserByUsername("test-user@localhost", realm1);
        for (int i=0 ; i<15 ; i++) {
            session1.sessions().createUserSession("123", realm1, user1, user1.getUsername(), "127.0.0.1", "form", true, null, null);
        }
        session1 = commit(server1, session1);

        // Add some userSessions on server2
        KeycloakSession session2 = server2.startSession();
        RealmModel realm2 = session2.realms().getRealmByName(REALM_NAME);
        UserModel user2 = session2.users().getUserByUsername("test-user@localhost", realm2);
        // Check we are really in cluster (same user ids)
        Assert.assertEquals(user2.getId(), user1.getId());

        for (int i=0 ; i<15 ; i++) {
            session2.sessions().createUserSession("456", realm2, user2, user2.getUsername(), "127.0.0.1", "form", true, null, null);
        }
        session2 = commit(server2, session2);

        // Assert sessions on both nodes
        List<UserSessionModel> sessions1 = getSessions(session1);
        List<UserSessionModel> sessions2 = getSessions(session2);
        Assert.assertEquals(30, sessions1.size());
        Assert.assertEquals(30, sessions2.size());
        logger.info("Before offset: sessions1 : " + sessions1.size());
        logger.info("Before offset: sessions2 : " + sessions2.size());


        // set Time offset and run periodic cleaner on server1
        Time.setOffset(999999);
        realm1 = session1.realms().getRealmByName(REALM_NAME);
        session1.sessions().removeExpired(realm1);
        session1 = commit(server1, session1);

        // Ensure some sessions still there
        sessions1 = getSessions(session1);
        sessions2 = getSessions(session2);
        logger.info("After server1 periodic clean: sessions1 : " + sessions1.size());
        logger.info("After server1 periodic clean: sessions2 : " + sessions2.size());


        // Run periodic cleaner on server2
        realm2 = session2.realms().getRealmByName(REALM_NAME);
        session2.sessions().removeExpired(realm2);
        session2 = commit(server1, session2);

        // Ensure there are no sessions on server1 or server2
        sessions1 = getSessions(session1);
        sessions2 = getSessions(session2);
        Assert.assertTrue(sessions1.isEmpty());
        Assert.assertTrue(sessions2.isEmpty());
        logger.info("After both periodic cleans: sessions1 : " + sessions1.size());
        logger.info("After both periodic cleans: sessions2 : " + sessions2.size());
    }

    private List<UserSessionModel> getSessions(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        UserModel user = session.users().getUserByUsername("test-user@localhost", realm);
        return session.sessions().getUserSessions(realm, user);
    }

    private KeycloakSession commit(KeycloakRule rule, KeycloakSession session) throws Exception {
        session.getTransactionManager().commit();
        session.close();
        return rule.startSession();
    }

}
