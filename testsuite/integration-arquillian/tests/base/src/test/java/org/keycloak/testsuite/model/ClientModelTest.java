/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
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
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class ClientModelTest extends AbstractKeycloakTest {
    private ClientModel client;
    private String roleId;
    private String realmName="original";
    private KeycloakSession currentSession;

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setEnabled(true);
        testRealms.add(realm);
    }

    public static void assertEquals(ClientModel expected, ClientModel actual) {
        assertThat(expected.getClientId(), is(actual.getClientId()));
        assertThat(expected.getName(), is(actual.getName()));
        assertThat(expected.getDescription(), is(actual.getDescription()));
        assertThat(expected.getBaseUrl(), is(actual.getBaseUrl()));
        assertThat(expected.getManagementUrl(), is(actual.getManagementUrl()));
        assertThat(expected.getDefaultRoles(), is(actual.getDefaultRoles()));

        assertThat(expected.getRedirectUris().containsAll(actual.getRedirectUris()), is(true));
        assertThat(expected.getWebOrigins().containsAll(actual.getWebOrigins()), is(true));
        assertThat(expected.getRegisteredNodes(), is(actual.getRegisteredNodes()));
    }


    private ClientModel setUpClient(RealmModel realm) {
        ClientModel client = realm.addClient("application");
        client.setName("Application");
        client.setDescription("Description");
        client.setBaseUrl("http://base");
        client.setManagementUrl("http://management");
        client.setClientId("app-name");
        client.setProtocol("openid-connect");
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
        client.addProtocolMapper(AddressMapper.createAddressMapper());
        client.updateClient();
        return client;
    }

    @Test
    @ModelTest
    public void testClientRoleRemovalAndClientScope(KeycloakSession session) {
        // Client "from" has a role.  Assign this role to a scope to client "scoped".  Delete the role and make sure
        // cache gets cleared

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionClientRoleRemove1) -> {
            currentSession = sessionClientRoleRemove1;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            assertThat("Realm Model 'original' is NULL !!", realm, notNullValue());
            ClientModel from = realm.addClient("from");

            RoleModel role = from.addRole("clientRole");
            roleId = role.getId();

            ClientModel scoped = realm.addClient("scoped");
            scoped.setFullScopeAllowed(false);
            scoped.addScopeMapping(role);

        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionClientRoleRemove2) -> {
            currentSession = sessionClientRoleRemove2;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            assertThat("Realm Model 'original' is NULL !!", realm, notNullValue());
            ClientModel from = realm.getClientByClientId("from");

            RoleModel role = currentSession.realms().getRoleById(roleId, realm);
            from.removeRole(role);
            currentSession.realms().removeClient(from.getId(), realm);

        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionClientRoleRemove3) -> {
            currentSession = sessionClientRoleRemove3;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            assertThat("Realm Model 'original' is NULL !!", realm, notNullValue());
            ClientModel scoped = realm.getClientByClientId("scoped");
            Set<RoleModel> scopeMappings = scoped.getScopeMappings();

            // used to throw an NPE
            assertThat("Scope Mappings must be 0", scopeMappings.size(), is(0));
            currentSession.realms().removeClient(scoped.getId(), realm);
        });

    }

    @Test
    @ModelTest
    public void testClientRoleRemovalAndClientScopeSameTx(KeycloakSession session) {
        // Client "from" has a role.  Assign this role to a scope to client "scoped".  Delete the role and make sure
        // cache gets cleared

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionClientRoleRemoveTx1) -> {
            currentSession = sessionClientRoleRemoveTx1;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            ClientModel from = realm.addClient("from");
            RoleModel role = from.addRole("clientRole");
            roleId = role.getId();
            ClientModel scoped = realm.addClient("scoped");

            scoped.setFullScopeAllowed(false);
            scoped.addScopeMapping(role);

        });
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionClientRoleRemoveTx2) -> {
            currentSession = sessionClientRoleRemoveTx2;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            ClientModel scoped = realm.getClientByClientId("scoped");
            ClientModel from = realm.getClientByClientId("from");
            RoleModel role = currentSession.realms().getRoleById(roleId, realm);
            from.removeRole(role);
            Set<RoleModel> scopeMappings = scoped.getScopeMappings();

            // used to throw an NPE
            assertThat("Scope Mappings is not 0", scopeMappings.size(), is(0));
            currentSession.realms().removeClient(scoped.getId(), realm);
            currentSession.realms().removeClient(from.getId(), realm);

        });
    }

    @Test
    @ModelTest
    public void testRealmRoleRemovalAndClientScope(KeycloakSession session) {
        // Client "from" has a role.  Assign this role to a scope to client "scoped".  Delete the role and make sure
        // cache gets cleared

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRealmRoleRemove1) -> {
            currentSession = sessionRealmRoleRemove1;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            RoleModel role = realm.addRole("clientRole");
            roleId = role.getId();
            ClientModel scoped = realm.addClient("scoped");
            scoped.setFullScopeAllowed(false);
            scoped.addScopeMapping(role);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRealmRoleRemove2) -> {
            currentSession = sessionRealmRoleRemove2;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            RoleModel role = currentSession.realms().getRoleById(roleId, realm);
            realm.removeRole(role);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRealmRoleRemove3) -> {
            currentSession = sessionRealmRoleRemove3;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            ClientModel scoped = realm.getClientByClientId("scoped");
            Set<RoleModel> scopeMappings = scoped.getScopeMappings();
            // used to throw an NPE
            assertThat("Scope Mappings is not 0", scopeMappings.size(), is(0));
            currentSession.realms().removeClient(scoped.getId(), realm);
        });
    }

    @Test
    @ModelTest
    public void testCircularClientScopes(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCircuilarClient1) -> {
            currentSession = sessionCircuilarClient1;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            ClientModel scoped1 = realm.addClient("scoped1");
            RoleModel role1 = scoped1.addRole("role1");
            ClientModel scoped2 = realm.addClient("scoped2");
            RoleModel role2 = scoped2.addRole("role2");
            scoped1.addScopeMapping(role2);
            scoped2.addScopeMapping(role1);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCircuilarClient2) -> {
            currentSession = sessionCircuilarClient2;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            // this hit the circular cache and failed with a stack overflow
            ClientModel scoped1 = realm.getClientByClientId("scoped1");
            currentSession.realms().removeClient(scoped1.getId(), realm);
        });
    }

    @Test
    @ModelTest
    public void persist(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionPersist) -> {
            currentSession = sessionPersist;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            client = setUpClient(realm);
            ClientModel actual = realm.getClientByClientId("app-name");

            assertEquals(client, actual);

            client.unregisterNode("node1");
            client.unregisterNode("10.20.30.40");

            currentSession.realms().removeClient(client.getId(), realm);
        });
    }

    @Test
    @ModelTest
    public void json(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionJson) -> {
            currentSession = sessionJson;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            client = setUpClient(realm);
            ClientRepresentation representation = ModelToRepresentation.toRepresentation(client, currentSession);
            representation.setId(null);
            for (ProtocolMapperRepresentation protocolMapper : representation.getProtocolMappers()) {
                protocolMapper.setId(null);
            }

            realm = currentSession.realms().createRealm("copy");
            ClientModel copyClient = RepresentationToModel.createClient(currentSession, realm, representation, true);

            assertEquals(client, copyClient);

            client.unregisterNode("node1");
            client.unregisterNode("10.20.30.40");

            currentSession.realms().removeClient(client.getId(), realm);
            currentSession.realms().removeClient(copyClient.getId(), realm);
            currentSession.realms().removeRealm(realm.getId());
        });
    }

    @Test
    @ModelTest
    public void testAddApplicationWithId(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionAppWithId1) -> {
            currentSession = sessionAppWithId1;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            client = realm.addClient("app-123", "application2");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionAppWithId2) -> {
            currentSession = sessionAppWithId2;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            client = currentSession.realms().getClientById("app-123", realm);
            assertThat("Client 'app-123' is NULL!!", client, notNullValue());

            currentSession.realms().removeClient(client.getId(), realm);
        });
    }

    @Test
    @ModelTest
    public void testClientScopesBinding(KeycloakSession session) {
        AtomicReference<ClientScopeModel> scope1Atomic = new AtomicReference<>();
        AtomicReference<ClientScopeModel> scope2Atomic = new AtomicReference<>();
        AtomicReference<ClientScopeModel> scope3Atomic = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionClientScopeBind1) -> {
            currentSession = sessionClientScopeBind1;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            client = realm.addClient("templatized");
            client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            ClientScopeModel scope1 = realm.addClientScope("scope1");
            scope1.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            scope1Atomic.set(scope1);

            ClientScopeModel scope2 = realm.addClientScope("scope2");
            scope2.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            scope2Atomic.set(scope2);

            ClientScopeModel scope3 = realm.addClientScope("scope3");
            scope3.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            scope3Atomic.set(scope3);

        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionClientScopeBind2) -> {
            currentSession = sessionClientScopeBind2;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            client = realm.getClientByClientId("templatized");

            ClientScopeModel scope1 = scope1Atomic.get();
            ClientScopeModel scope2 = scope2Atomic.get();
            ClientScopeModel scope3 = scope3Atomic.get();

            scope1 = realm.getClientScopeById(scope1.getId());
            scope2 = realm.getClientScopeById(scope2.getId());
            scope3 = realm.getClientScopeById(scope3.getId());

            client.addClientScope(scope1, true);
            client.addClientScope(scope2, false);
            client.addClientScope(scope3, false);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionClientScopeBind3) -> {
            currentSession = sessionClientScopeBind3;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            client = realm.getClientByClientId("templatized");

            ClientScopeModel scope1 = scope1Atomic.get();
            ClientScopeModel scope2 = scope2Atomic.get();

            Map<String, ClientScopeModel> clientScopes1 = client.getClientScopes(true, true);
            assertThat("Client Scope contains 'scope1':", clientScopes1.containsKey("scope1"), is(true));
            assertThat("Client Scope contains 'scope2':", clientScopes1.containsKey("scope2"), is(false));
            assertThat("Client Scope contains 'scope3':", clientScopes1.containsKey("scope3"), is(false));

            Map<String, ClientScopeModel> clientScopes2 = client.getClientScopes(false, true);
            assertThat("Client Scope contains 'scope1':", clientScopes2.containsKey("scope1"), is(false));
            assertThat("Client Scope contains 'scope2':", clientScopes2.containsKey("scope2"), is(true));
            assertThat("Client Scope contains 'scope3':", clientScopes2.containsKey("scope3"), is(true));

            // Remove some binding and check it was removed
            client.removeClientScope(scope1);
            client.removeClientScope(scope2);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionClientScopeBind3) -> {
            currentSession = sessionClientScopeBind3;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            client = realm.getClientByClientId("templatized");
            ClientScopeModel scope3 = scope3Atomic.get();

            Map<String, ClientScopeModel> clientScopes1 = client.getClientScopes(true, true);
            assertThat("Client Scope contains 'scope1':", clientScopes1.containsKey("scope1"), is(false));
            assertThat("Client Scope contains 'scope2':", clientScopes1.containsKey("scope2"), is(false));
            assertThat("Client Scope contains 'scope3':", clientScopes1.containsKey("scope3"), is(false));

            Map<String, ClientScopeModel> clientScopes2 = client.getClientScopes(false, true);
            assertThat("Client Scope contains 'scope1':", clientScopes2.containsKey("scope1"), is(false));
            assertThat("Client Scope contains 'scope2':", clientScopes2.containsKey("scope2"), is(false));
            assertThat("Client Scope contains 'scope3':", clientScopes2.containsKey("scope3"), is(true));

            currentSession.realms().removeClient(client.getId(), realm);
            client.removeClientScope(scope3);
            realm.removeClientScope(scope1Atomic.get().getId());
            realm.removeClientScope(scope2Atomic.get().getId());
            realm.removeClientScope(scope3Atomic.get().getId());
        });
    }

    @Test
    @ModelTest
    public void testCannotRemoveBoundClientTemplate(KeycloakSession session) {
        AtomicReference<ClientScopeModel> scope1Atomic = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCantRemoveBound1) -> {
            currentSession = sessionCantRemoveBound1;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            client = realm.addClient("templatized");
            ClientScopeModel scope1 = realm.addClientScope("template");
            scope1Atomic.set(scope1);
            client.addClientScope(scope1, true);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCantRemoveBound2) -> {
            currentSession = sessionCantRemoveBound2;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            ClientScopeModel scope1 = scope1Atomic.get();
            client = realm.getClientByClientId("templatized");

            assertThat("Scope name is wrong!!", scope1.getName(), is("template"));

            try {
                realm.removeClientScope(scope1.getId());
                Assert.fail();
            } catch (ModelException e) {
                // Expected
            }

            currentSession.realms().removeClient(client.getId(), realm);
            realm.removeClientScope(scope1Atomic.get().getId());

            assertThat("Error with removing Client from realm.", realm.getClientById(client.getId()), nullValue());
            assertThat("Error with removing Client Scope from realm.", realm.getClientScopeById(scope1.getId()), nullValue());
        });
    }

    @Test
    @ModelTest
    public void testDefaultDefaultClientScopes(KeycloakSession session) {
        AtomicReference<ClientScopeModel> scope1Atomic = new AtomicReference<>();
        AtomicReference<ClientScopeModel> scope2Atomic = new AtomicReference<>();
        AtomicReference<ClientScopeModel> scope3Atomic = new AtomicReference<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionDefaultClientScope1) -> {
            currentSession = sessionDefaultClientScope1;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            ClientScopeModel scope1 = realm.addClientScope("scope1");
            scope1.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            scope1Atomic.set(scope1);

            ClientScopeModel scope2 = realm.addClientScope("scope2");
            scope2.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            scope2Atomic.set(scope2);

            ClientScopeModel scope3 = realm.addClientScope("scope3");
            scope3.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            scope3Atomic.set(scope3);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionDefaultClientScope2) -> {
            currentSession = sessionDefaultClientScope2;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);

            ClientScopeModel scope1 = scope1Atomic.get();
            ClientScopeModel scope2 = scope2Atomic.get();
            ClientScopeModel scope3 = scope3Atomic.get();

            scope1 = realm.getClientScopeById(scope1.getId());
            scope2 = realm.getClientScopeById(scope2.getId());
            scope3 = realm.getClientScopeById(scope3.getId());

            realm.addDefaultClientScope(scope1, true);
            realm.addDefaultClientScope(scope2, false);
            realm.addDefaultClientScope(scope3, false);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionDefaultClientScope3) -> {
            currentSession = sessionDefaultClientScope3;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            client = realm.addClient("foo");
            client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionDefaultClientScope4) -> {
            currentSession = sessionDefaultClientScope4;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            client = realm.getClientByClientId("foo");

            ClientScopeModel scope1 = scope1Atomic.get();
            ClientScopeModel scope2 = scope2Atomic.get();


            Map<String, ClientScopeModel> clientScopes1 = client.getClientScopes(true, true);
            assertThat("Client Scope contains 'scope1':", clientScopes1.containsKey("scope1"), is(true));
            assertThat("Client Scope contains 'scope2':", clientScopes1.containsKey("scope2"), is(false));
            assertThat("Client Scope contains 'scope3':", clientScopes1.containsKey("scope3"), is(false));


            Map<String, ClientScopeModel> clientScopes2 = client.getClientScopes(false, true);
            assertThat("Client Scope contains 'scope1':", clientScopes2.containsKey("scope1"), is(false));
            assertThat("Client Scope contains 'scope2':", clientScopes2.containsKey("scope2"), is(true));
            assertThat("Client Scope contains 'scope3':", clientScopes2.containsKey("scope3"), is(true));

            currentSession.realms().removeClient(client.getId(), realm);
            // Remove some realm default client scopes
            realm.removeDefaultClientScope(scope1);
            realm.removeDefaultClientScope(scope2);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionDefaultClientScope5) -> {
            currentSession = sessionDefaultClientScope5;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            client = realm.addClient("foo2");
            client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionDefaultClientScope5) -> {
            currentSession = sessionDefaultClientScope5;
            RealmModel realm = currentSession.realms().getRealmByName(realmName);
            client = realm.getClientByClientId("foo2");

            Map<String, ClientScopeModel> clientScopes1 = client.getClientScopes(true, true);
            assertThat("Client Scope contains 'scope1':", clientScopes1.containsKey("scope1"), is(false));
            assertThat("Client Scope contains 'scope2':", clientScopes1.containsKey("scope2"), is(false));
            assertThat("Client Scope contains 'scope3':", clientScopes1.containsKey("scope3"), is(false));

            Map<String, ClientScopeModel> clientScopes2 = client.getClientScopes(false, true);
            assertThat("Client Scope contains 'scope1':", clientScopes2.containsKey("scope1"), is(false));
            assertThat("Client Scope contains 'scope2':", clientScopes2.containsKey("scope2"), is(false));
            assertThat("Client Scope contains 'scope3':", clientScopes2.containsKey("scope3"), is(true));

            currentSession.realms().removeClient(client.getId(), realm);
            realm.removeClientScope(scope1Atomic.get().getId());
            realm.removeClientScope(scope2Atomic.get().getId());

            realm.removeDefaultClientScope(scope3Atomic.get());
            realm.removeClientScope(scope3Atomic.get().getId());
        });
    }
}
