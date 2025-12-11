/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.authz.fgap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class ClientResourceTypeFilteringTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @BeforeEach
    public void onBeforeEach() {
        for (int i = 0; i < 50; i++) {
            ClientRepresentation client = new ClientRepresentation();

            client.setClientId("client-" + i);
            client.setName(client.getClientId());
            client.setEnabled(true);
            client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            client.setPublicClient(false);
            client.setSecret("secret");
            client.setAttributes(Map.of("saml.artifact.binding.identifier", "value"));

            realm.admin().clients().create(client).close();
        }
    }

    @Test
    public void testViewAllClientsUsingUserPolicy() {
        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertTrue(search.isEmpty());

        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, CLIENTS_RESOURCE_TYPE, policy, Set.of(VIEW));

        search = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertFalse(search.isEmpty());
        assertEquals(59, search.size());
    }

    @Test
    public void testViewSpecificClientsUsingUserPolicy() {
        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertTrue(search.isEmpty());

        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, CLIENTS_RESOURCE_TYPE, policy, Set.of(VIEW));

        search = realmAdminClient.realm(realm.getName()).clients().findAll("client-", true, true, null, null);
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());
    }

    @Test
    public void testViewClientsByAttributeUsingUserPolicy() {
        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findAll();
        assertTrue(search.isEmpty());

        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, CLIENTS_RESOURCE_TYPE, policy, Set.of(VIEW));

        search = realmAdminClient.realm(realm.getName()).clients().query("saml.artifact.binding.identifier:\"value\"");
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());
    }

    @Test
    public void testDeniedResourcesPrecedenceOverGrantedResources() {
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, CLIENTS_RESOURCE_TYPE, policy, Set.of(VIEW));

        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findAll("client-", true, true, null, null);
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());

        UserPolicyRepresentation notMyAdminPolicy = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        Set<String> notAllowedClients = search.stream()
                .filter((g) -> Set.of("client-0", "client-15", "client-30", "client-45").contains(g.getClientId()))
                .map(ClientRepresentation::getId)
                .collect(Collectors.toSet());
        assertFalse(notAllowedClients.isEmpty());
        createPermission(client, notAllowedClients, CLIENTS_RESOURCE_TYPE, Set.of(VIEW), notMyAdminPolicy);
        search = realmAdminClient.realm(realm.getName()).clients().findAll("client-", true, true, null, null);
        assertFalse(search.isEmpty());
        assertTrue(search.stream().map(ClientRepresentation::getId).noneMatch(notAllowedClients::contains));
    }

    @Test
    public void testSearchByClientId() {
        String expectedClientId = "client-0";
        List<ClientRepresentation> search = realmAdminClient.realm(realm.getName()).clients().findByClientId(expectedClientId);
        assertTrue(search.isEmpty());

        UserPolicyRepresentation allowPolicy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(client, expectedClientId, CLIENTS_RESOURCE_TYPE, Set.of(VIEW), allowPolicy);
        search = realmAdminClient.realm(realm.getName()).clients().findByClientId(expectedClientId);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());
        assertEquals(search.get(0).getClientId(), expectedClientId);
    }
}
