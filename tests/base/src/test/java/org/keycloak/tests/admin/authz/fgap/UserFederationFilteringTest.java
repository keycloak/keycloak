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

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.providers.federation.UserMapStorageFactory;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that federated (non-imported) users are excluded from search results
 * when FGAP v2 partial evaluation is active, preventing disclosure of federated
 * user representations to delegated admins with only query-users permission.
 */
@KeycloakIntegrationTest(config = UserFederationFilteringTest.ServerConfig.class)
public class UserFederationFilteringTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private String fedUsername;
    private String fedUserId;

    @BeforeEach
    public void onBeforeEach() {
        fedUsername = "fed-user-" + KeycloakModelUtils.generateId().substring(0, 8);

        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName("memory");
        memProvider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));
        memProvider.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));

        String memProviderId = ApiUtil.getCreatedId(realm.admin().components().add(memProvider));
        realm.cleanup().add(r -> r.components().component(memProviderId).remove());

        UserRepresentation fedUser = new UserRepresentation();
        fedUser.setUsername(fedUsername);
        fedUser.setEnabled(true);
        fedUser.setFederationLink(memProviderId);
        fedUserId = ApiUtil.getCreatedId(realm.admin().users().create(fedUser));
        assertFalse(StorageId.isLocalStorage(fedUserId));
    }

    @Test
    @DatabaseTest
    public void testDelegatedAdminCannotSearchFederatedUsers() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(fedUsername, 0, 10);
        assertThat(search, empty());
        assertThat(realmAdminClient.realm(realm.getName()).users().count(fedUsername), is(0));
    }

    @Test
    public void testPrivilegedAdminCanSearchFederatedUsers() {
        List<UserRepresentation> search = realm.admin().users().search(fedUsername, 0, 10);
        assertThat(search, hasSize(1));
        assertThat(search.get(0).getUsername(), is(fedUsername));
        assertThat(realm.admin().users().count(fedUsername), is(1));
    }

    @Test
    @DatabaseTest
    public void testDelegatedAdminCannotSeeRoleMembersFromFederatedStorage() {
        String myAdminId = realm.admin().users().search("myadmin").get(0).getId();
        String clientUuid = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewRealm = realm.admin().clients().get(clientUuid).roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        realm.admin().users().get(myAdminId).roles().clientLevel(clientUuid).add(List.of(viewRealm));

        String roleName = "test-role-" + KeycloakModelUtils.generateId().substring(0, 8);
        realm.admin().roles().create(new RoleRepresentation(roleName, null, false));
        realm.cleanup().add(r -> r.roles().deleteRole(roleName));
        RoleRepresentation role = realm.admin().roles().get(roleName).toRepresentation();

        realm.admin().users().get(fedUserId).roles().realmLevel().add(List.of(role));

        List<UserRepresentation> members = realm.admin().roles().get(roleName).getUserMembers(0, 10);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getUsername(), is(fedUsername));

        List<UserRepresentation> delegatedMembers = realmAdminClient.realm(realm.getName()).roles().get(roleName).getUserMembers(0, 10);
        assertThat(delegatedMembers, empty());
    }

    @Test
    @DatabaseTest
    public void testDelegatedAdminWithViewUsersCanSearchFederatedUsers() {
        String myAdminId = realm.admin().users().search("myadmin").get(0).getId();
        String clientUuid = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewUsers = realm.admin().clients().get(clientUuid).roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        realm.admin().users().get(myAdminId).roles().clientLevel(clientUuid).add(List.of(viewUsers));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(fedUsername, 0, 10);
        assertThat(search, hasSize(1));
        assertThat(search.get(0).getUsername(), is(fedUsername));
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
