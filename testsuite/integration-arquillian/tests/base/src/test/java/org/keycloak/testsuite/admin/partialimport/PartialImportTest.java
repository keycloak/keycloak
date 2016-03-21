/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.testsuite.admin.partialimport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.partialimport.PartialImportResult;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.keycloak.representations.idm.PartialImportRepresentation.Policy;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

/**
 * Tests for the partial import endpoint in admin client.  Also tests the
 * server side functionality of each resource along with "fail, skip, overwrite"
 * functions.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class PartialImportTest extends AbstractAuthTest {

    private static final int NUM_RESOURCE_TYPES = 5;
    private static final String CLIENT_ROLES_CLIENT = "clientRolesClient";
    private static final String USER_PREFIX = "user";
    private static final String CLIENT_PREFIX = "client";
    private static final String REALM_ROLE_PREFIX = "realmRole";
    private static final String CLIENT_ROLE_PREFIX = "clientRole";
    private static final String[] IDP_ALIASES = {"twitter", "github", "facebook", "google", "linkedin", "microsoft", "stackoverflow"};
    private static final int NUM_ENTITIES = IDP_ALIASES.length;

    private PartialImportRepresentation piRep;

    @Before
    public void init() {
        piRep = new PartialImportRepresentation();
    }

    @Before
    public void createClientForClientRoles() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ROLES_CLIENT);
        client.setName(CLIENT_ROLES_CLIENT);
        client.setRootUrl("foo");
        client.setProtocol("openid-connect");
        Response resp = testRealmResource().clients().create(client);

        // for some reason, findAll() will later fail unless readEntity is called here
        resp.readEntity(String.class);
        //testRealmResource().clients().findAll();
    }

    @Before
    public void removeUsers() {
        List<UserRepresentation> toRemove = testRealmResource().users().search(USER_PREFIX, 0, NUM_ENTITIES);
        for (UserRepresentation user : toRemove) {
            testRealmResource().users().get(user.getId()).remove();
        }
    }

    @Before
    public void removeClients() {
        List<ClientRepresentation> toRemove = testRealmResource().clients().findAll();
        for (ClientRepresentation client : toRemove) {
            if (client.getName().startsWith(CLIENT_PREFIX)) {
                testRealmResource().clients().get(client.getId()).remove();
            }
        }
    }

    @Before
    public void removeProviders() {
        List<IdentityProviderRepresentation> toRemove = testRealmResource().identityProviders().findAll();
        for (IdentityProviderRepresentation idp : toRemove) {
            testRealmResource().identityProviders().get(idp.getInternalId()).remove();
        }
    }

    @Before
    public void removeRealmRoles() {
        List<RoleRepresentation> toRemove = testRealmResource().roles().list();
        for (RoleRepresentation role : toRemove) {
            if (role.getName().startsWith(REALM_ROLE_PREFIX)) {
                testRealmResource().roles().get(role.getName()).remove();
            }
        }
    }

    @Before
    public void removeClientRoles() {
        List<RoleRepresentation> toRemove = clientRolesClient().roles().list();
        for (RoleRepresentation role : toRemove) {
            if (role.getName().startsWith(CLIENT_ROLE_PREFIX)) {
                testRealmResource().clients().get(CLIENT_ROLES_CLIENT).roles().get(role.getName()).remove();
            }
        }
    }

    private ClientResource clientRolesClient() {
        return ApiUtil.findClientResourceByName(testRealmResource(), CLIENT_ROLES_CLIENT);
    }

    private void setFail() {
        piRep.setIfResourceExists(Policy.FAIL.toString());
    }

    private void setSkip() {
        piRep.setIfResourceExists(Policy.SKIP.toString());
    }

    private void setOverwrite() {
        piRep.setIfResourceExists(Policy.OVERWRITE.toString());
    }

    private PartialImportResults doImport() {
        Response response = testRealmResource().partialImport(piRep);
        return response.readEntity(PartialImportResults.class);
    }

    private void addUsers() {
        List<UserRepresentation> users = new ArrayList<>();

        for (int i = 0; i < NUM_ENTITIES; i++) {
            UserRepresentation user = createUserRepresentation(USER_PREFIX + i, USER_PREFIX + i + "@foo.com", "foo", "bar", true);
            users.add(user);
        }

        piRep.setUsers(users);
    }

    private void addClients() {
        List<ClientRepresentation> clients = new ArrayList<>();

        for (int i = 0; i < NUM_ENTITIES; i++) {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(CLIENT_PREFIX + i);
            client.setName(CLIENT_PREFIX + i);
            client.setRootUrl("foo");
            clients.add(client);
        }

        piRep.setClients(clients);
    }

    private void addProviders() {
        List<IdentityProviderRepresentation> providers = new ArrayList<>();

        for (String alias : IDP_ALIASES) {
            IdentityProviderRepresentation idpRep = new IdentityProviderRepresentation();
            idpRep.setAlias(alias);
            idpRep.setProviderId(alias);
            idpRep.setEnabled(true);
            idpRep.setAuthenticateByDefault(false);
            idpRep.setFirstBrokerLoginFlowAlias("first broker login");

            Map<String, String> config = new HashMap<>();
            config.put("clientSecret", "secret");
            config.put("clientId", alias);
            idpRep.setConfig(config);
            providers.add(idpRep);
        }

        piRep.setIdentityProviders(providers);
    }

    private List<RoleRepresentation> makeRoles(String prefix) {
        List<RoleRepresentation> roles = new ArrayList<>();

        for (int i = 0; i < NUM_ENTITIES; i++) {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(prefix + i);
            roles.add(role);
        }

        return roles;
    }

    private void addRealmRoles() {
        RolesRepresentation roles = piRep.getRoles();
        if (roles == null) roles = new RolesRepresentation();
        roles.setRealm(makeRoles(REALM_ROLE_PREFIX));
        piRep.setRoles(roles);
    }

    private void addClientRoles() {
        RolesRepresentation roles = piRep.getRoles();
        if (roles == null) roles = new RolesRepresentation();
        Map<String, List<RoleRepresentation>> clientRolesMap = new HashMap<>();
        clientRolesMap.put(CLIENT_ROLES_CLIENT, makeRoles(CLIENT_ROLE_PREFIX));
        roles.setClient(clientRolesMap);
        piRep.setRoles(roles);
    }

    @Test
    public void testAddUsers() {
        setFail();
        addUsers();

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        for (PartialImportResult result : results.getResults()) {
            String id = result.getId();
            UserResource userRsc = testRealmResource().users().get(id);
            UserRepresentation user = userRsc.toRepresentation();
            assertTrue(user.getUsername().startsWith(USER_PREFIX));
        }
    }

    @Test
    public void testAddClients() {
        setFail();
        addClients();

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        for (PartialImportResult result : results.getResults()) {
            String id = result.getId();
            ClientResource clientRsc = testRealmResource().clients().get(id);
            ClientRepresentation client = clientRsc.toRepresentation();
            assertTrue(client.getName().startsWith(CLIENT_PREFIX));
        }
    }

    @Test
    public void testAddProviders() {
        setFail();
        addProviders();

        PartialImportResults results = doImport();
        assertEquals(IDP_ALIASES.length, results.getAdded());

        for (PartialImportResult result : results.getResults()) {
            String id = result.getId();
            IdentityProviderResource idpRsc = testRealmResource().identityProviders().get(id);
            IdentityProviderRepresentation idp = idpRsc.toRepresentation();
            Map<String, String> config = idp.getConfig();
            assertTrue(Arrays.asList(IDP_ALIASES).contains(config.get("clientId")));
        }
    }

    @Test
    public void testAddRealmRoles() {
        setFail();
        addRealmRoles();

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        for (PartialImportResult result : results.getResults()) {
            String name = result.getResourceName();
            RoleResource roleRsc = testRealmResource().roles().get(name);
            RoleRepresentation role = roleRsc.toRepresentation();
            assertTrue(role.getName().startsWith(REALM_ROLE_PREFIX));
        }
    }

    @Test
    public void testAddClientRoles() {
        setFail();
        addClientRoles();

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        List<RoleRepresentation> clientRoles = clientRolesClient().roles().list();
        assertEquals(NUM_ENTITIES, clientRoles.size());

        for (RoleRepresentation roleRep : clientRoles) {
            assertTrue(roleRep.getName().startsWith(CLIENT_ROLE_PREFIX));
        }
    }

    private void testFail() {
        setFail();
        PartialImportResults results = doImport();
        assertNull(results.getErrorMessage());
        results = doImport(); // second time should fail
        assertNotNull(results.getErrorMessage());
    }

    @Test
    public void testAddUsersFail() {
        addUsers();
        testFail();
    }

    @Test
    public void testAddClientsFail() {
        addClients();
        testFail();
    }

    @Test
    public void testAddProvidersFail() {
        addProviders();
        testFail();
    }

    @Test
    public void testAddRealmRolesFail() {
        addRealmRoles();
        testFail();
    }

    @Test
    public void testAddClientRolesFail() {
        addClientRoles();
        testFail();
    }

    private void testSkip() {
        setSkip();
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        results = doImport();
        assertEquals(NUM_ENTITIES, results.getSkipped());
    }

    @Test
    public void testAddUsersSkip() {
        addUsers();
        testSkip();
    }

    @Test
    public void testAddClientsSkip() {
        addClients();
        testSkip();
    }

    @Test
    public void testAddProvidersSkip() {
        addProviders();
        testSkip();
    }

    @Test
    public void testAddRealmRolesSkip() {
        addRealmRoles();
        testSkip();
    }

    @Test
    public void testAddClientRolesSkip() {
        addClientRoles();
        testSkip();
    }

    private void testOverwrite() {
        setOverwrite();
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        results = doImport();
        assertEquals(NUM_ENTITIES, results.getOverwritten());
    }

    @Test
    public void testAddUsersOverwrite() {
        addUsers();
        testOverwrite();
    }

    @Test
    public void testAddClientsOverwrite() {
        addClients();
        testOverwrite();
    }

    @Test
    public void testAddProvidersOverwrite() {
        addProviders();
        testOverwrite();
    }

    @Test
    public void testAddRealmRolesOverwrite() {
        addRealmRoles();
        testOverwrite();
    }

    @Test
    public void testAddClientRolesOverwrite() {
        addClientRoles();
        testOverwrite();
    }


    private void importEverything() {
        addUsers();
        addClients();
        addProviders();
        addRealmRoles();
        addClientRoles();

        PartialImportResults results = doImport();
        assertNull(results.getErrorMessage());
        assertEquals(NUM_ENTITIES * NUM_RESOURCE_TYPES, results.getAdded());
    }

    @Test
    public void testEverythingFail() {
        setFail();
        importEverything();
        PartialImportResults results = doImport(); // second import will fail because not allowed to skip or overwrite
        assertNotNull(results.getErrorMessage());
    }

    @Test
    public void testEverythingSkip() {
        setSkip();
        importEverything();
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES * NUM_RESOURCE_TYPES, results.getSkipped());
    }

    @Test
    public void testEverythingOverwrite() {
        setOverwrite();
        importEverything();
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES * NUM_RESOURCE_TYPES, results.getOverwritten());
    }

}
