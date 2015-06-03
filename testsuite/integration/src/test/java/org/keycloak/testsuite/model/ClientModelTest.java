package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.managers.ClientManager;

import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientModelTest extends AbstractModelTest {
    private ClientModel client;
    private RealmModel realm;
    private ClientManager appManager;

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        appManager = new ClientManager(realmManager);

        realm = realmManager.createRealm("original");
        client = realm.addClient("application");
        client.setName("Application");
        client.setBaseUrl("http://base");
        client.setManagementUrl("http://management");
        client.setClientId("app-name");
        client.addRole("role-1");
        client.addRole("role-2");
        client.addRole("role-3");
        client.addDefaultRole("role-1");
        client.addDefaultRole("role-2");

        client.addRedirectUri("redirect-1");
        client.addRedirectUri("redirect-2");

        client.addWebOrigin("origin-1");
        client.addWebOrigin("origin-2");

        client.registerNode("node1", 10);
        client.registerNode("10.20.30.40", 50);

        client.updateClient();
    }

    @Test
    public void persist() {
        RealmModel persisted = realmManager.getRealm(realm.getId());

        ClientModel actual = persisted.getClientNameMap().get("app-name");
        assertEquals(client, actual);
    }

    @Test
    public void json() {
        ClientRepresentation representation = ModelToRepresentation.toRepresentation(client);
        representation.setId(null);
        for (ProtocolMapperRepresentation protocolMapper : representation.getProtocolMappers()) {
            protocolMapper.setId(null);
        }

        RealmModel realm = realmManager.createRealm("copy");
        ClientModel copy = RepresentationToModel.createClient(session, realm, representation, true);

        assertEquals(client, copy);
    }

    @Test
    public void testAddApplicationWithId() {
        client = realm.addClient("app-123", "application2");
        commit();
        client = realmManager.getRealm(realm.getId()).getClientById("app-123");
        Assert.assertNotNull(client);
    }


    public static void assertEquals(ClientModel expected, ClientModel actual) {
        Assert.assertEquals(expected.getClientId(), actual.getClientId());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getBaseUrl(), actual.getBaseUrl());
        Assert.assertEquals(expected.getManagementUrl(), actual.getManagementUrl());
        Assert.assertEquals(expected.getDefaultRoles(), actual.getDefaultRoles());

        Assert.assertTrue(expected.getRedirectUris().containsAll(actual.getRedirectUris()));
        Assert.assertTrue(expected.getWebOrigins().containsAll(actual.getWebOrigins()));
        Assert.assertTrue(expected.getRegisteredNodes().equals(actual.getRegisteredNodes()));
    }

    public static void assertEquals(List<RoleModel> expected, List<RoleModel> actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Iterator<RoleModel> exp = expected.iterator();
        Iterator<RoleModel> act = actual.iterator();
        while (exp.hasNext()) {
            Assert.assertEquals(exp.next().getName(), act.next().getName());
        }
    }

}

