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

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.partialimport.PartialImportResult;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation.Policy;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.RealmBuilder;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.partialimport.ResourceType;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.common.Profile.Feature.UPLOAD_SCRIPTS;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import org.keycloak.util.JsonSerialization;

/**
 * Tests for the partial import endpoint in admin client.  Also tests the
 * server side functionality of each resource along with "fail, skip, overwrite"
 * functions.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class PartialImportTest extends AbstractAuthTest {

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    private static final int NUM_RESOURCE_TYPES = 6;
    private static final String CLIENT_ROLES_CLIENT = "clientRolesClient";
    private static final String CLIENT_SERVICE_ACCOUNT = "clientServiceAccount";
    private static final String USER_PREFIX = "user";
    private static final String GROUP_PREFIX = "group";
    private static final String CLIENT_PREFIX = "client";
    private static final String REALM_ROLE_PREFIX = "realmRole";
    private static final String CLIENT_ROLE_PREFIX = "clientRole";
    private static final String[] IDP_ALIASES = {"twitter", "github", "facebook", "google", "linkedin", "microsoft", "stackoverflow"};
    private static final int NUM_ENTITIES = IDP_ALIASES.length;
    private static final ResourceServerRepresentation resourceServerSampleSettings;

    private PartialImportRepresentation piRep;
    private String realmId;

    static {
        try {
            resourceServerSampleSettings = JsonSerialization.readValue(
                PartialImportTest.class.getResourceAsStream("/import/sample-authz-partial-import.json"),
                ResourceServerRepresentation.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load sample resource server configuration", e);
        }
    }

    @Before
    public void initAdminEvents() {
        RealmRepresentation realmRep = RealmBuilder.edit(testRealmResource().toRepresentation()).testEventListener().build();
        realmId = realmRep.getId();
        realmRep.setDuplicateEmailsAllowed(false);
        adminClient.realm(realmRep.getRealm()).update(realmRep);

        piRep = new PartialImportRepresentation();
    }

    @After
    public void tearDownAdminEvents() {
        RealmRepresentation realmRep = RealmBuilder.edit(testRealmResource().toRepresentation()).removeTestEventListener().build();
        adminClient.realm(realmRep.getRealm()).update(realmRep);
    }

    @Before
    public void createClientForClientRoles() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ROLES_CLIENT);
        client.setName(CLIENT_ROLES_CLIENT);
        client.setProtocol("openid-connect");
        try (Response resp = testRealmResource().clients().create(client)) {

            // for some reason, findAll() will later fail unless readEntity is called here
            resp.readEntity(String.class);
            //testRealmResource().clients().findAll();
        }
    }

    @Before
    public void createClientWithServiceAccount() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_SERVICE_ACCOUNT);
        client.setName(CLIENT_SERVICE_ACCOUNT);
        client.setRootUrl("http://localhost/foo");
        client.setProtocol("openid-connect");
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setServiceAccountsEnabled(true);
        try (Response resp = testRealmResource().clients().create(client)) {
            String id = ApiUtil.getCreatedId(resp);
            UserRepresentation serviceAccountUser = testRealmResource().clients().get(id).getServiceAccountUser();
            assertNotNull(serviceAccountUser);
        }
    }

    @Before
    public void removeUsers() {
        List<UserRepresentation> toRemove = testRealmResource().users().search(USER_PREFIX, 0, NUM_ENTITIES);
        for (UserRepresentation user : toRemove) {
            testRealmResource().users().get(user.getId()).remove();
        }
    }

    @Before
    public void removeGroups() {
        List<GroupRepresentation> toRemove = testRealmResource().groups().groups();
        for (GroupRepresentation group: toRemove) {
            testRealmResource().groups().group(group.getId()).remove();
        }
    }

    @Before
    public void removeClients() {
        List<ClientRepresentation> toRemove = testRealmResource().clients().findAll();
        for (ClientRepresentation client : toRemove) {
            if (client.getName() != null && client.getName().startsWith(CLIENT_PREFIX)) {
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
        try (Response response = testRealmResource().partialImport(piRep)) {
            return response.readEntity(PartialImportResults.class);
        }
    }

    private void addUsers() {
        List<UserRepresentation> users = new ArrayList<>();

        for (int i = 0; i < NUM_ENTITIES; i++) {
            UserRepresentation user = createUserRepresentation(USER_PREFIX + i, USER_PREFIX + i + "@foo.com", "foo", "bar", true);
            users.add(user);
        }

        piRep.setUsers(users);
    }

    private void addUsersWithTermsAndConditions() {
        List<UserRepresentation> users = new ArrayList<>();
        List<String> requiredActions = new ArrayList<>();
        requiredActions.add("terms_and_conditions");

        for (int i = 0; i < NUM_ENTITIES; i++) {
            UserRepresentation user = createUserRepresentation(USER_PREFIX + i, USER_PREFIX + i + "@foo.com", "foo", "bar", true);
            user.setRequiredActions(requiredActions);
            users.add(user);
        }

        piRep.setUsers(users);
    }

    private void addGroups() {
        List<GroupRepresentation> groups = new ArrayList<>();

        for (int i=0; i < NUM_ENTITIES; i++) {
            GroupRepresentation group = new GroupRepresentation();
            group.setName(GROUP_PREFIX + i);
            group.setPath("/" + GROUP_PREFIX + i);
            groups.add(group);
        }

        piRep.setGroups(groups);
    }

    private void addClients(boolean withServiceAccounts) throws IOException {
        List<ClientRepresentation> clients = new ArrayList<>();
        List<UserRepresentation> serviceAccounts = new ArrayList<>();

        for (int i = 0; i < NUM_ENTITIES; i++) {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(CLIENT_PREFIX + i);
            client.setName(CLIENT_PREFIX + i);
            clients.add(client);
            if (withServiceAccounts) {
                client.setServiceAccountsEnabled(true);
                client.setBearerOnly(false);
                client.setPublicClient(false);
                client.setAuthorizationSettings(resourceServerSampleSettings);
                client.setAuthorizationServicesEnabled(true);
                // create the user service account
                UserRepresentation serviceAccount = new UserRepresentation();
                serviceAccount.setUsername(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId());
                serviceAccount.setEnabled(true);
                serviceAccount.setEmail(serviceAccount.getUsername() + "@placeholder.org");
                serviceAccount.setServiceAccountClientId(client.getClientId());
                serviceAccounts.add(serviceAccount);
            }
        }

        if (withServiceAccounts) {
            if (piRep.getUsers() == null) {
                piRep.setUsers(new ArrayList<>());
            }
            piRep.getUsers().addAll(serviceAccounts);
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
        assertAdminEvents.clear();

        setFail();
        addUsers();

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        // Need to do this way as admin events from partial import are unsorted
        Set<String> userIds = new HashSet<>();
        for (int i=0 ; i<NUM_ENTITIES ; i++) {
            AdminEventRepresentation adminEvent = assertAdminEvents.poll();
            Assert.assertEquals(realmId, adminEvent.getRealmId());
            Assert.assertEquals(OperationType.CREATE.name(), adminEvent.getOperationType());
            Assert.assertTrue(adminEvent.getResourcePath().startsWith("users/"));
            String userId = adminEvent.getResourcePath().substring(6);
            userIds.add(userId);
        }

        assertAdminEvents.assertEmpty();


        for (PartialImportResult result : results.getResults()) {
            String id = result.getId();
            UserResource userRsc = testRealmResource().users().get(id);
            UserRepresentation user = userRsc.toRepresentation();
            Assert.assertThat(user.getUsername(), startsWith(USER_PREFIX));
            Assert.assertThat(userIds, hasItem(id));
        }
    }

    @Test
    public void testAddUsersWithDuplicateEmailsForbidden() {
        assertAdminEvents.clear();

        setFail();
        addUsers();
        
        UserRepresentation user = createUserRepresentation(USER_PREFIX + 999, USER_PREFIX + 1 + "@foo.com", "foo", "bar", true);
        piRep.getUsers().add(user);

        try (Response response = testRealmResource().partialImport(piRep)) {
            assertEquals(409, response.getStatus());
        }
    }
    
    @Test
    public void testAddUsersWithDuplicateEmailsAllowed() {
        
        RealmRepresentation realmRep = testRealmResource().toRepresentation();
        realmRep.setDuplicateEmailsAllowed(true);
        testRealmResource().update(realmRep);
                
        assertAdminEvents.clear();

        setFail();
        addUsers();
        doImport();
        
        UserRepresentation user = createUserRepresentation(USER_PREFIX + 999, USER_PREFIX + 1 + "@foo.com", "foo", "bar", true);
        piRep.setUsers(Arrays.asList(user));
        
        PartialImportResults results = doImport();
        assertEquals(1, results.getAdded());
    }

    @Test
    public void testAddUsersWithTermsAndConditions() {
        assertAdminEvents.clear();

        setFail();
        addUsersWithTermsAndConditions();

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        // Need to do this way as admin events from partial import are unsorted
        Set<String> userIds = new HashSet<>();
        for (int i=0 ; i<NUM_ENTITIES ; i++) {
            AdminEventRepresentation adminEvent = assertAdminEvents.poll();
            Assert.assertEquals(realmId, adminEvent.getRealmId());
            Assert.assertEquals(OperationType.CREATE.name(), adminEvent.getOperationType());
            Assert.assertTrue(adminEvent.getResourcePath().startsWith("users/"));
            String userId = adminEvent.getResourcePath().substring(6);
            userIds.add(userId);
        }

        assertAdminEvents.assertEmpty();

        for (PartialImportResult result : results.getResults()) {
            String id = result.getId();
            UserResource userRsc = testRealmResource().users().get(id);
            UserRepresentation user = userRsc.toRepresentation();
            assertTrue(user.getUsername().startsWith(USER_PREFIX));
            Assert.assertTrue(userIds.contains(id));
        }
    }

    @Test
    public void testAddClients() throws IOException {
        setFail();
        addClients(false);

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        for (PartialImportResult result : results.getResults()) {
            String id = result.getId();
            ClientResource clientRsc = testRealmResource().clients().get(id);
            ClientRepresentation client = clientRsc.toRepresentation();
            assertTrue(client.getName().startsWith(CLIENT_PREFIX));
        }
    }

    @EnableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
    @Test
    public void testAddClientsWithServiceAccountsAndAuthorization() throws IOException {
        setFail();
        addClients(true);

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES * 2, results.getAdded());

        for (PartialImportResult result : results.getResults()) {
            if (result.getResourceType().equals(ResourceType.CLIENT)) {
                String id = result.getId();
                ClientResource clientRsc = testRealmResource().clients().get(id);
                ClientRepresentation client = clientRsc.toRepresentation();
                assertTrue(client.getName().startsWith(CLIENT_PREFIX));
                Assert.assertTrue(client.isServiceAccountsEnabled());
                if (ProfileAssume.isFeatureEnabled(AUTHORIZATION)) {
                    Assert.assertTrue(client.getAuthorizationServicesEnabled());
                    AuthorizationResource authRsc = clientRsc.authorization();
                    ResourceServerRepresentation authRep = authRsc.exportSettings();
                    Assert.assertNotNull(authRep);
                    Assert.assertEquals(2, authRep.getResources().size());
                    Assert.assertEquals(3, authRep.getPolicies().size());
                } else {
                    Assert.assertNull(client.getAuthorizationServicesEnabled());
                }
            } else {
                UserResource userRsc = testRealmResource().users().get(result.getId());
                Assert.assertTrue(userRsc.toRepresentation().getUsername().startsWith(
                        ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + CLIENT_PREFIX));
            }
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
    public void testAddGroupsFail() {
        addGroups();
        testFail();
    }

    @Test
    public void testAddClientsFail() throws IOException {
        addClients(false);
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
    public void testAddGroupsSkip() {
        addGroups();
        testSkip();
    }

    @Test
    public void testAddClientsSkip() throws IOException {
        addClients(false);
        testSkip();
    }

    @EnableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
    @Test
    public void testAddClientsSkipWithServiceAccountsAndAuthorization() throws IOException {
        addClients(true);
        setSkip();
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES * 2, results.getAdded());

        results = doImport();
        assertEquals(NUM_ENTITIES * 2, results.getSkipped());
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
    public void testAddGroupsOverwrite() {
        addGroups();
        testOverwrite();
    }

    @Test
    public void testAddClientsOverwrite() throws IOException {
        addClients(false);
        testOverwrite();
    }

    @EnableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
    @Test
    public void testAddClientsOverwriteWithServiceAccountsAndAuthorization() throws IOException {
        addClients(true);
        setOverwrite();
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES * 2, results.getAdded());

        results = doImport();
        assertEquals(NUM_ENTITIES * 2, results.getOverwritten());
    }

    @EnableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
    @Test
    public void testAddClientsOverwriteServiceAccountsWithNoServiceAccounts() throws IOException {
        addClients(true);
        setOverwrite();
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES * 2, results.getAdded());
        // check the service accounts are there
        for (int i = 0; i < NUM_ENTITIES; i++) {
            List<UserRepresentation> l = testRealmResource().users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + CLIENT_PREFIX + i);
            Assert.assertEquals(1, l.size());
        }
        // re-import without service accounts enabled
        piRep = new PartialImportRepresentation();
        addClients(false);
        setOverwrite();
        results = doImport();
        assertEquals(NUM_ENTITIES, results.getOverwritten());
        // check the service accounts have been removed
        for (int i = 0; i < NUM_ENTITIES; i++) {
            List<UserRepresentation> l = testRealmResource().users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + CLIENT_PREFIX + i);
            Assert.assertEquals(0, l.size());
        }
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

    private void importEverything(boolean withServiceAccounts) throws IOException {
        addUsers();
        addGroups();
        addClients(withServiceAccounts);
        addProviders();
        addRealmRoles();
        addClientRoles();

        PartialImportResults results = doImport();
        assertNull(results.getErrorMessage());
        if (withServiceAccounts) {
            assertEquals(NUM_ENTITIES * (NUM_RESOURCE_TYPES + 1), results.getAdded());
        } else {
            assertEquals(NUM_ENTITIES * NUM_RESOURCE_TYPES, results.getAdded());
        }
    }

    @Test
    public void testEverythingFail() throws IOException {
        setFail();
        importEverything(false);
        PartialImportResults results = doImport(); // second import will fail because not allowed to skip or overwrite
        assertNotNull(results.getErrorMessage());
    }

    @Test
    public void testEverythingSkip() throws IOException {
        setSkip();
        importEverything(false);
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES * NUM_RESOURCE_TYPES, results.getSkipped());
    }

    @EnableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
    @Test
    public void testEverythingSkipWithServiceAccounts() throws IOException {
        setSkip();
        importEverything(true);
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES * (NUM_RESOURCE_TYPES + 1), results.getSkipped());
    }

    @Test
    public void testEverythingOverwrite() throws IOException {
        setOverwrite();
        importEverything(false);
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES * NUM_RESOURCE_TYPES, results.getOverwritten());
    }

    @EnableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
    @Test
    public void testEverythingOverwriteWithServiceAccounts() throws IOException {
        setOverwrite();
        importEverything(true);
        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES * (NUM_RESOURCE_TYPES + 1), results.getOverwritten());
    }

    //KEYCLOAK-3042
    @Test
    public void testOverwriteExistingClientWithRoles() {
        setOverwrite();

        ClientRepresentation client = adminClient.realm(MASTER).clients().findByClientId("broker").get(0);
        List<RoleRepresentation> clientRoles = adminClient.realm(MASTER).clients().get(client.getId()).roles().list();
        
        Map<String, List<RoleRepresentation>> clients = new HashMap<>();
        clients.put(client.getClientId(), clientRoles);
        
        RolesRepresentation roles = new RolesRepresentation();
        roles.setClient(clients);
        
        piRep.setClients(Arrays.asList(client));
        piRep.setRoles(roles);
                
        doImport();
    }

    // KEYCLOAK-6058
    @Test
    public void testOverwriteExistingInternalClient() {
        setOverwrite();
        ClientRepresentation client = adminClient.realm(MASTER).clients().findByClientId("security-admin-console").get(0);
        ClientRepresentation client2 = adminClient.realm(MASTER).clients().findByClientId("master-realm").get(0);
        piRep.setClients(Arrays.asList(client, client2));

        PartialImportResults result = doImport();
        Assert.assertEquals(0, result.getOverwritten());
    }

    @Test
    public void testOverwriteExistingClientWithServiceAccount() {
        setOverwrite();
        piRep.setClients(Arrays.asList(testRealmResource().clients().findByClientId(CLIENT_SERVICE_ACCOUNT).get(0)));

        Assert.assertEquals(1, doImport().getOverwritten());

        ClientRepresentation client = testRealmResource().clients().findByClientId(CLIENT_SERVICE_ACCOUNT).get(0);
        testRealmResource().clients().get(client.getId()).getServiceAccountUser();
    }

}
