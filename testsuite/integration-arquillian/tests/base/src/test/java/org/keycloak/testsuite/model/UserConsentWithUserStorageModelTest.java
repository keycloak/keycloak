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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UserConsentManager;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.client.ClientStorageProviderModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.federation.HardcodedClientStorageProviderFactory;
import org.keycloak.testsuite.federation.UserMapStorageFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentWithUserStorageModelTest extends AbstractTestRealmKeycloakTest {

    private static ComponentModel clientStorageComponent;

    @Before
    public void before() {
        testingClient.server().run(UserConsentWithUserStorageModelTest::setupEnv);
    }

    @After
    public void after() {
        testingClient.server().run(session -> {

            RealmManager realmManager = new RealmManager(session);
            RealmModel realm = realmManager.getRealmByName("original");
            session.getContext().setRealm(realm);

            if (realm != null) {

                session.sessions().removeUserSessions(realm);
                UserModel user = session.users().getUserByUsername(realm, "user");
                UserModel user1 = session.users().getUserByUsername(realm, "user1");
                UserModel user2 = session.users().getUserByUsername(realm, "user2");
                UserModel user3 = session.users().getUserByUsername(realm, "user3");

                UserManager um = new UserManager(session);
                if (user != null) {
                    um.removeUser(realm, user);
                }
                if (user1 != null) {
                    um.removeUser(realm, user1);
                }
                if (user2 != null) {
                    um.removeUser(realm, user2);
                }
                if (user3 != null) {
                    um.removeUser(realm, user3);
                }
                realmManager.removeRealm(realm);
            }
        });
    }

    public static void setupEnv(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionSetUpEnv) -> {
            KeycloakSession currentSession = sessionSetUpEnv;

            RealmManager realmManager = new RealmManager(currentSession);
            RealmModel realm = realmManager.createRealm("original");
            currentSession.getContext().setRealm(realm);

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setName("memory");
            model.setPriority(0);
            model.setProviderId(UserMapStorageFactory.PROVIDER_ID);
            model.setParentId(realm.getId());
            model.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));
            realm.addComponentModel(model);

            ClientModel fooClient = realm.addClient("foo-client");
            ClientModel barClient = realm.addClient("bar-client");

            ClientScopeModel fooScope = realm.addClientScope("foo");
            fooScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            ClientScopeModel barScope = realm.addClientScope("bar");
            fooScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            UserModel john = currentSession.users().addUser(realm, "john");
            UserModel mary = currentSession.users().addUser(realm, "mary");

            UserConsentModel johnFooGrant = new UserConsentModel(fooClient);
            johnFooGrant.addGrantedClientScope(fooScope);
            UserConsentManager.addConsent(realmManager.getSession(), realm, john, johnFooGrant);

            UserConsentModel johnBarGrant = new UserConsentModel(barClient);
            johnBarGrant.addGrantedClientScope(barScope);

            // Update should fail as grant doesn't yet exists
            try {
                UserConsentManager.updateConsent(currentSession, realm, john, johnBarGrant);
                Assert.fail("Not expected to end here");
            } catch (ModelException expected) {
            }

            UserConsentManager.addConsent(realmManager.getSession(), realm, john, johnBarGrant);

            UserConsentModel maryFooGrant = new UserConsentModel(fooClient);
            maryFooGrant.addGrantedClientScope(fooScope);
            UserConsentManager.addConsent(realmManager.getSession(), realm, mary, maryFooGrant);

            ClientStorageProviderModel clientStorage = new ClientStorageProviderModel();
            clientStorage.setProviderId(HardcodedClientStorageProviderFactory.PROVIDER_ID);
            clientStorage.getConfig().putSingle(HardcodedClientStorageProviderFactory.CLIENT_ID, "hardcoded-client");
            clientStorage.getConfig().putSingle(HardcodedClientStorageProviderFactory.REDIRECT_URI, "http://localhost:8081/*");
            clientStorage.getConfig().putSingle(HardcodedClientStorageProviderFactory.CONSENT, "true");
            clientStorage.setParentId(realm.getId());
            clientStorageComponent = realm.addComponentModel(clientStorage);

            ClientModel hardcodedClient = currentSession.clients().getClientByClientId(realm, "hardcoded-client");

            Assert.assertNotNull(hardcodedClient);

            UserConsentModel maryHardcodedGrant = new UserConsentModel(hardcodedClient);
            UserConsentManager.addConsent(realmManager.getSession(), realm, mary, maryHardcodedGrant);
        });
    }

    @Test
    @ModelTest
    public void basicConsentTest(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSessionCT) -> {
            KeycloakSession currentSession = currentSessionCT;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientModel fooClient = realm.getClientByClientId("foo-client");
            ClientModel barClient = realm.getClientByClientId("bar-client");

            UserModel john = currentSessionCT.users().getUserByUsername(realm, "john");
            UserModel mary = currentSessionCT.users().getUserByUsername(realm, "mary");

            UserConsentModel johnFooConsent = UserConsentManager.getConsentByClient(currentSession, realm, john, fooClient.getId());
            Assert.assertEquals(1, johnFooConsent.getGrantedClientScopes().size());
            Assert.assertTrue(isClientScopeGranted(realm, "foo", johnFooConsent));
            Assert.assertNotNull("Created Date should be set", johnFooConsent.getCreatedDate());
            Assert.assertNotNull("Last Updated Date should be set", johnFooConsent.getLastUpdatedDate());

            UserConsentModel johnBarConsent = UserConsentManager.getConsentByClient(currentSession, realm, john, barClient.getId());
            Assert.assertEquals(1, johnBarConsent.getGrantedClientScopes().size());
            Assert.assertTrue(isClientScopeGranted(realm, "bar", johnBarConsent));
            Assert.assertNotNull("Created Date should be set", johnBarConsent.getCreatedDate());
            Assert.assertNotNull("Last Updated Date should be set", johnBarConsent.getLastUpdatedDate());

            UserConsentModel maryConsent = UserConsentManager.getConsentByClient(currentSession, realm, mary, fooClient.getId());
            Assert.assertEquals(1, maryConsent.getGrantedClientScopes().size());
            Assert.assertTrue(isClientScopeGranted(realm, "foo", maryConsent));
            Assert.assertNotNull("Created Date should be set", maryConsent.getCreatedDate());
            Assert.assertNotNull("Last Updated Date should be set", maryConsent.getLastUpdatedDate());

            ClientModel hardcodedClient = currentSessionCT.clients().getClientByClientId(realm, "hardcoded-client");
            UserConsentModel maryHardcodedConsent = UserConsentManager.getConsentByClient(currentSession, realm, mary, hardcodedClient.getId());
            Assert.assertEquals(0, maryHardcodedConsent.getGrantedClientScopes().size());
            Assert.assertNotNull("Created Date should be set", maryHardcodedConsent.getCreatedDate());
            Assert.assertNotNull("Last Updated Date should be set", maryHardcodedConsent.getLastUpdatedDate());

            Assert.assertNull(UserConsentManager.getConsentByClient(currentSession, realm, mary, barClient.getId()));
            Assert.assertNull(UserConsentManager.getConsentByClient(currentSession, realm, john, hardcodedClient.getId()));
        });
    }

    @Test
    @ModelTest
    public void getAllConsentTest(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession currentSessionACT) -> {
            KeycloakSession currentSession = currentSessionACT;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientModel fooClient = realm.getClientByClientId("foo-client");

            UserModel john = currentSessionACT.users().getUserByUsername(realm, "john");
            UserModel mary = currentSessionACT.users().getUserByUsername(realm, "mary");

            Assert.assertEquals(2, UserConsentManager.getConsentsStream(currentSession, realm, john).count());

            ClientModel hardcodedClient = currentSessionACT.clients().getClientByClientId(realm, "hardcoded-client");

            List<UserConsentModel> maryConsents = UserConsentManager.getConsentsStream(currentSession, realm, mary)
                    .collect(Collectors.toList());
            Assert.assertEquals(2, maryConsents.size());
            UserConsentModel maryConsent = maryConsents.get(0);
            UserConsentModel maryHardcodedConsent = maryConsents.get(1);
            if (maryConsents.get(0).getClient().getId().equals(hardcodedClient.getId())) {
                maryConsent = maryConsents.get(1);
                maryHardcodedConsent = maryConsents.get(0);

            }
            Assert.assertEquals(maryConsent.getClient().getId(), fooClient.getId());
            Assert.assertEquals(1, maryConsent.getGrantedClientScopes().size());
            Assert.assertTrue(isClientScopeGranted(realm, "foo", maryConsent));

            Assert.assertEquals(maryHardcodedConsent.getClient().getId(), hardcodedClient.getId());
            Assert.assertEquals(0, maryHardcodedConsent.getGrantedClientScopes().size());
        });
    }

    @Test
    @ModelTest
    public void updateWithClientScopeRemovalTest(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionScopeRemoval1) -> {
            KeycloakSession currentSession = sessionScopeRemoval1;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientModel fooClient = realm.getClientByClientId("foo-client");
            UserModel john = currentSession.users().getUserByUsername(realm, "john");

            UserConsentModel johnConsent = UserConsentManager.getConsentByClient(currentSession, realm, john, fooClient.getId());
            Assert.assertEquals(1, johnConsent.getGrantedClientScopes().size());

            // Remove foo protocol mapper from johnConsent
            ClientScopeModel fooScope = KeycloakModelUtils.getClientScopeByName(realm, "foo");
            johnConsent.getGrantedClientScopes().remove(fooScope);

            UserConsentManager.updateConsent(currentSession, realm, john, johnConsent);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionScopeRemoval2) -> {
            KeycloakSession currentSession = sessionScopeRemoval2;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientModel fooClient = realm.getClientByClientId("foo-client");
            UserModel john = currentSession.users().getUserByUsername(realm, "john");
            UserConsentModel johnConsent = UserConsentManager.getConsentByClient(currentSession, realm, john, fooClient.getId());

            Assert.assertEquals(0, johnConsent.getGrantedClientScopes().size());
            Assert.assertTrue("Created date should be less than last updated date", johnConsent.getCreatedDate() < johnConsent.getLastUpdatedDate());
        });
    }

    @Test
    @ModelTest
    public void revokeTest(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRevoke1) -> {
            KeycloakSession currentSession = sessionRevoke1;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientModel fooClient = realm.getClientByClientId("foo-client");
            UserModel john = currentSession.users().getUserByUsername(realm, "john");
            UserModel mary = currentSession.users().getUserByUsername(realm, "mary");

            UserConsentManager.revokeConsentForClient(currentSession, realm, john, fooClient.getId());
            ClientModel hardcodedClient = currentSession.clients().getClientByClientId(realm, "hardcoded-client");
            UserConsentManager.revokeConsentForClient(currentSession, realm, mary, hardcodedClient.getId());
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRevoke2) -> {
            KeycloakSession currentSession = sessionRevoke2;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientModel fooClient = realm.getClientByClientId("foo-client");
            ClientModel hardcodedClient = currentSession.clients().getClientByClientId(realm, "hardcoded-client");

            UserModel john = currentSession.users().getUserByUsername(realm, "john");
            Assert.assertNull(UserConsentManager.getConsentByClient(currentSession, realm, john, fooClient.getId()));

            UserModel mary = currentSession.users().getUserByUsername(realm, "mary");
            Assert.assertNull(UserConsentManager.getConsentByClient(currentSession, realm, mary, hardcodedClient.getId()));
        });
    }

    @Test
    @ModelTest
    public void deleteUserTest(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionDelete) -> {
            // Validate user deleted without any referential constraint errors
            KeycloakSession currentSession = sessionDelete;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            UserModel john = currentSession.users().getUserByUsername(realm, "john");
            currentSession.users().removeUser(realm, john);
            UserModel mary = currentSession.users().getUserByUsername(realm, "mary");
            currentSession.users().removeUser(realm, mary);
        });
    }

    @Test
    @ModelTest
    public void deleteClientScopeTest(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesDelClScope1) -> {
            KeycloakSession currentSession = sesDelClScope1;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientScopeModel fooScope = KeycloakModelUtils.getClientScopeByName(realm, "foo");
            realm.removeClientScope(fooScope.getId());
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesDelClScope2) -> {
            KeycloakSession currentSession = sesDelClScope2;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientModel fooClient = realm.getClientByClientId("foo-client");
            UserModel john = currentSession.users().getUserByUsername(realm, "john");
            UserConsentModel johnConsent = UserConsentManager.getConsentByClient(currentSession, realm, john, fooClient.getId());

            Assert.assertEquals(0, johnConsent.getGrantedClientScopes().size());
        });
    }

    @Test
    @ModelTest
    public void deleteClientTest(KeycloakSession session) {
        AtomicReference<String> barClientID = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesDelClient1) -> {
            KeycloakSession currentSession = sesDelClient1;
            RealmManager realmManager = new RealmManager(currentSession);
            RealmModel realm = realmManager.getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientModel barClient = realm.getClientByClientId("bar-client");
            barClientID.set(barClient.getId());

            realm.removeClient(barClient.getId());
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesDelClient2) -> {
            KeycloakSession currentSession = sesDelClient2;
            RealmManager realmManager = new RealmManager(currentSession);
            RealmModel realm = realmManager.getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientModel fooClient = realm.getClientByClientId("foo-client");
            Assert.assertNull(realm.getClientByClientId("bar-client"));

            UserModel john = realmManager.getSession().users().getUserByUsername(realm, "john");

            UserConsentModel johnFooConsent = UserConsentManager.getConsentByClient(realmManager.getSession(), realm, john, fooClient.getId());
            Assert.assertEquals(1, johnFooConsent.getGrantedClientScopes().size());
            Assert.assertTrue(isClientScopeGranted(realm, "foo", johnFooConsent));

            Assert.assertNull(UserConsentManager.getConsentByClient(realmManager.getSession(), realm, john, barClientID.get()));
        });
    }

    @Test
    @ModelTest
    public void deleteClientStorageTest(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesDelClientStore1) -> {
            KeycloakSession currentSession = sesDelClientStore1;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            realm.removeComponent(clientStorageComponent);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sesDelClientStore2) -> {
            KeycloakSession currentSession = sesDelClientStore2;
            RealmModel realm = currentSession.realms().getRealmByName("original");
            currentSession.getContext().setRealm(realm);

            ClientModel hardcodedClient = currentSession.clients().getClientByClientId(realm, "hardcoded-client");
            Assert.assertNull(hardcodedClient);

            UserModel mary = currentSession.users().getUserByUsername(realm, "mary");
            Assert.assertEquals(1, UserConsentManager.getConsentsStream(currentSession, realm, mary).count());
        });
    }

    private boolean isClientScopeGranted(RealmModel realm, String scopeName, UserConsentModel consentModel) {
        ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(realm, scopeName);
        return consentModel.isClientScopeGranted(clientScope);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }
}
