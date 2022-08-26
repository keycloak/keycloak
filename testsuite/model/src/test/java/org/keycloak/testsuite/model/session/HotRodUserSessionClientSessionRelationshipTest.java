/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.session;

import org.infinispan.client.hotrod.RemoteCache;
import org.junit.Test;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.hotRod.connections.DefaultHotRodConnectionProviderFactory;
import org.keycloak.models.map.storage.hotRod.connections.HotRodConnectionProvider;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodUserSessionEntity;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;

@RequireProvider(UserSessionProvider.class)
@RequireProvider(value = HotRodConnectionProvider.class, only = DefaultHotRodConnectionProviderFactory.PROVIDER_ID)
public class HotRodUserSessionClientSessionRelationshipTest extends KeycloakModelTest {

    private String realmId;
    private String CLIENT0_CLIENT_ID = "client0";

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("test");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setSsoSessionIdleTimeout(1800);
        realm.setSsoSessionMaxLifespan(36000);
        this.realmId = realm.getId();
        s.clients().addClient(realm, CLIENT0_CLIENT_ID);

        s.users().addUser(realm, "user1").setEmail("user1@localhost");
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        if (realmId != null) {
            s.realms().removeRealm(realmId);
        }
    }

    @Test
    public void testClientSessionAreRemovedOnUserSessionRemoval() {
        AtomicReference<String> uSessionId = new AtomicReference<>();
        AtomicReference<String> cSessionId = new AtomicReference<>();
        prepareSessions(uSessionId, cSessionId);

        withRealm(realmId, (session, realm) -> {
            UserSessionModel uSession = session.sessions().getUserSession(realm, uSessionId.get());
            session.sessions().removeUserSession(realm, uSession);
            return null;
        });

        assertCacheContains(remoteCache -> assertThat(remoteCache, anEmptyMap()));
    }

    @Test
    public void testSessionsAreRemovedOnUserRemoval() {
        AtomicReference<String> uSessionId = new AtomicReference<>();
        AtomicReference<String> cSessionId = new AtomicReference<>();
        prepareSessions(uSessionId, cSessionId);

        withRealm(realmId, (session, realm) -> {
            session.users().removeUser(realm, session.users().getUserByUsername(realm, "user1"));
            return null;
        });

        assertCacheContains(remoteCache -> {
            assertThat(remoteCache, anEmptyMap());
        });
    }

    @Test
    public void testSessionsAreRemovedOnRealmRemoval() {
        AtomicReference<String> uSessionId = new AtomicReference<>();
        AtomicReference<String> cSessionId = new AtomicReference<>();
        prepareSessions(uSessionId, cSessionId);

        withRealm(realmId, (session, realm) -> {
            session.realms().removeRealm(realm.getId());
            return null;
        });

        assertCacheContains(remoteCache -> {
            assertThat(remoteCache, anEmptyMap());
        });
    }

    private void assertCacheContains(Consumer<RemoteCache<String, HotRodUserSessionEntity>> checker) {
        withRealm(realmId, (session, realm) -> {
            HotRodConnectionProvider provider = session.getProvider(HotRodConnectionProvider.class);
            RemoteCache<String, HotRodUserSessionEntity> remoteCache = provider.getRemoteCache(ModelEntityUtil.getModelName(UserSessionModel.class));
            checker.accept(remoteCache);
            return null;
        });
    }

    private void prepareSessions(AtomicReference<String> uSessionId, AtomicReference<String> cSessionId) {
        withRealm(realmId, (session, realm) -> {
            UserSessionModel uSession = session.sessions().createUserSession(realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", true, null, null);
            ClientModel client = realm.getClientByClientId(CLIENT0_CLIENT_ID);

            AuthenticatedClientSessionModel cSession = session.sessions().createClientSession(realm, client, uSession);

            uSessionId.set(uSession.getId());
            cSessionId.set(cSession.getId());
            return null;
        });

        assertCacheContains(remoteCache -> {
            assertThat(remoteCache, aMapWithSize(2));
            assertThat(remoteCache.keySet(), containsInAnyOrder(uSessionId.get(), cSessionId.get()));
        });
    }
}
