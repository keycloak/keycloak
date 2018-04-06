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
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientModelTest extends AbstractModelTest {
    private ClientModel client;
    private RealmModel realm;

    @Before
    @Override
    public void before() throws Exception {
        super.before();

        realm = realmManager.createRealm("original");
        client = realm.addClient("application");
        client.setName("Application");
        client.setDescription("Description");
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
    public void testClientRoleRemovalAndClientScope() throws Exception {
        // Client "from" has a role.  Assign this role to a scope to client "scoped".  Delete the role and make sure
        // cache gets cleared
        ClientModel from = realm.addClient("from");
        RoleModel role = from.addRole("clientRole");
        String roleId = role.getId();
        ClientModel scoped = realm.addClient("scoped");
        String idOfClient = scoped.getId();
        scoped.setFullScopeAllowed(false);
        scoped.addScopeMapping(role);
        commit();
        realm = session.realms().getRealmByName("original");
        scoped = realm.getClientByClientId("scoped");
        from = realm.getClientByClientId("from");
        role = session.realms().getRoleById(roleId, realm);
        from.removeRole(role);
        commit();
        realm = session.realms().getRealmByName("original");
        scoped = realm.getClientByClientId("scoped");
        Set<RoleModel> scopeMappings = scoped.getScopeMappings();
        Assert.assertEquals(0, scopeMappings.size());  // used to throw an NPE

    }

    @Test
    public void testClientRoleRemovalAndClientScopeSameTx() throws Exception {
        // Client "from" has a role.  Assign this role to a scope to client "scoped".  Delete the role and make sure
        // cache gets cleared
        ClientModel from = realm.addClient("from");
        RoleModel role = from.addRole("clientRole");
        String roleId = role.getId();
        ClientModel scoped = realm.addClient("scoped");
        String idOfClient = scoped.getId();
        scoped.setFullScopeAllowed(false);
        scoped.addScopeMapping(role);
        commit();
        realm = session.realms().getRealmByName("original");
        scoped = realm.getClientByClientId("scoped");
        from = realm.getClientByClientId("from");
        role = session.realms().getRoleById(roleId, realm);
        from.removeRole(role);
        Set<RoleModel> scopeMappings = scoped.getScopeMappings();
        Assert.assertEquals(0, scopeMappings.size());  // used to throw an NPE

    }

    @Test
    public void testRealmRoleRemovalAndClientScope() throws Exception {
        // Client "from" has a role.  Assign this role to a scope to client "scoped".  Delete the role and make sure
        // cache gets cleared
        RoleModel role = realm.addRole("clientRole");
        String roleId = role.getId();
        ClientModel scoped = realm.addClient("scoped");
        String idOfClient = scoped.getId();
        scoped.setFullScopeAllowed(false);
        scoped.addScopeMapping(role);
        commit();
        realm = session.realms().getRealmByName("original");
        scoped = realm.getClientByClientId("scoped");
        role = session.realms().getRoleById(roleId, realm);
        realm.removeRole(role);
        commit();
        realm = session.realms().getRealmByName("original");
        scoped = realm.getClientByClientId("scoped");
        Set<RoleModel> scopeMappings = scoped.getScopeMappings();
        Assert.assertEquals(0, scopeMappings.size());  // used to throw an NPE

    }

    @Test
    public void testCircularClientScopes() throws Exception {
        ClientModel scoped1 = realm.addClient("scoped");
        RoleModel role1 = scoped1.addRole("role1");
        ClientModel scoped2 = realm.addClient("scoped2");
        RoleModel role2 = scoped2.addRole("role2");
        scoped1.addScopeMapping(role2);
        scoped2.addScopeMapping(role1);
        commit();
        realm = session.realms().getRealmByName("original");

        // this hit the circular cache and failed with a stack overflow
        scoped1 = realm.getClientByClientId("scoped");


    }




    @Test
    public void persist() {
        RealmModel persisted = realmManager.getRealm(realm.getId());

        ClientModel actual = persisted.getClientByClientId("app-name");
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

    @Test
    public void testCannotRemoveBoundClientTemplate() {
        ClientModel client = realm.addClient("templatized");
        ClientTemplateModel template = realm.addClientTemplate("template");
        client.setClientTemplate(template);
        commit();
        realm = realmManager.getRealmByName("original");
        try {
            realm.removeClientTemplate(template.getId());
            Assert.fail();
        } catch (ModelException e) {

        }
        realm.removeClient(client.getId());
        realm.removeClientTemplate(template.getId());
        commit();
    }


    public static void assertEquals(ClientModel expected, ClientModel actual) {
        Assert.assertEquals(expected.getClientId(), actual.getClientId());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getDescription(), actual.getDescription());
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

