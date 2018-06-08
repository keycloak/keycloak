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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.storage.client.ClientStorageProviderModel;
import org.keycloak.testsuite.federation.HardcodedClientStorageProviderFactory;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentModelTest extends AbstractModelTest {

    private ComponentModel clientStorageComponent;

    @Before
    public void setupEnv() {
        RealmModel realm = realmManager.createRealm("original");

        ClientModel fooClient = realm.addClient("foo-client");
        ClientModel barClient = realm.addClient("bar-client");

        ClientScopeModel fooScope = realm.addClientScope("foo");
        fooScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        ClientScopeModel barScope = realm.addClientScope("bar");
        fooScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        UserModel john = session.users().addUser(realm, "john");
        UserModel mary = session.users().addUser(realm, "mary");

        UserConsentModel johnFooGrant = new UserConsentModel(fooClient);
        johnFooGrant.addGrantedClientScope(fooScope);
        realmManager.getSession().users().addConsent(realm, john.getId(), johnFooGrant);

        UserConsentModel johnBarGrant = new UserConsentModel(barClient);
        johnBarGrant.addGrantedClientScope(barScope);

        // Update should fail as grant doesn't yet exists
        try {
            realmManager.getSession().users().updateConsent(realm, john.getId(), johnBarGrant);
            Assert.fail("Not expected to end here");
        } catch (ModelException expected) {
        }

        realmManager.getSession().users().addConsent(realm, john.getId(), johnBarGrant);

        UserConsentModel maryFooGrant = new UserConsentModel(fooClient);
        maryFooGrant.addGrantedClientScope(fooScope);
        realmManager.getSession().users().addConsent(realm, mary.getId(), maryFooGrant);

        ClientStorageProviderModel clientStorage = new ClientStorageProviderModel();
        clientStorage.setProviderId(HardcodedClientStorageProviderFactory.PROVIDER_ID);
        clientStorage.getConfig().putSingle(HardcodedClientStorageProviderFactory.CLIENT_ID, "hardcoded-client");
        clientStorage.getConfig().putSingle(HardcodedClientStorageProviderFactory.REDIRECT_URI, "http://localhost:8081/*");
        clientStorage.getConfig().putSingle(HardcodedClientStorageProviderFactory.CONSENT, "true");
        clientStorage.setParentId(realm.getId());
        clientStorageComponent = realm.addComponentModel(clientStorage);

        ClientModel hardcodedClient = session.realms().getClientByClientId("hardcoded-client", realm);

        Assert.assertNotNull(hardcodedClient);

        UserConsentModel maryHardcodedGrant = new UserConsentModel(hardcodedClient);
        realmManager.getSession().users().addConsent(realm, mary.getId(), maryHardcodedGrant);


        commit();
    }

    @Test
    public void basicConsentTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");
        ClientModel barClient = realm.getClientByClientId("bar-client");

        UserModel john = session.users().getUserByUsername("john", realm);
        UserModel mary = session.users().getUserByUsername("mary", realm);

        UserConsentModel johnFooConsent = realmManager.getSession().users().getConsentByClient(realm, john.getId(), fooClient.getId());
        Assert.assertEquals(johnFooConsent.getGrantedClientScopes().size(), 1);
        Assert.assertTrue(isClientScopeGranted(realm, "foo", johnFooConsent));
        Assert.assertNotNull("Created Date should be set", johnFooConsent.getCreatedDate());
        Assert.assertNotNull("Last Updated Date should be set", johnFooConsent.getLastUpdatedDate());

        UserConsentModel johnBarConsent = realmManager.getSession().users().getConsentByClient(realm, john.getId(), barClient.getId());
        Assert.assertEquals(johnBarConsent.getGrantedClientScopes().size(), 1);
        Assert.assertTrue(isClientScopeGranted(realm, "bar", johnBarConsent));
        Assert.assertNotNull("Created Date should be set", johnBarConsent.getCreatedDate());
        Assert.assertNotNull("Last Updated Date should be set", johnBarConsent.getLastUpdatedDate());

        UserConsentModel maryConsent = realmManager.getSession().users().getConsentByClient(realm, mary.getId(), fooClient.getId());
        Assert.assertEquals(maryConsent.getGrantedClientScopes().size(), 1);
        Assert.assertTrue(isClientScopeGranted(realm, "foo", maryConsent));
        Assert.assertNotNull("Created Date should be set", maryConsent.getCreatedDate());
        Assert.assertNotNull("Last Updated Date should be set", maryConsent.getLastUpdatedDate());

        ClientModel hardcodedClient = session.realms().getClientByClientId("hardcoded-client", realm);
        UserConsentModel maryHardcodedConsent = realmManager.getSession().users().getConsentByClient(realm, mary.getId(), hardcodedClient.getId());
        Assert.assertEquals(maryHardcodedConsent.getGrantedClientScopes().size(), 0);
        Assert.assertNotNull("Created Date should be set", maryHardcodedConsent.getCreatedDate());
        Assert.assertNotNull("Last Updated Date should be set", maryHardcodedConsent.getLastUpdatedDate());

        Assert.assertNull(realmManager.getSession().users().getConsentByClient(realm, mary.getId(), barClient.getId()));
        Assert.assertNull(realmManager.getSession().users().getConsentByClient(realm, john.getId(), hardcodedClient.getId()));
    }

    @Test
    public void getAllConsentTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");

        UserModel john = session.users().getUserByUsername("john", realm);
        UserModel mary = session.users().getUserByUsername("mary", realm);

        List<UserConsentModel> johnConsents = realmManager.getSession().users().getConsents(realm, john.getId());
        Assert.assertEquals(2, johnConsents.size());

        ClientModel hardcodedClient = session.realms().getClientByClientId("hardcoded-client", realm);

        List<UserConsentModel> maryConsents = realmManager.getSession().users().getConsents(realm, mary.getId());
        Assert.assertEquals(2, maryConsents.size());
        UserConsentModel maryConsent = maryConsents.get(0);
        UserConsentModel maryHardcodedConsent = maryConsents.get(1);
        if (maryConsents.get(0).getClient().getId().equals(hardcodedClient.getId())) {
            maryConsent = maryConsents.get(1);
            maryHardcodedConsent = maryConsents.get(0);

        }
        Assert.assertEquals(maryConsent.getClient().getId(), fooClient.getId());
        Assert.assertEquals(maryConsent.getGrantedClientScopes().size(), 1);
        Assert.assertTrue(isClientScopeGranted(realm, "foo", maryConsent));

        Assert.assertEquals(maryHardcodedConsent.getClient().getId(), hardcodedClient.getId());
        Assert.assertEquals(maryHardcodedConsent.getGrantedClientScopes().size(), 0);
    }

    @Test
    public void updateWithClientScopeRemovalTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");
        UserModel john = session.users().getUserByUsername("john", realm);

        UserConsentModel johnConsent = realmManager.getSession().users().getConsentByClient(realm, john.getId(), fooClient.getId());
        Assert.assertEquals(1, johnConsent.getGrantedClientScopes().size());

        // Remove foo protocol mapper from johnConsent
        ClientScopeModel fooScope = KeycloakModelUtils.getClientScopeByName(realm, "foo");
        johnConsent.getGrantedClientScopes().remove(fooScope);

        realmManager.getSession().users().updateConsent(realm, john.getId(), johnConsent);

        commit();

        realm = realmManager.getRealm("original");
        fooClient = realm.getClientByClientId("foo-client");
        john = session.users().getUserByUsername("john", realm);
        johnConsent = realmManager.getSession().users().getConsentByClient(realm, john.getId(), fooClient.getId());

        Assert.assertEquals(johnConsent.getGrantedClientScopes().size(), 0);
        Assert.assertTrue("Created date should be less than last updated date", johnConsent.getCreatedDate() < johnConsent.getLastUpdatedDate());
    }

    @Test
    public void revokeTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");
        UserModel john = session.users().getUserByUsername("john", realm);
        UserModel mary = session.users().getUserByUsername("mary", realm);

        realmManager.getSession().users().revokeConsentForClient(realm, john.getId(), fooClient.getId());
        ClientModel hardcodedClient = session.realms().getClientByClientId("hardcoded-client", realm);
        realmManager.getSession().users().revokeConsentForClient(realm, mary.getId(), hardcodedClient.getId());

        commit();

        realm = realmManager.getRealm("original");
        john = session.users().getUserByUsername("john", realm);
        Assert.assertNull(realmManager.getSession().users().getConsentByClient(realm, john.getId(), fooClient.getId()));
        mary = session.users().getUserByUsername("mary", realm);
        Assert.assertNull(realmManager.getSession().users().getConsentByClient(realm, mary.getId(), hardcodedClient.getId()));
    }

    @Test
    public void deleteUserTest() {
        // Validate user deleted without any referential constraint errors
        RealmModel realm = realmManager.getRealm("original");
        UserModel john = session.users().getUserByUsername("john", realm);
        session.users().removeUser(realm, john);
        UserModel mary = session.users().getUserByUsername("mary", realm);
        session.users().removeUser(realm, mary);
    }

    @Test
    public void deleteClientScopeTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");
        ClientScopeModel fooScope = KeycloakModelUtils.getClientScopeByName(realm, "foo");
        realm.removeClientScope(fooScope.getId());

        commit();

        realm = realmManager.getRealm("original");
        fooClient = realm.getClientByClientId("foo-client");
        UserModel john = session.users().getUserByUsername("john", realm);
        UserConsentModel johnConsent = realmManager.getSession().users().getConsentByClient(realm, john.getId(), fooClient.getId());

        Assert.assertEquals(johnConsent.getGrantedClientScopes().size(), 0);
    }

    @Test
    public void deleteClientTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel barClient = realm.getClientByClientId("bar-client");
        realm.removeClient(barClient.getId());

        commit();

        realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");
        Assert.assertNull(realm.getClientByClientId("bar-client"));

        UserModel john = session.users().getUserByUsername("john", realm);

        UserConsentModel johnFooConsent = realmManager.getSession().users().getConsentByClient(realm, john.getId(), fooClient.getId());
        Assert.assertEquals(johnFooConsent.getGrantedClientScopes().size(), 1);
        Assert.assertTrue(isClientScopeGranted(realm, "foo", johnFooConsent));

        Assert.assertNull(realmManager.getSession().users().getConsentByClient(realm, john.getId(), barClient.getId()));
    }

    @Test
    public void deleteClientStorageTest() {
        RealmModel realm = realmManager.getRealm("original");
        realm.removeComponent(clientStorageComponent);
        commit();



        realm = realmManager.getRealm("original");
        ClientModel hardcodedClient = session.realms().getClientByClientId("hardcoded-client", realm);
        Assert.assertNull(hardcodedClient);

        UserModel mary = session.users().getUserByUsername("mary", realm);

        List<UserConsentModel> maryConsents = realmManager.getSession().users().getConsents(realm, mary.getId());
        Assert.assertEquals(1, maryConsents.size());
    }

    private boolean isClientScopeGranted(RealmModel realm, String scopeName, UserConsentModel consentModel) {
        ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(realm, scopeName);
        return consentModel.isClientScopeGranted(clientScope);
    }
}
