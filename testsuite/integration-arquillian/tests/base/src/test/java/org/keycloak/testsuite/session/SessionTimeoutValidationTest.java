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

package org.keycloak.testsuite.session;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SessionTimeoutValidationTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    
    @Before
    public  void before() {
        testingClient.server().run( session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            session.users().addUser(realm, "user1");
        });
    }
    

    @After
    public void after() {
        testingClient.server().run( session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            session.sessions().removeUserSessions(realm);
            UserModel user1 = session.users().getUserByUsername(realm, "user1");

            UserManager um = new UserManager(session);
            if (user1 != null) {
                um.removeUser(realm, user1);
            }
        });
    }
    

    @Test
    @ModelTest
    public  void testIsSessionValid(KeycloakSession session) {
        
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
        Assert.assertTrue("Session validataion with large SsoSessionIdleTimeout failed",
                          AuthenticationManager.isSessionValid(realm, userSessionModel));
        
        realm.setSsoSessionMaxLifespan(Integer.MAX_VALUE);
        Assert.assertTrue("Session validataion with large SsoSessionMaxLifespan failed",
                          AuthenticationManager.isSessionValid(realm, userSessionModel));
        
        realm.setSsoSessionIdleTimeout(ssoSessionIdleTimeoutOrig);
        realm.setSsoSessionMaxLifespan(ssoSessionMaxLifespanOrig);
    }
}
