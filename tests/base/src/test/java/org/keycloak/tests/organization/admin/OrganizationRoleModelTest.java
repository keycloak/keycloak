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

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class OrganizationRoleModelTest {

    private static final String CLIENT_ID = "client-with-roles";
    private static final String ACME_ID = "org-acme";
    private static final String OTHER_ID = "org-other";

    @InjectRealm(config = OrganizationRoleRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public void createOrganizations() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RoleModel defaultRole = session.roles().getRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName());

            if (defaultRole == null) {
                defaultRole = session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName());
            }

            realm.setDefaultRole(defaultRole);

            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            organizations.create(ACME_ID, "Acme", "acme");
            organizations.create(OTHER_ID, "Other", "other");
        });
    }

    @Test
    public void shouldCreateAndIsolateOrganizationRoles() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            RoleModel defaultRole = acme.getDefaultRole();
            RoleModel acmeRole = acme.addRole("shared");
            RoleModel otherRole = other.addRole("shared");
            RoleModel realmRole = session.roles().addRealmRole(realm, "shared");
            RoleModel clientRole = session.roles().addClientRole(client, "shared");

            assertThat(defaultRole, notNullValue());
            assertThat(defaultRole.getType(), is(RoleModel.Type.ORGANIZATION));
            assertThat(defaultRole.getContainerId(), is(acme.getId()));
            assertThat(defaultRole.getContainer().getId(), is(acme.getId()));
            assertThat(defaultRole.getName(), is(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme"));
            assertThrows(ModelDuplicateException.class, () -> acme.addRole("shared"));
            assertThat(session.roles().getOrganizationRole(acme, "shared").getId(), is(acmeRole.getId()));
            assertThat(session.roles().getOrganizationRole(other, "shared").getId(), is(otherRole.getId()));
            assertThat(session.roles().getRoleById(acme, acmeRole.getId()).getId(), is(acmeRole.getId()));
            assertThat(session.roles().getRoleById(acme, otherRole.getId()), nullValue());
            assertThat(session.roles().getRoleById(acme, realmRole.getId()), nullValue());
            assertThat(session.roles().getRoleById(acme, clientRole.getId()), nullValue());
            assertThat(session.roles().getRoleById(realm, acmeRole.getId()).getType(), is(RoleModel.Type.ORGANIZATION));
            assertThat(session.roles().getRealmRole(realm, "shared").getId(), is(realmRole.getId()));
            assertThat(session.roles().getClientRole(client, "shared").getId(), is(clientRole.getId()));
            assertThat(acme.searchForRolesStream("shared", null, null).map(RoleModel::getName).toList(), contains("shared"));
            assertThat(acme.getRolesStream().map(RoleModel::getName).toList(), containsInAnyOrder(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme", "shared"));
            assertThat(session.roles().getOrganizationRolesCount(acme, "shared"), is(1L));
        });
    }

    @Test
    public void shouldConstrainOrganizationRoleMappingsAndComposites() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            UserModel user = session.users().addUser(realm, "organization-role-member");
            RoleModel acmeRole = acme.addRole("member-only");
            RoleModel child = acme.addRole("child");
            RoleModel otherRole = other.addRole("member-only");
            RoleModel realmRole = session.roles().addRealmRole(realm, "realm-composite");
            RoleModel clientRole = session.roles().addClientRole(client, "client-composite");

            assertThrows(ModelException.class, () -> user.grantRole(acmeRole));
            organizations.addMember(acme, user);
            assertThrows(ModelException.class, () -> user.grantRole(otherRole));
            user.grantRole(acmeRole);
            assertThat(user.hasDirectRole(acme.getDefaultRole()), is(true));
            assertThat(user.hasDirectRole(acmeRole), is(true));

            acmeRole.addCompositeRole(child);
            acmeRole.addCompositeRole(realmRole);
            acmeRole.addCompositeRole(clientRole);
            assertThat(acmeRole.getCompositesStream().map(RoleModel::getId).toList(),
                    containsInAnyOrder(child.getId(), realmRole.getId(), clientRole.getId()));
            assertThrows(ModelException.class, () -> acmeRole.addCompositeRole(otherRole));
            assertThrows(ModelException.class, () -> realmRole.addCompositeRole(child));
            assertThrows(ModelException.class, () -> clientRole.addCompositeRole(child));

            organizations.removeMember(acme, user);
            assertThat(user.hasDirectRole(acme.getDefaultRole()), is(false));
            assertThat(user.hasDirectRole(acmeRole), is(false));
        });
    }

    @Test
    public void shouldRejectOrganizationRolesForGroupsAndScopeMappings() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            ClientScopeModel clientScope = session.clientScopes().addClientScope(realm, "organization-role-client-scope");
            GroupModel group = session.groups().createGroup(realm, "organization-role-group");
            RoleModel organizationRole = acme.addRole("organization-only");
            RoleModel realmRole = session.roles().addRealmRole(realm, "realm-group-role");
            RoleModel clientRole = session.roles().addClientRole(client, "client-group-role");

            assertThrows(ModelException.class, () -> group.grantRole(organizationRole));
            assertThrows(ModelException.class, () -> client.addScopeMapping(organizationRole));
            assertThrows(ModelException.class, () -> clientScope.addScopeMapping(organizationRole));
            group.grantRole(realmRole);
            group.grantRole(clientRole);
            client.addScopeMapping(realmRole);
            clientScope.addScopeMapping(realmRole);

            assertThat(group.hasDirectRole(realmRole), is(true));
            assertThat(group.getClientRoleMappingsStream(client).map(RoleModel::getId).toList(), contains(clientRole.getId()));
            assertThat(client.hasDirectScope(realmRole), is(true));
            assertThat(clientScope.hasDirectScope(realmRole), is(true));
        });
    }

    @Test
    public void shouldProtectDefaultRoleAndRemoveRolesWithOrganization() {
        String[] roleIds = runOnServer.fetch(session -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel defaultRole = acme.getDefaultRole();
            RoleModel customRole = acme.addRole("project-admin");

            defaultRole.addCompositeRole(customRole);
            assertThrows(ModelException.class, () -> session.roles().removeRole(defaultRole));
            organizations.remove(acme);

            return new String[] { defaultRole.getId(), customRole.getId() };
        }, String[].class);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();

            assertThat(session.roles().getRoleById(realm, roleIds[0]), nullValue());
            assertThat(session.roles().getRoleById(realm, roleIds[1]), nullValue());
            assertThat(session.roles().searchForRolesStream(realm, "default-roles-acme", null, null).toList(), empty());
        });
    }

    private static OrganizationModel getOrganization(KeycloakSession session, String id) {
        return session.getProvider(OrganizationProvider.class).getById(id);
    }

    public static final class OrganizationRoleRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true)
                    .clients(ClientBuilder.create(CLIENT_ID));
        }
    }
}
