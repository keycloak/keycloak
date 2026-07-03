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

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaRealmProvider;
import org.keycloak.models.jpa.RoleAdapter;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeNotNull;

@RequireProvider(RealmProvider.class)
@RequireProvider(ClientProvider.class)
@RequireProvider(ClientScopeProvider.class)
@RequireProvider(GroupProvider.class)
@RequireProvider(RoleProvider.class)
@RequireProvider(value = OrganizationProvider.class, only = "jpa")
public class OrganizationRoleModelTest extends KeycloakModelTest {

    private static final String REALM_NAME = "organization-role-realm";
    private static final String CLIENT_ID = "client-with-roles";
    private static final String ACME_ID = "org-acme";
    private static final String OTHER_ID = "org-other";

    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession session) {
        RealmModel realm = createRealm(session, REALM_NAME);
        session.getContext().setRealm(realm);
        realm.setOrganizationsEnabled(true);
        realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        session.clients().addClient(realm, CLIENT_ID);

        OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
        organizations.create(ACME_ID, "Acme", "acme");
        organizations.create(OTHER_ID, "Other", "other");

        realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);
        session.realms().removeRealm(realmId);
    }

    @Test
    public void shouldCreateDefaultOrganizationRole() {
        withRealm(realmId, (session, realm) -> {
            OrganizationModel organization = getOrganization(session, ACME_ID);
            RoleModel defaultRole = organization.getDefaultRole();

            assertThat(defaultRole, notNullValue());
            assertThat(defaultRole.getName(), is(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme"));
            assertThat(defaultRole.getType(), is(RoleModel.Type.ORGANIZATION));
            assertThat(defaultRole.isOrganizationRole(), is(true));
            assertThat(defaultRole.getType(), is(RoleModel.Type.ORGANIZATION));
            assertThat(defaultRole.getContainerId(), is(organization.getId()));
            assertThat(defaultRole.getContainer().getId(), is(organization.getId()));
            assertThat(session.roles().getRoleById(organization, defaultRole.getId()).getId(), is(defaultRole.getId()));
            assertThat(session.roles().getRoleById(getOrganization(session, OTHER_ID), defaultRole.getId()), nullValue());

            return null;
        });
    }

    @Test
    public void shouldIsolateOrganizationRolesFromRealmAndClientRoles() {
        withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);

            RoleModel acmeRole = acme.addRole("shared");
            RoleModel otherRole = other.addRole("shared");
            RoleModel realmRole = session.roles().addRealmRole(realm, "shared");
            RoleModel clientRole = session.roles().addClientRole(client, "shared");

            assertThrows(ModelDuplicateException.class, () -> acme.addRole("shared"));
            assertThat(acme.getRole("shared").getId(), is(acmeRole.getId()));
            assertThat(session.roles().getOrganizationRole(acme, "shared").getId(), is(acmeRole.getId()));
            assertThat(session.roles().getOrganizationRole(other, "shared").getId(), is(otherRole.getId()));
            assertThat(session.roles().getRealmRole(realm, "shared").getId(), is(realmRole.getId()));
            assertThat(session.roles().getClientRole(client, "shared").getId(), is(clientRole.getId()));
            assertThat(realmRole.getType(), is(RoleModel.Type.REALM));
            assertThat(realmRole.getContainerId(), is(realm.getId()));
            assertThat(realmRole.getContainer().getId(), is(realm.getId()));
            assertThat(clientRole.getType(), is(RoleModel.Type.CLIENT));
            assertThat(clientRole.getContainerId(), is(client.getId()));
            assertThat(clientRole.getContainer().getId(), is(client.getId()));

            assertThat(roleNames(session.roles().searchForRolesStream(realm, "shared", null, null)), contains("shared"));
            assertThat(roleNames(session.roles().searchForClientRolesStream(client, "shared", null, null)), contains("shared"));
            assertThat(roleNames(session.roles().searchForClientRolesStream(realm, List.of(clientRole.getId()).stream(), "shared", null, null)), contains("shared"));
            assertThat(roleNames(acme.getRolesStream()), containsInAnyOrder(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme", "shared"));
            assertThat(roleNames(acme.searchForRolesStream("shared", null, null)), contains("shared"));
            assertThat(roleNames(acme.searchForRolesStream(" ", null, null)), containsInAnyOrder(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme", "shared"));
            assertThat(session.roles().getOrganizationRolesCount(acme, "shared"), is(1L));
            assertThat(session.roles().getOrganizationRolesCount(acme), is(2L));
            assertThat(roleNames(acme.getRolesStream(1, 1)), contains("shared"));

            RoleModel removableRole = acme.addRole("removable");
            assertThat(acme.removeRole(removableRole), is(true));

            return null;
        });
    }

    @Test
    public void shouldAssignAndRemoveOrganizationRolesWithMembership() {
        String[] ids = withRealm(realmId, (session, realm) -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session, ACME_ID);
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            UserModel user = session.users().addUser(realm, "member");
            RoleModel defaultRole = organization.getDefaultRole();
            RoleModel customRole = organization.addRole("project-admin");
            RoleModel clientRole = session.roles().addClientRole(client, "bulk-client-role");

            session.users().grantToAllUsers(realm, customRole);
            assertThat(user.hasDirectRole(customRole), is(false));
            session.users().grantToAllUsers(realm, clientRole);
            assertThat(roleIds(user.getClientRoleMappingsStream(client)), contains(clientRole.getId()));

            organizations.addMember(organization, user);
            user.grantRole(customRole);

            assertThat(user.hasDirectRole(defaultRole), is(true));
            assertThat(user.hasDirectRole(customRole), is(true));

            organizations.removeMember(organization, user);

            assertThat(user.hasDirectRole(defaultRole), is(false));
            assertThat(user.hasDirectRole(customRole), is(false));
            assertThat(organizations.getMemberById(organization, user.getId()), nullValue());

            return new String[] { user.getId(), defaultRole.getId(), customRole.getId() };
        });

        withRealm(realmId, (session, realm) -> {
            UserModel user = session.users().getUserById(realm, ids[0]);
            assertThat(user.hasDirectRole(session.roles().getRoleById(realm, ids[1])), is(false));
            assertThat(user.hasDirectRole(session.roles().getRoleById(realm, ids[2])), is(false));
            return null;
        });
    }

    @Test
    public void shouldRejectOrganizationRoleMappingsForNonMembers() {
        withRealm(realmId, (session, realm) -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);
            UserModel user = session.users().addUser(realm, "not-an-organization-member");
            RoleModel acmeRole = acme.addRole("member-only-role");
            RoleModel otherRole = other.addRole("other-member-only-role");

            assertThrows(ModelException.class, () -> user.grantRole(acmeRole));
            assertThat(user.hasDirectRole(acmeRole), is(false));

            organizations.addMember(acme, user);
            assertThrows(ModelException.class, () -> user.grantRole(otherRole));
            assertThat(user.hasDirectRole(otherRole), is(false));

            user.grantRole(acmeRole);
            assertThat(user.hasDirectRole(acmeRole), is(true));

            return null;
        });
    }

    @Test
    public void shouldValidateDirectFederatedRoleMappings() {
        withRealm(realmId, (session, realm) -> {
            UserFederatedStorageProvider federatedStorage = session.getProvider(UserFederatedStorageProvider.class);
            assumeNotNull(federatedStorage);

            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session, ACME_ID);
            UserModel user = session.users().addUser(realm, "federated-role-member");
            RoleModel role = organization.addRole("federated-role");

            assertThrows(ModelException.class, () -> federatedStorage.grantRole(realm, user.getId(), role));

            organizations.addMember(organization, user);
            federatedStorage.grantRole(realm, user.getId(), role);
            assertThat(roleIds(federatedStorage.getRoleMappingsStream(realm, user.getId())), contains(role.getId()));

            return null;
        });
    }

    @Test
    public void shouldRejectOrganizationRoleMappingsForGroups() {
        withRealm(realmId, (session, realm) -> {
            OrganizationModel organization = getOrganization(session, ACME_ID);
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            GroupModel group = session.groups().createGroup(realm, "organization-role-group");
            RoleModel role = organization.addRole("group-only-role");
            RoleModel realmRole = session.roles().addRealmRole(realm, "realm-group-role");
            RoleModel clientRole = session.roles().addClientRole(client, "client-group-role");

            assertThrows(ModelException.class, () -> group.grantRole(role));
            assertThat(group.hasDirectRole(role), is(false));
            group.grantRole(realmRole);
            group.grantRole(clientRole);
            assertThat(group.hasDirectRole(realmRole), is(true));
            assertThat(roleIds(group.getClientRoleMappingsStream(client)), contains(clientRole.getId()));

            return null;
        });
    }

    @Test
    public void shouldValidateOrganizationRoleComposites() {
        withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            RoleModel parent = acme.addRole("composite-parent");
            RoleModel child = acme.addRole("composite-child");
            RoleModel otherRole = other.addRole("composite-child");
            RoleModel realmRole = session.roles().addRealmRole(realm, "composite-realm-role");
            RoleModel clientRole = session.roles().addClientRole(client, "composite-client-role");

            parent.addCompositeRole(child);
            parent.addCompositeRole(realmRole);
            parent.addCompositeRole(clientRole);

            assertThat(roleIds(parent.getCompositesStream()), containsInAnyOrder(child.getId(), realmRole.getId(), clientRole.getId()));
            assertThrows(ModelException.class, () -> parent.addCompositeRole(otherRole));
            assertThrows(ModelException.class, () -> realmRole.addCompositeRole(child));
            assertThrows(ModelException.class, () -> clientRole.addCompositeRole(child));

            return null;
        });
    }

    @Test
    public void shouldRejectOrganizationRolesInScopeMappings() {
        withRealm(realmId, (session, realm) -> {
            OrganizationModel organization = getOrganization(session, ACME_ID);
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            ClientScopeModel clientScope = session.clientScopes().addClientScope(realm, "organization-role-client-scope");
            RoleModel role = organization.addRole("scope-only-role");
            RoleModel realmRole = session.roles().addRealmRole(realm, "realm-scope-role");

            assertThrows(ModelException.class, () -> client.addScopeMapping(role));
            assertThrows(ModelException.class, () -> clientScope.addScopeMapping(role));
            client.addScopeMapping(realmRole);
            clientScope.addScopeMapping(realmRole);
            assertThat(client.hasDirectScope(realmRole), is(true));
            assertThat(clientScope.hasDirectScope(realmRole), is(true));

            return null;
        });
    }

    @Test
    public void shouldProtectDefaultRoleAndRemoveRolesWithOrganization() {
        String[] roleIds = withRealm(realmId, (session, realm) -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = getOrganization(session, ACME_ID);
            RoleModel defaultRole = organization.getDefaultRole();
            RoleModel customRole = organization.addRole("project-admin");

            defaultRole.addCompositeRole(customRole);

            assertThrows(ModelException.class, () -> session.roles().removeRole(defaultRole));

            organizations.remove(organization);

            assertThat(organizations.getById(ACME_ID), nullValue());
            return new String[] { defaultRole.getId(), customRole.getId() };
        });

        withRealm(realmId, (session, realm) -> {
            assertThat(session.roles().getRoleById(realm, roleIds[0]), nullValue());
            assertThat(session.roles().getRoleById(realm, roleIds[1]), nullValue());
            assertThat(roleNames(session.roles().searchForRolesStream(realm, "default-roles-acme", null, null)), empty());
            return null;
        });
    }

    @Test
    public void shouldValidateDefaultOrganizationRoleContainer() {
        withRealm(realmId, (session, realm) -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);
            RoleModel originalDefaultRole = acme.getDefaultRole();
            RoleModel realmRole = session.roles().addRealmRole(realm, "realm-default-candidate");
            RoleModel otherOrganizationRole = other.addRole("other-default-candidate");

            assertThrows(ModelException.class, () -> acme.setDefaultRole(realmRole));
            assertThrows(ModelException.class, () -> acme.setDefaultRole(otherOrganizationRole));

            acme.setDefaultRole(null);
            assertThat(acme.getDefaultRole(), nullValue());

            acme.setDefaultRole(originalDefaultRole);
            assertThat(acme.getDefaultRole().getId(), is(originalDefaultRole.getId()));

            return null;
        });
    }

    @Test
    public void shouldReturnNullWhenOrganizationRoleContainerCannotBeResolved() {
        withRealm(realmId, (session, realm) -> {
            OrganizationModel organization = getOrganization(session, ACME_ID);
            RoleModel role = organization.addRole("orphaned-container");
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            RoleEntity entity = em.find(RoleEntity.class, role.getId());
            RoleModel mismatchedRole = new RoleAdapter(session, realmWithId("another-realm"), em, entity);

            assertThat(mismatchedRole.getContainer(), nullValue());

            return null;
        });
    }

    @Test
    public void shouldRejectRolesWithUnsupportedContainers() {
        withRealm(realmId, (session, realm) -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            JpaRealmProvider provider = new JpaRealmProvider(session, em, null, null);

            assertThrows(IllegalStateException.class, () -> provider.removeRole(roleWithoutContainer()));
            return null;
        });
    }

    private OrganizationModel getOrganization(KeycloakSession session, String id) {
        return session.getProvider(OrganizationProvider.class).getById(id);
    }

    private List<String> roleNames(java.util.stream.Stream<RoleModel> roles) {
        return roles.map(RoleModel::getName).collect(Collectors.toList());
    }

    private List<String> roleIds(java.util.stream.Stream<RoleModel> roles) {
        return roles.map(RoleModel::getId).collect(Collectors.toList());
    }

    private RoleModel roleWithoutContainer() {
        return (RoleModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { RoleModel.class },
                (proxy, method, args) -> "getId".equals(method.getName()) ? "role-without-container" : null);
    }

    private RealmModel realmWithId(String id) {
        return (RealmModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> "getId".equals(method.getName()) ? id : null);
    }
}
