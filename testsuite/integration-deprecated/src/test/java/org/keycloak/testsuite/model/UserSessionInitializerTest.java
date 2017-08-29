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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.models.UserManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.testsuite.rule.KeycloakRule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserSessionInitializerTest {


    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    private KeycloakSession session;
    private RealmModel realm;
    private UserSessionManager sessionManager;

    @Before
    public void before() {
        session = kc.startSession();
        realm = session.realms().getRealm("test");
        session.users().addUser(realm, "user1").setEmail("user1@localhost");
        session.users().addUser(realm, "user2").setEmail("user2@localhost");
        sessionManager = new UserSessionManager(session);
    }

    @After
    public void after() {
        resetSession();
        session.sessions().removeUserSessions(realm);
        UserModel user1 = session.users().getUserByUsername("user1", realm);
        UserModel user2 = session.users().getUserByUsername("user2", realm);

        UserManager um = new UserManager(session);
        um.removeUser(realm, user1);
        um.removeUser(realm, user2);
        kc.stopSession(session, true);
    }

    @Test
    public void testUserSessionInitializer() {
        UserSessionModel[] origSessions = createSessions();

        resetSession();

        // Create and persist offline sessions
        int started = Time.currentTime();
        int serverStartTime = session.getProvider(ClusterProvider.class).getClusterStartupTime();

        for (UserSessionModel origSession : origSessions) {
            UserSessionModel userSession = session.sessions().getUserSession(realm, origSession.getId());
            for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
                sessionManager.createOrUpdateOfflineSession(clientSession, userSession);
            }
        }

        resetSession();

        // Delete cache (persisted sessions are still kept)
        session.sessions().onRealmRemoved(realm);

        // Clear ispn cache to ensure initializerState is removed as well
        InfinispanConnectionProvider infinispan = session.getProvider(InfinispanConnectionProvider.class);
        infinispan.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME).clear();

        resetSession();

        ClientModel testApp = realm.getClientByClientId("test-app");
        ClientModel thirdparty = realm.getClientByClientId("third-party");
        Assert.assertEquals(0, session.sessions().getOfflineSessionsCount(realm, testApp));
        Assert.assertEquals(0, session.sessions().getOfflineSessionsCount(realm, thirdparty));

        // Load sessions from persister into infinispan/memory
        UserSessionProviderFactory userSessionFactory = (UserSessionProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserSessionProvider.class);
        userSessionFactory.loadPersistentSessions(session.getKeycloakSessionFactory(), 1, 2);

        resetSession();

        // Assert sessions are in
        testApp = realm.getClientByClientId("test-app");
        Assert.assertEquals(3, session.sessions().getOfflineSessionsCount(realm, testApp));
        Assert.assertEquals(1, session.sessions().getOfflineSessionsCount(realm, thirdparty));

        List<UserSessionModel> loadedSessions = session.sessions().getOfflineUserSessions(realm, testApp, 0, 10);
        UserSessionProviderTest.assertSessions(loadedSessions, origSessions);

        UserSessionPersisterProviderTest.assertSessionLoaded(loadedSessions, origSessions[0].getId(), session.users().getUserByUsername("user1", realm), "127.0.0.1", started, serverStartTime, "test-app", "third-party");
        UserSessionPersisterProviderTest.assertSessionLoaded(loadedSessions, origSessions[1].getId(), session.users().getUserByUsername("user1", realm), "127.0.0.2", started, serverStartTime, "test-app");
        UserSessionPersisterProviderTest.assertSessionLoaded(loadedSessions, origSessions[2].getId(), session.users().getUserByUsername("user2", realm), "127.0.0.3", started, serverStartTime, "test-app");
    }

    private AuthenticatedClientSessionModel createClientSession(ClientModel client, UserSessionModel userSession, String redirect, String state, Set<String> roles, Set<String> protocolMappers) {
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, userSession);
        if (userSession != null) clientSession.setUserSession(userSession);
        clientSession.setRedirectUri(redirect);
        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        if (roles != null) clientSession.setRoles(roles);
        if (protocolMappers != null) clientSession.setProtocolMappers(protocolMappers);
        return clientSession;
    }

    private UserSessionModel[] createSessions() {
        UserSessionModel[] sessions = new UserSessionModel[3];
        sessions[0] = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true, null, null);

        Set<String> roles = new HashSet<String>();
        roles.add("one");
        roles.add("two");

        Set<String> protocolMappers = new HashSet<String>();
        protocolMappers.add("mapper-one");
        protocolMappers.add("mapper-two");

        createClientSession(realm.getClientByClientId("test-app"), sessions[0], "http://redirect", "state", roles, protocolMappers);
        createClientSession(realm.getClientByClientId("third-party"), sessions[0], "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        sessions[1] = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.2", "form", true, null, null);
        createClientSession(realm.getClientByClientId("test-app"), sessions[1], "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        sessions[2] = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user2", realm), "user2", "127.0.0.3", "form", true, null, null);
        createClientSession(realm.getClientByClientId("test-app"), sessions[2], "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        resetSession();

        return sessions;
    }

    private void resetSession() {
        kc.stopSession(session, true);
        session = kc.startSession();
        realm = session.realms().getRealm("test");
        sessionManager = new UserSessionManager(session);
    }

}
