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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentModelTest extends AbstractModelTest {

    @Before
    public void setupEnv() {
        RealmModel realm = realmManager.createRealm("original");

        ClientModel fooClient = realm.addClient("foo-client");
        ClientModel barClient = realm.addClient("bar-client");

        RoleModel realmRole = realm.addRole("realm-role");
        RoleModel barClientRole = barClient.addRole("bar-client-role");

        ProtocolMapperModel fooMapper = new ProtocolMapperModel();
        fooMapper.setName("foo");
        fooMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        fooMapper.setProtocolMapper(UserPropertyMapper.PROVIDER_ID);
        fooMapper = fooClient.addProtocolMapper(fooMapper);

        ProtocolMapperModel barMapper = new ProtocolMapperModel();
        barMapper.setName("bar");
        barMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        barMapper.setProtocolMapper(UserPropertyMapper.PROVIDER_ID);
        barMapper = barClient.addProtocolMapper(barMapper);

        UserModel john = session.users().addUser(realm, "john");
        UserModel mary = session.users().addUser(realm, "mary");

        UserConsentModel johnFooGrant = new UserConsentModel(fooClient);
        johnFooGrant.addGrantedRole(realmRole);
        johnFooGrant.addGrantedRole(barClientRole);
        johnFooGrant.addGrantedProtocolMapper(fooMapper);
        realmManager.getSession().users().addConsent(realm, john, johnFooGrant);

        UserConsentModel johnBarGrant = new UserConsentModel(barClient);
        johnBarGrant.addGrantedProtocolMapper(barMapper);
        johnBarGrant.addGrantedRole(realmRole);

        // Update should fail as grant doesn't yet exists
        try {
            realmManager.getSession().users().updateConsent(realm, john, johnBarGrant);
            Assert.fail("Not expected to end here");
        } catch (ModelException expected) {
        }

        realmManager.getSession().users().addConsent(realm, john, johnBarGrant);

        UserConsentModel maryFooGrant = new UserConsentModel(fooClient);
        maryFooGrant.addGrantedRole(realmRole);
        maryFooGrant.addGrantedProtocolMapper(fooMapper);
        realmManager.getSession().users().addConsent(realm, mary, maryFooGrant);

        commit();
    }

    @Test
    public void basicConsentTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");
        ClientModel barClient = realm.getClientByClientId("bar-client");

        UserModel john = session.users().getUserByUsername("john", realm);
        UserModel mary = session.users().getUserByUsername("mary", realm);

        UserConsentModel johnFooConsent = realmManager.getSession().users().getConsentByClient(realm, john, fooClient.getId());
        Assert.assertEquals(johnFooConsent.getGrantedRoles().size(), 2);
        Assert.assertEquals(johnFooConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertTrue(isRoleGranted(realm, "realm-role", johnFooConsent));
        Assert.assertTrue(isRoleGranted(barClient, "bar-client-role", johnFooConsent));
        Assert.assertTrue(isMapperGranted(fooClient, "foo", johnFooConsent));

        UserConsentModel johnBarConsent = realmManager.getSession().users().getConsentByClient(realm, john, barClient.getId());
        Assert.assertEquals(johnBarConsent.getGrantedRoles().size(), 1);
        Assert.assertEquals(johnBarConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertTrue(isRoleGranted(realm, "realm-role", johnBarConsent));
        Assert.assertTrue(isMapperGranted(barClient, "bar", johnBarConsent));

        UserConsentModel maryConsent = realmManager.getSession().users().getConsentByClient(realm, mary, fooClient.getId());
        Assert.assertEquals(maryConsent.getGrantedRoles().size(), 1);
        Assert.assertEquals(maryConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertTrue(isRoleGranted(realm, "realm-role", maryConsent));
        Assert.assertFalse(isRoleGranted(barClient, "bar-client-role", maryConsent));
        Assert.assertTrue(isMapperGranted(fooClient, "foo", maryConsent));

        Assert.assertNull(realmManager.getSession().users().getConsentByClient(realm, mary, barClient.getId()));
    }

    @Test
    public void getAllConsentTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");

        UserModel john = session.users().getUserByUsername("john", realm);
        UserModel mary = session.users().getUserByUsername("mary", realm);

        List<UserConsentModel> johnConsents = realmManager.getSession().users().getConsents(realm, john);
        Assert.assertEquals(2, johnConsents.size());

        List<UserConsentModel> maryConsents = realmManager.getSession().users().getConsents(realm, mary);
        Assert.assertEquals(1, maryConsents.size());
        UserConsentModel maryConsent = maryConsents.get(0);
        Assert.assertEquals(maryConsent.getClient().getId(), fooClient.getId());
        Assert.assertEquals(maryConsent.getGrantedRoles().size(), 1);
        Assert.assertEquals(maryConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertTrue(isRoleGranted(realm, "realm-role", maryConsent));
        Assert.assertTrue(isMapperGranted(fooClient, "foo", maryConsent));
    }

    @Test
    public void updateWithRoleRemovalTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");
        UserModel john = session.users().getUserByUsername("john", realm);

        UserConsentModel johnConsent = realmManager.getSession().users().getConsentByClient(realm, john, fooClient.getId());

        // Remove foo protocol mapper from johnConsent
        ProtocolMapperModel protMapperModel = fooClient.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "foo");
        johnConsent.getGrantedProtocolMappers().remove(protMapperModel);

        // Remove realm-role and add new-realm-role to johnConsent
        RoleModel realmRole = realm.getRole("realm-role");
        johnConsent.getGrantedRoles().remove(realmRole);

        RoleModel newRealmRole = realm.addRole("new-realm-role");
        johnConsent.addGrantedRole(newRealmRole);

        realmManager.getSession().users().updateConsent(realm, john, johnConsent);

        commit();

        realm = realmManager.getRealm("original");
        fooClient = realm.getClientByClientId("foo-client");
        john = session.users().getUserByUsername("john", realm);
        johnConsent = realmManager.getSession().users().getConsentByClient(realm, john, fooClient.getId());

        Assert.assertEquals(johnConsent.getGrantedRoles().size(), 2);
        Assert.assertEquals(johnConsent.getGrantedProtocolMappers().size(), 0);
        Assert.assertFalse(isRoleGranted(realm, "realm-role", johnConsent));
        Assert.assertTrue(isRoleGranted(realm, "new-realm-role", johnConsent));
        Assert.assertFalse(isMapperGranted(fooClient, "foo", johnConsent));
    }

    @Test
    public void revokeTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");
        UserModel john = session.users().getUserByUsername("john", realm);

        realmManager.getSession().users().revokeConsentForClient(realm, john, fooClient.getId());

        commit();

        realm = realmManager.getRealm("original");
        john = session.users().getUserByUsername("john", realm);
        Assert.assertNull(realmManager.getSession().users().getConsentByClient(realm, john, fooClient.getId()));
    }

    @Test
    public void deleteUserTest() {
        // Validate user deleted without any referential constraint errors
        RealmModel realm = realmManager.getRealm("original");
        UserModel john = session.users().getUserByUsername("john", realm);
        session.users().removeUser(realm, john);
    }

    @Test
    public void deleteProtocolMapperTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");
        ProtocolMapperModel fooMapper = fooClient.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "foo");
        fooClient.removeProtocolMapper(fooMapper);

        commit();

        realm = realmManager.getRealm("original");
        fooClient = realm.getClientByClientId("foo-client");
        UserModel john = session.users().getUserByUsername("john", realm);
        UserConsentModel johnConsent = realmManager.getSession().users().getConsentByClient(realm, john, fooClient.getId());

        Assert.assertEquals(johnConsent.getGrantedRoles().size(), 2);
        Assert.assertEquals(johnConsent.getGrantedProtocolMappers().size(), 0);
        Assert.assertFalse(johnConsent.isProtocolMapperGranted(fooMapper));
    }

    @Test
    public void deleteRoleTest() {
        RealmModel realm = realmManager.getRealm("original");
        RoleModel realmRole = realm.getRole("realm-role");
        realm.removeRole(realmRole);

        commit();

        realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientByClientId("foo-client");
        ClientModel barClient = realm.getClientByClientId("bar-client");
        UserModel john = session.users().getUserByUsername("john", realm);
        UserConsentModel johnConsent = realmManager.getSession().users().getConsentByClient(realm, john, fooClient.getId());

        Assert.assertEquals(johnConsent.getGrantedRoles().size(), 1);
        Assert.assertEquals(johnConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertFalse(johnConsent.isRoleGranted(realmRole));
        Assert.assertTrue(isRoleGranted(barClient, "bar-client-role", johnConsent));
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

        UserConsentModel johnFooConsent = realmManager.getSession().users().getConsentByClient(realm, john, fooClient.getId());
        Assert.assertEquals(johnFooConsent.getGrantedRoles().size(), 1);
        Assert.assertEquals(johnFooConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertTrue(isRoleGranted(realm, "realm-role", johnFooConsent));
        Assert.assertTrue(isMapperGranted(fooClient, "foo", johnFooConsent));

        Assert.assertNull(realmManager.getSession().users().getConsentByClient(realm, john, barClient.getId()));
    }

    private boolean isRoleGranted(RoleContainerModel roleContainer, String roleName, UserConsentModel consentModel) {
        RoleModel role = roleContainer.getRole(roleName);
        return consentModel.isRoleGranted(role);
    }

    private boolean isMapperGranted(ClientModel client, String protocolMapperName, UserConsentModel consentModel) {
        ProtocolMapperModel protocolMapper = client.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, protocolMapperName);
        return consentModel.isProtocolMapperGranted(protocolMapper);
    }
}
