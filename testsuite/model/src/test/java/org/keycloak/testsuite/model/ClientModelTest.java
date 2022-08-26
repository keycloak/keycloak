/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author rmartinc
 */
@RequireProvider(RealmProvider.class)
@RequireProvider(ClientProvider.class)
@RequireProvider(RoleProvider.class)
public class ClientModelTest extends KeycloakModelTest {

    private String realmId;

    private static final String searchClientId = "My ClIeNt WITH sP%Ces and sp*ci_l Ch***cters \" ?!";

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("realm");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testClientsBasics() {
        // Create client
        ClientModel originalModel = withRealm(realmId, (session, realm) -> session.clients().addClient(realm, "myClientId"));
        ClientModel searchClient = withRealm(realmId, (session, realm) -> {
            ClientModel client = session.clients().addClient(realm, searchClientId);
            client.setAlwaysDisplayInConsole(true);
            client.addRedirectUri("http://www.redirecturi.com");
            return client;
        });
        assertThat(originalModel.getId(), notNullValue());

        // Find by id
        {
            ClientModel model = withRealm(realmId, (session, realm) -> session.clients().getClientById(realm, originalModel.getId()));
            assertThat(model, notNullValue());
            assertThat(model.getId(), is(equalTo(model.getId())));
            assertThat(model.getClientId(), is(equalTo("myClientId")));
        }

        // Find by clientId
        {
            ClientModel model = withRealm(realmId, (session, realm) -> session.clients().getClientByClientId(realm, "myClientId"));
            assertThat(model, notNullValue());
            assertThat(model.getId(), is(equalTo(originalModel.getId())));
            assertThat(model.getClientId(), is(equalTo("myClientId")));
        }

        // Search by clientId
        {
            withRealm(realmId, (session, realm) -> {
                ClientModel client = session.clients().searchClientsByClientIdStream(realm, "client with", 0, 10).findFirst().orElse(null);
                assertThat(client, notNullValue());
                assertThat(client.getId(), is(equalTo(searchClient.getId())));
                assertThat(client.getClientId(), is(equalTo(searchClientId)));
                return null;
            });


            withRealm(realmId, (session, realm) -> {
                ClientModel client = session.clients().searchClientsByClientIdStream(realm, "sp*ci_l Ch***cters", 0, 10).findFirst().orElse(null);
                assertThat(client, notNullValue());
                assertThat(client.getId(), is(equalTo(searchClient.getId())));
                assertThat(client.getClientId(), is(equalTo(searchClientId)));
                return null;
            });

            withRealm(realmId, (session, realm) -> {
                ClientModel client = session.clients().searchClientsByClientIdStream(realm, " AND ", 0, 10).findFirst().orElse(null);
                assertThat(client, notNullValue());
                assertThat(client.getId(), is(equalTo(searchClient.getId())));
                assertThat(client.getClientId(), is(equalTo(searchClientId)));
                return null;
            });

            withRealm(realmId, (session, realm) -> {
                // when searching by "%" all entries are expected
                assertThat(session.clients().searchClientsByClientIdStream(realm, "%", 0, 10).count(), is(equalTo(2L)));
                return null;
            });
        }

        // using Boolean operand
        {
            Map<ClientModel, Set<String>> allRedirectUrisOfEnabledClients = withRealm(realmId, (session, realm) -> session.clients().getAllRedirectUrisOfEnabledClients(realm));
            assertThat(allRedirectUrisOfEnabledClients.values(), hasSize(1));
            assertThat(allRedirectUrisOfEnabledClients.keySet().iterator().next().getId(), is(equalTo(searchClient.getId())));
        }

        // Test storing flow binding override
        {
            // Add some override
            withRealm(realmId, (session, realm) -> {
                ClientModel clientById = session.clients().getClientById(realm, originalModel.getId());
                clientById.setAuthenticationFlowBindingOverride("browser", "customFlowId");
                return clientById;
            });

            String browser = withRealm(realmId, (session, realm) -> session.clients().getClientById(realm, originalModel.getId()).getAuthenticationFlowBindingOverride("browser"));
            assertThat(browser, is(equalTo("customFlowId")));
        }
    }

    @Test
    public void testScopeMappingRoleRemoval() {
        // create two clients, one realm role and one client role and assign both to one of the clients
        inComittedTransaction(1, (session , i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            ClientModel client1 = session.clients().addClient(realm, "client1");
            ClientModel client2 = session.clients().addClient(realm, "client2");
            RoleModel realmRole = session.roles().addRealmRole(realm, "realm-role");
            RoleModel client2Role = session.roles().addClientRole(client2, "client2-role");
            client1.addScopeMapping(realmRole);
            client1.addScopeMapping(client2Role);
            return null;
        });

        // check everything is OK
        inComittedTransaction(1, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            final ClientModel client1 = session.clients().getClientByClientId(realm, "client1");
            assertThat(client1.getScopeMappingsStream().count(), is(2L));
            assertThat(client1.getScopeMappingsStream().filter(r -> r.getName().equals("realm-role")).count(), is(1L));
            assertThat(client1.getScopeMappingsStream().filter(r -> r.getName().equals("client2-role")).count(), is(1L));
            return null;
        });

        // remove the realm role
        inComittedTransaction(1, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            final RoleModel role = session.roles().getRealmRole(realm, "realm-role");
            session.roles().removeRole(role);
            return null;
        });

        // check it is removed
        inComittedTransaction(1, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            final ClientModel client1 = session.clients().getClientByClientId(realm, "client1");
            assertThat(client1.getScopeMappingsStream().count(), is(1L));
            assertThat(client1.getScopeMappingsStream().filter(r -> r.getName().equals("client2-role")).count(), is(1L));
            return null;
        });

        // remove client role
        inComittedTransaction(1, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            final ClientModel client2 = session.clients().getClientByClientId(realm, "client2");
            final RoleModel role = session.roles().getClientRole(client2, "client2-role");
            session.roles().removeRole(role);
            return null;
        });

        // check both clients are removed
        inComittedTransaction(1, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            final ClientModel client1 = session.clients().getClientByClientId(realm, "client1");
            assertThat(client1.getScopeMappingsStream().count(), is(0L));
            return null;
        });

        // remove clients
        inComittedTransaction(1, (session , i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            final ClientModel client1 = session.clients().getClientByClientId(realm, "client1");
            final ClientModel client2 = session.clients().getClientByClientId(realm, "client2");
            session.clients().removeClient(realm, client1.getId());
            session.clients().removeClient(realm, client2.getId());
            return null;
        });
    }

    @Test
    public void testClientScopes() {
        List<String> clientScopes = new LinkedList<>();
        withRealm(realmId, (session, realm) -> {
            ClientModel client = session.clients().addClient(realm, "myClientId");

            ClientScopeModel clientScope1 = session.clientScopes().addClientScope(realm, "myClientScope1");
            clientScopes.add(clientScope1.getId());
            ClientScopeModel clientScope2 = session.clientScopes().addClientScope(realm, "myClientScope2");
            clientScopes.add(clientScope2.getId());


            client.addClientScope(clientScope1, true);
            client.addClientScope(clientScope2, false);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            List<String> actualClientScopes = session.clientScopes().getClientScopesStream(realm).map(ClientScopeModel::getId).collect(Collectors.toList());
            assertThat(actualClientScopes, containsInAnyOrder(clientScopes.toArray()));

            ClientScopeModel clientScopeById = session.clientScopes().getClientScopeById(realm, clientScopes.get(0));
            assertThat(clientScopeById.getId(), is(clientScopes.get(0)));

            session.clientScopes().removeClientScopes(realm);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            List<ClientScopeModel> actualClientScopes = session.clientScopes().getClientScopesStream(realm).collect(Collectors.toList());
            assertThat(actualClientScopes, empty());

            return null;
        });
    }
}
