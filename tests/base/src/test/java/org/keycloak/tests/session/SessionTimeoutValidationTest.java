/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.session;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.annotations.TestOnServer;

import org.junit.jupiter.api.Assertions;

@KeycloakIntegrationTest
public class SessionTimeoutValidationTest {

    @InjectRealm(config = SessionTimeoutValidationRealmConfig.class)
    ManagedRealm managedRealm;


    @TestOnServer
    public void testIsSessionValid(KeycloakSession session) {
        
        // KEYCLOAK-9833 Large SSO Session Idle/SSO Session Max causes login failure
        RealmModel realm = session.realms().getRealmByName("test");
        int ssoSessionIdleTimeoutOrig = realm.getSsoSessionIdleTimeout();
        int ssoSessionMaxLifespanOrig = realm.getSsoSessionMaxLifespan();
        UserSessionModel userSessionModel =
            session.sessions().createUserSession(
                                                null, realm,
                                                session.users().getUserByUsername(realm, "user1"),
                                                "user1", "127.0.0.1", "form", false, null, null,
                                                UserSessionModel.SessionPersistenceState.PERSISTENT);

        realm.setSsoSessionIdleTimeout(Integer.MAX_VALUE);
        Assertions.assertTrue(AuthenticationManager.isSessionValid(realm, userSessionModel),
                "Session validation with large SsoSessionIdleTimeout failed");
        
        realm.setSsoSessionMaxLifespan(Integer.MAX_VALUE);
        Assertions.assertTrue(AuthenticationManager.isSessionValid(realm, userSessionModel),
                "Session validation with large SsoSessionMaxLifespan failed");
        
        realm.setSsoSessionIdleTimeout(ssoSessionIdleTimeoutOrig);
        realm.setSsoSessionMaxLifespan(ssoSessionMaxLifespanOrig);
    }

    private static class SessionTimeoutValidationRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name("test");
            realm.addUser("user1");
            return realm;
        }
    }
}
