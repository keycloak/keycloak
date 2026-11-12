/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.model;

import org.keycloak.authentication.jpa.AuthenticationSessionExpirationAction;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.models.utils.SessionExpiration;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.remote.providers.timeoffset.InfinispanTimeUtil;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks that the authentication session expiration only has work for realms with authentication sessions.
 */
@KeycloakIntegrationTest
public class AuthenticationSessionExpirationTest {

    private static final String REALM_NAME = "auth-expiration-work";

    @InjectRealm(config = ExpirationRealmConfig.class)
    ManagedRealm realm;

    @TestOnServer
    public void emptyRealmHasNoWork(KeycloakSession session) {
        assumeTrue(Profile.isFeatureEnabled(Profile.Feature.STATELESS));
        String realmId = session.realms().getRealmByName(REALM_NAME).getId();

        assertFalse(hasWork(session, realmId));
    }

    @TestOnServer
    public void sessionInPreviouslyEmptyRealmIsStillExpired(KeycloakSession session) {
        assumeTrue(Profile.isFeatureEnabled(Profile.Feature.STATELESS));
        RealmModel realmModel = session.realms().getRealmByName(REALM_NAME);
        String realmId = realmModel.getId();
        InfinispanTimeUtil.enableTestingTimeService(session);
        try {
            assertFalse(hasWork(session, realmId));

            String[] sessionId = new String[1];
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session1 -> {
                RealmModel r = session1.realms().getRealm(realmId);
                sessionId[0] = session1.authenticationSessions().createRootAuthenticationSession(r).getId();
            });

            assertTrue(hasWork(session, realmId));

            Time.setOffset(SessionExpiration.getAuthSessionLifespan(realmModel) + 60);
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session1 ->
                    AuthenticationSessionExpirationAction.INSTANCE.removeExpired(session1, realmId, Time.currentTime(), Integer.MAX_VALUE, removed -> {
                    }));

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session1 -> {
                assertNull(session1.getProvider(JpaConnectionProvider.class).getEntityManager()
                        .find(org.keycloak.authentication.jpa.RootAuthenticationSessionEntity.class, sessionId[0]));
            });
            assertFalse(hasWork(session, realmId));
        } finally {
            Time.setOffset(0);
            session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
            InfinispanTimeUtil.disableTestingTimeService(session);
        }
    }

    private static boolean hasWork(KeycloakSession session, String realmId) {
        return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), session1 ->
                AuthenticationSessionExpirationAction.INSTANCE.hasWork(session1, realmId));
    }

    private static class ExpirationRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(REALM_NAME);
        }
    }
}
