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

package org.keycloak.testsuite.model.organization;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@RequireProvider(RealmProvider.class)
@RequireProvider(ClientProvider.class)
@RequireProvider(RoleProvider.class)
@RequireProvider(CacheRealmProvider.class)
@RequireProvider(value = OrganizationProvider.class, only = "infinispan")
public class OrganizationRoleCacheModelTest extends KeycloakModelTest {

    private static final String REALM_NAME = "organization-role-cache-realm";
    private static final String CLIENT_ID = "cache-client-with-roles";
    private static final String ACME_ID = "cache-org-acme";
    private static final String OTHER_ID = "cache-org-other";

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession session) {
        RealmModel realm = createRealm(session, REALM_NAME);
        session.getContext().setRealm(realm);
        realm.setOrganizationsEnabled(true);
        realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        session.clients().addClient(realm, CLIENT_ID);

        OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
        organizations.create(ACME_ID, "Cache Acme", "cache-acme");
        organizations.create(OTHER_ID, "Cache Other", "cache-other");

        realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);
        session.realms().removeRealm(realmId);
    }

    @Test
    public void shouldCacheOrganizationRoleContainerAndLookups() {
        String[] ids = withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);

            RoleModel organizationRole = acme.addRole("project-admin");
            RoleModel directRole = session.roles().addOrganizationRole(acme, "direct-cache-role");
            RoleModel otherRole = other.addRole("project-admin");
            RoleModel realmRole = session.roles().addRealmRole(realm, "cache-realm-role");
            RoleModel clientRole = session.roles().addClientRole(client, "cache-client-role");

            assertThat(roleNames(acme.getRolesStream()), hasItem("project-admin"));
            assertThat(acme.getRole("project-admin").getId(), is(organizationRole.getId()));

            return new String[] { organizationRole.getId(), directRole.getId(), otherRole.getId(), realmRole.getId(), clientRole.getId() };
        });

        withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);

            assertThat(acme.getRealm().getId(), is(realm.getId()));
            assertThat(acme.getDefaultRole(), notNullValue());

            RoleModel organizationRole = session.roles().getRoleById(acme, ids[0]);
            assertThat(organizationRole, notNullValue());
            assertThat(session.roles().getRoleById(acme, ids[0]).getId(), is(ids[0]));
            assertThat(session.roles().getRoleById(other, ids[0]), nullValue());
            assertThat(session.roles().getRoleById(acme, ids[3]), nullValue());
            RoleModel realmRole = session.roles().getRoleById(realm, ids[3]);
            RoleModel clientRole = session.roles().getRoleById(realm, ids[4]);
            assertThat(session.roles().getRoleById(realm, ids[0]).getType(), is(RoleModel.Type.ORGANIZATION));
            assertThat(realmRole.getType(), is(RoleModel.Type.REALM));
            assertThat(realmRole.getContainerId(), is(realm.getId()));
            assertThat(realmRole.getContainer().getId(), is(realm.getId()));
            assertThat(clientRole.getType(), is(RoleModel.Type.CLIENT));
            assertThat(clientRole.getContainerId(), is(session.clients().getClientByClientId(realm, CLIENT_ID).getId()));
            assertThat(clientRole.getContainer().getId(), is(session.clients().getClientByClientId(realm, CLIENT_ID).getId()));
            assertThat(organizationRole.getType(), is(RoleModel.Type.ORGANIZATION));
            assertThat(organizationRole.isOrganizationRole(), is(true));
            assertThat(organizationRole.getContainerId(), is(acme.getId()));
            assertThat(organizationRole.getContainer().getId(), is(acme.getId()));
            session.getProvider(CacheRealmProvider.class).registerRoleInvalidation(ids[0], organizationRole.getName(), acme.getId());
            assertThat(organizationRole.getName(), is("project-admin"));

            assertThat(acme.getRole("project-admin").getId(), is(ids[0]));
            assertThat(acme.getRole("project-admin").getId(), is(ids[0]));
            assertThat(roleNames(acme.getRolesStream()), containsInAnyOrder(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-cache-acme", "project-admin", "direct-cache-role"));
            assertThat(roleNames(acme.getRolesStream()), containsInAnyOrder(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-cache-acme", "project-admin", "direct-cache-role"));
            assertThat(roleNames(acme.searchForRolesStream("project", null, null)), contains("project-admin"));
            assertThat(roleNames(acme.getRolesStream(1, 1)), containsInAnyOrder("direct-cache-role"));
            assertThat(session.roles().getOrganizationRolesCount(acme, "project"), is(1L));
            assertThat(other.getRole("project-admin").getId(), is(ids[2]));
            RoleModel removableRole = acme.addRole("adapter-remove-role");
            assertThat(acme.removeRole(removableRole), is(true));

            return null;
        });
    }

    @Test
    public void shouldInvalidateOrganizationRoleRenameAndComposites() {
        String[] ids = withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel role = acme.addRole("before-rename");
            RoleModel child = acme.addRole("child-composite");
            return new String[] { role.getId(), child.getId() };
        });

        withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel role = session.roles().getRoleById(acme, ids[0]);
            RoleModel child = session.roles().getRoleById(acme, ids[1]);

            role.setName("after-rename");
            role.addCompositeRole(child);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel role = acme.getRole("after-rename");
            RoleModel child = session.roles().getRoleById(acme, ids[1]);

            assertThat(acme.getRole("before-rename"), nullValue());
            assertThat(role.getCompositesStream().map(RoleModel::getId).collect(Collectors.toList()), contains(ids[1]));

            role.removeCompositeRole(child);
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel role = acme.getRole("after-rename");

            assertThat(role.getCompositesStream().collect(Collectors.toList()), empty());
            return null;
        });
    }

    @Test
    public void shouldInvalidateStaleOrganizationRoleQueries() {
        String roleId = withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel role = acme.addRole("stale-cache-role");

            return role.getId();
        });

        withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);

            assertThat(acme.getRole("stale-cache-role").getId(), is(roleId));
            assertThat(roleNames(acme.getRolesStream()), hasItem("stale-cache-role"));

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            RoleEntity entity = em.find(RoleEntity.class, roleId);
            em.remove(entity);
            session.getProvider(CacheRealmProvider.class).registerInvalidation(roleId);

            assertThat(roleNames(acme.getRolesStream()), not(hasItem("stale-cache-role")));
            assertThat(acme.getRole("stale-cache-role"), nullValue());

            return null;
        });
    }

    @Test
    public void shouldInvalidateRolesWhenRemovingRolesOrOrganization() {
        String[] clearedRoleIds = withRealm(realmId, (session, realm) -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.create("cache-org-clear", "Cache Clear", "cache-clear");
            RoleModel defaultRole = organization.getDefaultRole();
            RoleModel customRole = organization.addRole("clear-custom-role");

            assertThat(roleNames(organization.getRolesStream()), hasItem("clear-custom-role"));
            session.roles().removeRoles(organization);
            assertThat(organization.getRolesStream().collect(Collectors.toList()), empty());

            return new String[] { defaultRole.getId(), customRole.getId() };
        });

        withRealm(realmId, (session, realm) -> {
            assertThat(session.roles().getRoleById(realm, clearedRoleIds[0]), nullValue());
            assertThat(session.roles().getRoleById(realm, clearedRoleIds[1]), nullValue());
            return null;
        });

        String[] removedRoleIds = withRealm(realmId, (session, realm) -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.create("cache-org-delete", "Cache Delete", "cache-delete");
            RoleModel defaultRole = organization.getDefaultRole();
            RoleModel customRole = organization.addRole("delete-custom-role");

            assertThat(organization.getRole("delete-custom-role").getId(), is(customRole.getId()));
            assertThat(roleNames(organization.getRolesStream()), hasItem("delete-custom-role"));
            assertThat(organizations.remove(organization), is(true));

            return new String[] { defaultRole.getId(), customRole.getId() };
        });

        withRealm(realmId, (session, realm) -> {
            assertThat(session.roles().getRoleById(realm, removedRoleIds[0]), nullValue());
            assertThat(session.roles().getRoleById(realm, removedRoleIds[1]), nullValue());
            assertThat(getOrganization(session, "cache-org-delete"), nullValue());
            return null;
        });
    }

    @Test
    public void shouldInvalidateDefaultRoleRelationshipInOrganizationCache() {
        withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel defaultRole = acme.getDefaultRole();

            acme.setDefaultRole(null);
            assertThat(acme.getDefaultRole(), nullValue());

            acme.setDefaultRole(defaultRole);
            assertThat(acme.getDefaultRole().getId(), is(defaultRole.getId()));

            return null;
        });
    }

    private OrganizationModel getOrganization(KeycloakSession session, String id) {
        return session.getProvider(OrganizationProvider.class).getById(id);
    }

    private List<String> roleNames(java.util.stream.Stream<RoleModel> roles) {
        return roles.map(RoleModel::getName).collect(Collectors.toList());
    }
}
