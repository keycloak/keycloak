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

package org.keycloak.testsuite.login;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

@AuthServerContainerExclude(REMOTE)
public class LoginTimeoutValidationTest extends AbstractTestRealmKeycloakTest {

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
    public  void testIsLoginTimeoutValid(KeycloakSession keycloakSession) {
        
        RealmModel realm = keycloakSession.realms().getRealmByName("test");
        UserSessionModel userSession =
            keycloakSession.sessions().createUserSession(
                                                 realm,
                                                 keycloakSession.users().getUserByUsername(realm, "user1"),
                                                 "user1", "127.0.0.1", "form", true, null, null
                                                 );
        ClientModel client = realm.getClientByClientId("account");
        AuthenticationSessionModel authSession = keycloakSession.authenticationSessions().createRootAuthenticationSession(realm)
            .createAuthenticationSession(client);
        ClientSessionCode clientSessionCode = new ClientSessionCode(keycloakSession, realm, authSession);

        /*
         * KEYCLOAK-10636 Large Login timeout causes login failure
         * realm > Realm setting > Tokens > Login timeout
         */
        int accessCodeLifespanLoginOrig = realm.getAccessCodeLifespanLogin(); // Login timeout
        realm.setAccessCodeLifespanLogin(Integer.MAX_VALUE);
        Assert.assertTrue("Login validataion with large Login Timeout failed",
                          clientSessionCode.isActionActive(ClientSessionCode.ActionType.LOGIN));
        realm.setAccessCodeLifespanLogin(accessCodeLifespanLoginOrig);

        /*
         * KEYCLOAK-10637 Large Login Action timeout causes login failure
         * realm > Realm setting > Tokens > Login Action timeout
         */
        int accessCodeLifespanUserActionOrig = realm.getAccessCodeLifespanUserAction(); // Login Action timeout
        realm.setAccessCodeLifespanUserAction(Integer.MAX_VALUE);
        Assert.assertTrue("Login validataion with large Login Action Timeout failed",
                          clientSessionCode.isActionActive(ClientSessionCode.ActionType.USER));
        realm.setAccessCodeLifespanUserAction(accessCodeLifespanUserActionOrig);
    }
}
