/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.organization.admin;

import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.storage.RoleStorageManager;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.role.RoleStorageProviderModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.federation.AdaptedRoleStorageProvider;
import org.keycloak.tests.providers.federation.AdaptedRoleStorageProviderFactory;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class GenericRoleStorageModelTest {

    private static final String OTHER_CLIENT_ID = "spi-other-client";
    private static final String EMPTY_CLIENT_ID = "spi-empty-client";

    @InjectRealm(config = GenericRoleStorageRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void shouldCountAndSearchEveryLocalRoleContainerType() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, AdaptedRoleStorageProvider.CLIENT_ID);
            ClientModel otherClient = session.clients().getClientByClientId(realm, OTHER_CLIENT_ID);
            ClientModel emptyClient = session.clients().getClientByClientId(realm, EMPTY_CLIENT_ID);
            OrganizationModel organization = createOrganization(session, "spi-count-org", "spi-count-org");
            OrganizationModel otherOrganization = createOrganization(session, "spi-count-other-org", "spi-count-other-org");

            addRole(session, realm, "e1-parity-shared", "realm-name-hit");
            addRole(session, realm, "e1-parity-realm-second", "realm-description-hit");
            addRole(session, client, "e1-parity-shared", "client-name-hit");
            addRole(session, client, "e1-parity-client-second", "client-description-hit");
            addRole(session, otherClient, "e1-parity-shared", "other-client-description");
            addRole(session, organization, "e1-parity-shared", "organization-name-hit");
            addRole(session, organization, "e1-parity-organization-second", "organization-description-hit");
            addRole(session, otherOrganization, "e1-parity-shared", "other-organization-description");

            assertCountParity(session, realm, null);
            assertCountParity(session, realm, "  ");
            assertCountParity(session, client, null);
            assertCountParity(session, client, "  ");
            assertCountParity(session, organization, null);
            assertCountParity(session, organization, "  ");

            for (RoleContainerModel container : List.of(realm, client, organization)) {
                assertCountParity(session, container, "E1-PARITY-SHARED");
                assertCountParity(session, container, "description-hit");
                assertCountParity(session, container, "missing-role");
                assertThat(session.roles().getRolesCount(container, "e1-parity-shared"), is(1L));
                assertThat(session.roles().getRolesCount(container, "e1-parity-"), is(2L));
                assertThat(session.roles().getRolesCount(container, "missing-role"), is(0L));
            }

            assertThat(session.roles().getRolesCount(otherClient, "e1-parity-shared"), is(1L));
            assertThat(session.roles().getRolesCount(otherOrganization, "e1-parity-shared"), is(1L));
            assertThat(session.roles().getRolesCount(emptyClient, null), is(0L));
        });
    }

    @Test
    public void shouldRouteGenericContainerLookupToAnAdaptedExternalProvider() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, AdaptedRoleStorageProvider.CLIENT_ID);
            ClientModel otherClient = session.clients().getClientByClientId(realm, OTHER_CLIENT_ID);
            OrganizationModel organization = createOrganization(session, "spi-provider-org", "spi-provider-org");
            RoleModel localRealmRole = session.roles().addRole(realm, "local-realm-role");
            RoleModel localClientRole = session.roles().addRole(client, "local-client-role");

            RoleStorageProviderModel component = new RoleStorageProviderModel();
            component.setName("adapted-role-storage");
            component.setProviderId(AdaptedRoleStorageProviderFactory.PROVIDER_ID);
            component.setParentId(realm.getId());
            component = new RoleStorageProviderModel(realm.addComponentModel(component));

            String realmRoleId = new StorageId(component.getId(), AdaptedRoleStorageProvider.REALM_ROLE_EXTERNAL_ID).getId();
            String clientRoleId = new StorageId(component.getId(), AdaptedRoleStorageProvider.CLIENT_ROLE_EXTERNAL_ID).getId();
            RoleStorageManager uncachedRoles = new RoleStorageManager(session, 3000);

            assertThat(uncachedRoles.getRoleInContainerById(realm, localRealmRole.getId()).getId(), is(localRealmRole.getId()));
            assertThat(uncachedRoles.getRoleInContainerById(client, localClientRole.getId()).getId(), is(localClientRole.getId()));
            assertExternalRole(uncachedRoles.getRoleInContainerById(realm, realmRoleId), realmRoleId, RoleModel.Type.REALM, realm);
            assertExternalRole(uncachedRoles.getRoleInContainerById(client, clientRoleId), clientRoleId, RoleModel.Type.CLIENT, client);

            assertExternalRole(session.roles().getRoleInContainerById(realm, realmRoleId), realmRoleId, RoleModel.Type.REALM, realm);
            assertExternalRole(session.roles().getRoleInContainerById(client, clientRoleId), clientRoleId, RoleModel.Type.CLIENT, client);
            assertExternalRole(session.roles().getRoleInContainerById(realm, realmRoleId), realmRoleId, RoleModel.Type.REALM, realm);
            assertExternalRole(session.roles().getRoleInContainerById(client, clientRoleId), clientRoleId, RoleModel.Type.CLIENT, client);

            assertThat(uncachedRoles.getRoleInContainerById(realm, clientRoleId), nullValue());
            assertThat(uncachedRoles.getRoleInContainerById(client, realmRoleId), nullValue());
            assertThat(uncachedRoles.getRoleInContainerById(otherClient, clientRoleId), nullValue());
            assertThat(uncachedRoles.getRoleInContainerById(organization, clientRoleId), nullValue());
            assertThat(uncachedRoles.getRoleInContainerById(realm,
                    new StorageId(component.getId(), "missing-role").getId()), nullValue());
            assertThat(uncachedRoles.getRoleInContainerById(realm,
                    new StorageId("missing-provider", AdaptedRoleStorageProvider.REALM_ROLE_EXTERNAL_ID).getId()), nullValue());

            RealmModel otherRealm = session.realms().createRealm("spi-other-realm");
            try {
                ClientModel sameClientIdInOtherRealm = session.clients().addClient(otherRealm,
                        AdaptedRoleStorageProvider.CLIENT_ID);
                assertThat(uncachedRoles.getRoleInContainerById(otherRealm, realmRoleId), nullValue());
                assertThat(uncachedRoles.getRoleInContainerById(sameClientIdInOtherRealm, clientRoleId), nullValue());
            } finally {
                session.realms().removeRealm(otherRealm.getId());
            }
        });
    }

    @Test
    public void shouldKeepExternalRealmRoleMappingsForOrganizationGroups() {
        String[] state = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = createOrganization(session, "external-group-role-org",
                    "external-group-role-org");
            GroupModel group = organizations.createGroup(organization, "external-role-group", null);
            UserModel user = session.users().addUser(realm, "external-role-user");
            organizations.addMember(organization, user);
            user.joinGroup(group);

            RoleStorageProviderModel component = new RoleStorageProviderModel();
            component.setId("egrs");
            component.setName("external-group-role-storage");
            component.setProviderId(AdaptedRoleStorageProviderFactory.PROVIDER_ID);
            component.setParentId(realm.getId());
            component = new RoleStorageProviderModel(realm.addComponentModel(component));

            String roleId = new StorageId(component.getId(), AdaptedRoleStorageProvider.REALM_ROLE_EXTERNAL_ID).getId();
            RoleModel externalRole = session.roles().getRoleById(realm, roleId);

            group.grantRole(externalRole);

            assertThat(group.hasDirectRole(externalRole), is(true));
            assertThat(group.getRoleMappingsStream().map(RoleModel::getId).toList(), is(List.of(roleId)));
            return new String[] { group.getId(), user.getId(), roleId };
        }, String[].class);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            GroupModel group = realm.getGroupById(state[0]);
            UserModel user = session.users().getUserById(realm, state[1]);
            RoleModel externalRole = session.roles().getRoleById(realm, state[2]);

            assertThat(group.hasDirectRole(externalRole), is(true));
            assertThat(group.getRoleMappingsStream().map(RoleModel::getId).toList(), is(List.of(state[2])));
            assertThat(user.hasRole(externalRole), is(true));
        });
    }

    @Test
    public void shouldRejectExternalRolesAsJpaComposites() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RoleModel localRole = session.roles().addRealmRole(realm, "external-composite-parent");
            RoleStorageProviderModel component = new RoleStorageProviderModel();
            component.setName("external-composite-role-storage");
            component.setProviderId(AdaptedRoleStorageProviderFactory.PROVIDER_ID);
            component.setParentId(realm.getId());
            component = new RoleStorageProviderModel(realm.addComponentModel(component));
            String roleId = new StorageId(component.getId(), AdaptedRoleStorageProvider.REALM_ROLE_EXTERNAL_ID).getId();
            RoleModel externalRole = session.roles().getRoleById(realm, roleId);

            assertThat(assertThrows(ModelValidationException.class, () -> localRole.addCompositeRole(externalRole)).getMessage(),
                    is("Role does not exist in local storage"));
        });
    }

    private static RoleModel addRole(KeycloakSession session, RoleContainerModel container, String name, String description) {
        RoleModel role = session.roles().addRole(container, name);
        role.setDescription(description);
        return role;
    }

    private static OrganizationModel createOrganization(KeycloakSession session, String id, String alias) {
        return session.getProvider(OrganizationProvider.class).create(id, id, alias);
    }

    private static void assertCountParity(KeycloakSession session, RoleContainerModel container, String search) {
        long listed = container.searchForRolesStream(search, null, null).count();
        assertThat(session.roles().getRolesCount(container, search), is(listed));
    }

    private static void assertExternalRole(RoleModel role, String id, RoleModel.Type type, RoleContainerModel container) {
        assertThat(role, notNullValue());
        assertThat(role.getId(), is(id));
        assertThat(role.getType(), is(type));
        assertThat(role.getContainerId(), is(container.getId()));
        assertThat(role.getContainer().getRealm().getId(), is(container.getRealm().getId()));
    }

    public static final class GenericRoleStorageRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true)
                    .clients(ClientBuilder.create(AdaptedRoleStorageProvider.CLIENT_ID),
                            ClientBuilder.create(OTHER_CLIENT_ID), ClientBuilder.create(EMPTY_CLIENT_ID));
        }
    }
}
