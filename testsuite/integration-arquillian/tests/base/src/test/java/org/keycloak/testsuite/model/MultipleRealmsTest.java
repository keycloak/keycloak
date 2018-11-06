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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.services.managers.RealmManager;

import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MultipleRealmsTest extends AbstractTestRealmKeycloakTest {


    @Deployment
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class, MultipleRealmsTest.class)
                .addPackages(true,
                        "org.keycloak.testsuite",
                        "org.keycloak.testsuite.model");
    }

    private static RealmModel realm1;
    private static RealmModel realm2;
    private static Boolean isInitedP = false;

    public static void initState(KeycloakSession session) {

        if (!isInitedP) {
            RealmModel realm1 = session.realms().createRealm("id1", "realm1");
            RealmModel realm2 = session.realms().createRealm("id2", "realm2");

            createObjects(session, realm1);
            createObjects(session, realm2);
            isInitedP = true;
        }
    }

    public static void createObjects(KeycloakSession session, RealmModel realm) {
        ClientModel app1 = realm.addClient("app1");
        realm.addClient("app2");

        session.users().addUser(realm, "user1");
        session.users().addUser(realm, "user2");

        realm.addRole("role1");
        realm.addRole("role2");

        app1.addRole("app1Role1");
        app1.addScopeMapping(realm.getRole("role1"));

        realm.addClient("cl1");
    }

    @Test
    @ModelTest
    public void testUsers(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionTU) -> {


            RealmModel realm1 = sessionTU.realms().createRealm("id1", "realm1");
            RealmModel realm2 = sessionTU.realms().createRealm("id2", "realm2");

            createObjects(sessionTU, realm1);
            createObjects(sessionTU, realm2);

            UserModel r1user1 = sessionTU.users().getUserByUsername("user1", realm1);
            UserModel r2user1 = sessionTU.users().getUserByUsername("user1", realm2);
            Assert.assertEquals(r1user1.getUsername(), r2user1.getUsername());
            Assert.assertNotEquals(r1user1.getId(), r2user1.getId());

            // Test password
            sessionTU.userCredentialManager().updateCredential(realm1, r1user1, UserCredentialModel.password("pass1"));
            sessionTU.userCredentialManager().updateCredential(realm2, r2user1, UserCredentialModel.password("pass2"));

            Assert.assertTrue(sessionTU.userCredentialManager().isValid(realm1, r1user1, UserCredentialModel.password("pass1")));
            Assert.assertFalse(sessionTU.userCredentialManager().isValid(realm1, r1user1, UserCredentialModel.password("pass2")));
            Assert.assertFalse(sessionTU.userCredentialManager().isValid(realm2, r2user1, UserCredentialModel.password("pass1")));
            Assert.assertTrue(sessionTU.userCredentialManager().isValid(realm2, r2user1, UserCredentialModel.password("pass2")));

            // Test searching
            Assert.assertEquals(2, sessionTU.users().searchForUser("user", realm1).size());

            realm1 = sessionTU.realms().getRealm("id1");
            realm2 = sessionTU.realms().getRealm("id2");

            sessionTU.users().removeUser(realm1, r1user1);
            UserModel user2 = sessionTU.users().getUserByUsername("user2", realm1);
            sessionTU.users().removeUser(realm1, user2);
            Assert.assertEquals(0, sessionTU.users().searchForUser("user", realm1).size());
            Assert.assertEquals(2, sessionTU.users().searchForUser("user", realm2).size());


            UserModel user1 = sessionTU.users().getUserByUsername("user1", realm1);
            UserModel user1a = sessionTU.users().getUserByUsername("user1", realm2);

            UserManager um = new UserManager(session);
            if (user1 != null) {
                um.removeUser(realm1, user1);
            }
            if (user1a != null) {
                um.removeUser(realm2, user1a);
            }
            sessionTU.realms().removeRealm("realm1");
            sessionTU.realms().removeRealm("realm2");
        });
    }

    @Test
    @ModelTest
    public void testGetById(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionBI) -> {

            RealmModel realm1 = sessionBI.realms().getRealm("id1");
            RealmModel realm2 = sessionBI.realms().getRealm("id2");

            Assert.assertEquals(realm1, sessionBI.realms().getRealm("id1"));
            Assert.assertEquals(realm1, sessionBI.realms().getRealmByName("realm1"));
            Assert.assertEquals(realm2, sessionBI.realms().getRealm("id2"));
            Assert.assertEquals(realm2, sessionBI.realms().getRealmByName("realm2"));

            ClientModel r1app1 = realm1.getClientByClientId("app1");
            ClientModel r1app2 = realm1.getClientByClientId("app2");
            ClientModel r2app1 = realm2.getClientByClientId("app1");
            ClientModel r2app2 = realm2.getClientByClientId("app2");

            Assert.assertEquals(r1app1, realm1.getClientById(r1app1.getId()));
            Assert.assertNull(realm2.getClientById(r1app1.getId()));

            ClientModel r2cl1 = realm2.getClientByClientId("cl1");
            Assert.assertEquals(r2cl1.getId(), realm2.getClientById(r2cl1.getId()).getId());
            Assert.assertNull(realm1.getClientByClientId(r2cl1.getId()));

            RoleModel r1App1Role = r1app1.getRole("app1Role1");
            Assert.assertEquals(r1App1Role, realm1.getRoleById(r1App1Role.getId()));
            Assert.assertNull(realm2.getRoleById(r1App1Role.getId()));

            RoleModel r2Role1 = realm2.getRole("role2");
            Assert.assertNull(realm1.getRoleById(r2Role1.getId()));
            Assert.assertEquals(r2Role1, realm2.getRoleById(r2Role1.getId()));


            UserModel user1 = sessionBI.users().getUserByUsername("user1", realm1);
            UserModel user1a = sessionBI.users().getUserByUsername("user1", realm2);

            UserManager um = new UserManager(session);
            if (user1 != null) {
                um.removeUser(realm1, user1);
            }
            if (user1a != null) {
                um.removeUser(realm2, user1a);
            }
            sessionBI.realms().removeRealm("realm1");
            sessionBI.realms().removeRealm("realm2");
        });
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {


    }
}
