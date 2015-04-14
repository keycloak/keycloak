package org.keycloak.testsuite.model;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GrantedConsentModel;
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
public class GrantedConsentModelTest extends AbstractModelTest {

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

        GrantedConsentModel johnFooGrant = new GrantedConsentModel(fooClient.getId());
        johnFooGrant.addGrantedRole(realmRole.getId());
        johnFooGrant.addGrantedRole(barClientRole.getId());
        johnFooGrant.addGrantedProtocolMapper(fooMapper.getId());
        john.addGrantedConsent(johnFooGrant);

        GrantedConsentModel johnBarGrant = new GrantedConsentModel(barClient.getId());
        johnBarGrant.addGrantedProtocolMapper(barMapper.getId());
        johnBarGrant.addGrantedRole(realmRole.getId());

        // Update should fail as grant doesn't yet exists
        try {
            john.updateGrantedConsent(johnBarGrant);
            Assert.fail("Not expected to end here");
        } catch (ModelException expected) {
        }

        john.addGrantedConsent(johnBarGrant);

        GrantedConsentModel maryFooGrant = new GrantedConsentModel(fooClient.getId());
        maryFooGrant.addGrantedRole(realmRole.getId());
        maryFooGrant.addGrantedProtocolMapper(fooMapper.getId());
        mary.addGrantedConsent(maryFooGrant);

        commit();
    }

    @Test
    public void basicConsentTest() {
        RealmModel realm = realmManager.getRealm("original");
        Map<String, ClientModel> clients = realm.getClientNameMap();
        ClientModel fooClient = clients.get("foo-client");
        ClientModel barClient = clients.get("bar-client");

        UserModel john = session.users().getUserByUsername("john", realm);
        UserModel mary = session.users().getUserByUsername("mary", realm);

        GrantedConsentModel johnFooConsent = john.getGrantedConsentByClient(fooClient.getId());
        Assert.assertEquals(johnFooConsent.getGrantedRoles().size(), 2);
        Assert.assertEquals(johnFooConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertTrue(isRoleGranted(realm, "realm-role", johnFooConsent));
        Assert.assertTrue(isRoleGranted(barClient, "bar-client-role", johnFooConsent));
        Assert.assertTrue(isMapperGranted(fooClient, "foo", johnFooConsent));

        GrantedConsentModel johnBarConsent = john.getGrantedConsentByClient(barClient.getId());
        Assert.assertEquals(johnBarConsent.getGrantedRoles().size(), 1);
        Assert.assertEquals(johnBarConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertTrue(isRoleGranted(realm, "realm-role", johnBarConsent));
        Assert.assertTrue(isMapperGranted(barClient, "bar", johnBarConsent));

        GrantedConsentModel maryConsent = mary.getGrantedConsentByClient(fooClient.getId());
        Assert.assertEquals(maryConsent.getGrantedRoles().size(), 1);
        Assert.assertEquals(maryConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertTrue(isRoleGranted(realm, "realm-role", maryConsent));
        Assert.assertFalse(isRoleGranted(barClient, "bar-client-role", maryConsent));
        Assert.assertTrue(isMapperGranted(fooClient, "foo", maryConsent));

        Assert.assertNull(mary.getGrantedConsentByClient(barClient.getId()));
    }

    @Test
    public void getAllConsentTest() {
        RealmModel realm = realmManager.getRealm("original");
        Map<String, ClientModel> clients = realm.getClientNameMap();
        ClientModel fooClient = clients.get("foo-client");

        UserModel john = session.users().getUserByUsername("john", realm);
        UserModel mary = session.users().getUserByUsername("mary", realm);

        List<GrantedConsentModel> johnConsents = john.getGrantedConsents();
        Assert.assertEquals(2, johnConsents.size());

        List<GrantedConsentModel> maryConsents = mary.getGrantedConsents();
        Assert.assertEquals(1, maryConsents.size());
        GrantedConsentModel maryConsent = maryConsents.get(0);
        Assert.assertEquals(maryConsent.getClientId(), fooClient.getId());
        Assert.assertEquals(maryConsent.getGrantedRoles().size(), 1);
        Assert.assertEquals(maryConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertTrue(isRoleGranted(realm, "realm-role", maryConsent));
        Assert.assertTrue(isMapperGranted(fooClient, "foo", maryConsent));
    }

    @Test
    public void updateWithRoleRemovalTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientNameMap().get("foo-client");
        UserModel john = session.users().getUserByUsername("john", realm);

        GrantedConsentModel johnConsent = john.getGrantedConsentByClient(fooClient.getId());

        // Remove foo protocol mapper from johnConsent
        ProtocolMapperModel protMapperModel = fooClient.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "foo");
        johnConsent.getGrantedProtocolMappers().remove(protMapperModel.getId());

        // Remove realm-role and add new-realm-role to johnConsent
        RoleModel realmRole = realm.getRole("realm-role");
        johnConsent.getGrantedRoles().remove(realmRole.getId());

        RoleModel newRealmRole = realm.addRole("new-realm-role");
        johnConsent.addGrantedRole(newRealmRole.getId());

        john.updateGrantedConsent(johnConsent);

        commit();

        realm = realmManager.getRealm("original");
        fooClient = realm.getClientNameMap().get("foo-client");
        john = session.users().getUserByUsername("john", realm);
        johnConsent = john.getGrantedConsentByClient(fooClient.getId());

        Assert.assertEquals(johnConsent.getGrantedRoles().size(), 2);
        Assert.assertEquals(johnConsent.getGrantedProtocolMappers().size(), 0);
        Assert.assertFalse(isRoleGranted(realm, "realm-role", johnConsent));
        Assert.assertTrue(isRoleGranted(realm, "new-realm-role", johnConsent));
        Assert.assertFalse(isMapperGranted(fooClient, "foo", johnConsent));
    }

    @Test
    public void revokeTest() {
        RealmModel realm = realmManager.getRealm("original");
        ClientModel fooClient = realm.getClientNameMap().get("foo-client");
        UserModel john = session.users().getUserByUsername("john", realm);

        john.revokeGrantedConsentForClient(fooClient.getId());

        commit();

        realm = realmManager.getRealm("original");
        john = session.users().getUserByUsername("john", realm);
        Assert.assertNull(john.getGrantedConsentByClient(fooClient.getId()));
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
        ClientModel fooClient = realm.getClientNameMap().get("foo-client");
        ProtocolMapperModel fooMapper = fooClient.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, "foo");
        String fooMapperId = fooMapper.getId();
        fooClient.removeProtocolMapper(fooMapper);

        commit();

        realm = realmManager.getRealm("original");
        fooClient = realm.getClientNameMap().get("foo-client");
        UserModel john = session.users().getUserByUsername("john", realm);
        GrantedConsentModel johnConsent = john.getGrantedConsentByClient(fooClient.getId());

        Assert.assertEquals(johnConsent.getGrantedRoles().size(), 2);
        Assert.assertEquals(johnConsent.getGrantedProtocolMappers().size(), 0);
        Assert.assertFalse(johnConsent.isProtocolMapperGranted(fooMapperId));
    }

    @Test
    public void deleteRoleTest() {
        RealmModel realm = realmManager.getRealm("original");
        RoleModel realmRole = realm.getRole("realm-role");
        String realmRoleId = realmRole.getId();
        realm.removeRole(realmRole);

        commit();

        realm = realmManager.getRealm("original");
        Map<String, ClientModel> clients = realm.getClientNameMap();
        ClientModel fooClient = clients.get("foo-client");
        ClientModel barClient = clients.get("bar-client");
        UserModel john = session.users().getUserByUsername("john", realm);
        GrantedConsentModel johnConsent = john.getGrantedConsentByClient(fooClient.getId());

        Assert.assertEquals(johnConsent.getGrantedRoles().size(), 1);
        Assert.assertEquals(johnConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertFalse(johnConsent.isRoleGranted(realmRoleId));
        Assert.assertTrue(isRoleGranted(barClient, "bar-client-role", johnConsent));
    }

    @Test
    public void deleteClientTest() {
        RealmModel realm = realmManager.getRealm("original");
        Map<String, ClientModel> clients = realm.getClientNameMap();
        ClientModel barClient = clients.get("bar-client");
        realm.removeClient(barClient.getId());

        commit();

        realm = realmManager.getRealm("original");
        clients = realm.getClientNameMap();
        ClientModel fooClient = clients.get("foo-client");
        Assert.assertNull(clients.get("bar-client"));

        UserModel john = session.users().getUserByUsername("john", realm);

        GrantedConsentModel johnFooConsent = john.getGrantedConsentByClient(fooClient.getId());
        Assert.assertEquals(johnFooConsent.getGrantedRoles().size(), 1);
        Assert.assertEquals(johnFooConsent.getGrantedProtocolMappers().size(), 1);
        Assert.assertTrue(isRoleGranted(realm, "realm-role", johnFooConsent));
        Assert.assertTrue(isMapperGranted(fooClient, "foo", johnFooConsent));

        Assert.assertNull(john.getGrantedConsentByClient(barClient.getId()));
    }

    private boolean isRoleGranted(RoleContainerModel roleContainer, String roleName, GrantedConsentModel consentModel) {
        RoleModel role = roleContainer.getRole(roleName);
        return consentModel.isRoleGranted(role.getId());
    }

    private boolean isMapperGranted(ClientModel client, String protocolMapperName, GrantedConsentModel consentModel) {
        ProtocolMapperModel protocolMapper = client.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, protocolMapperName);
        return consentModel.isProtocolMapperGranted(protocolMapper.getId());
    }
}
