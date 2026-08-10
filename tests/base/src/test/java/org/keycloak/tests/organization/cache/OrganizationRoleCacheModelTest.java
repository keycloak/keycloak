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

package org.keycloak.tests.organization.cache;

import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@KeycloakIntegrationTest
public class OrganizationRoleCacheModelTest {

    private static final String CLIENT_ID = "cache-client-with-roles";
    private static final String ACME_ID = "cache-org-acme";
    private static final String OTHER_ID = "cache-org-other";

    @InjectRealm(config = OrganizationRoleCacheRealmConfig.class, lifecycle = LifeCycle.METHOD)
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
            organizations.create(ACME_ID, "Cache Acme", "cache-acme");
            organizations.create(OTHER_ID, "Cache Other", "cache-other");
        });
    }

    @Test
    public void shouldInvalidateOrganizationRoleLifecycleAcrossCachedLookups() {
        String[] ids = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            RoleModel organizationRole = acme.addRole("project-admin");
            RoleModel childRole = session.roles().addOrganizationRole(acme, "direct-cache-role");
            RoleModel otherRole = other.addRole("project-admin");
            RoleModel realmRole = session.roles().addRealmRole(realm, "cache-realm-role");
            RoleModel clientRole = session.roles().addClientRole(client, "cache-client-role");
            UserModel user = session.users().addUser(realm, "cache-organization-role-member");
            session.getProvider(OrganizationProvider.class).addMember(acme, user);

            assertThat(acme.getRole("project-admin").getId(), is(organizationRole.getId()));

            return new String[] { organizationRole.getId(), childRole.getId(), otherRole.getId(), realmRole.getId(),
                    clientRole.getId(), user.getId() };
        }, String[].class);

        // Warm every organization-role cache path before mutating the backing models.
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);
            RoleModel organizationRole = session.roles().getRoleById(acme, ids[0]);
            RoleModel realmRole = session.roles().getRoleById(realm, ids[3]);
            RoleModel clientRole = session.roles().getRoleById(realm, ids[4]);

            assertThat(organizationRole, notNullValue());
            assertThat(session.roles().getRoleById(acme, ids[0]).getId(), is(ids[0]));
            assertThat(session.roles().getRoleById(other, ids[0]), nullValue());
            assertThat(session.roles().getRoleById(acme, ids[3]), nullValue());
            assertThat(session.roles().getRoleById(acme, ids[4]), nullValue());
            assertThat(session.roles().getRoleById(realm, ids[0]).getType(), is(RoleModel.Type.ORGANIZATION));
            assertThat(realmRole.getType(), is(RoleModel.Type.REALM));
            assertThat(clientRole.getType(), is(RoleModel.Type.CLIENT));
            assertThat(organizationRole.isOrganizationRole(), is(true));
            assertThat(organizationRole.getContainerId(), is(acme.getId()));
            assertThat(organizationRole.getContainer().getId(), is(acme.getId()));
            assertThat(acme.getDefaultRole(), notNullValue());
            assertThat(acme.getRolesStream().map(RoleModel::getName).toList(),
                    containsInAnyOrder(Constants.DEFAULT_ORGANIZATION_ROLES_ROLE_PREFIX + "-cache-acme", "project-admin", "direct-cache-role"));
            assertThat(acme.searchForRolesStream("project", null, null).map(RoleModel::getName).toList(), hasItem("project-admin"));
            assertThat(session.roles().getOrganizationRolesCount(acme, "project"), is(1L));
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel role = session.roles().getRoleById(acme, ids[0]);
            RoleModel childRole = session.roles().getRoleById(acme, ids[1]);
            RoleModel realmRole = session.roles().getRoleById(realm, ids[3]);
            RoleModel clientRole = session.roles().getRoleById(realm, ids[4]);
            UserModel user = session.users().getUserById(realm, ids[5]);

            role.setName("renamed-project-admin");
            role.addCompositeRole(childRole);
            role.addCompositeRole(realmRole);
            role.addCompositeRole(clientRole);
            user.grantRole(role);
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel role = session.roles().getRoleById(acme, ids[0]);
            UserModel user = session.users().getUserById(realm, ids[5]);

            assertThat(role.getName(), is("renamed-project-admin"));
            assertThat(acme.getRole("project-admin"), nullValue());
            assertThat(acme.getRole("renamed-project-admin").getId(), is(ids[0]));
            assertThat(role.getCompositesStream().map(RoleModel::getId).toList(),
                    containsInAnyOrder(ids[1], ids[3], ids[4]));
            assertThat(user.hasDirectRole(role), is(true));
            assertThat(acme.getRolesStream().map(RoleModel::getName).toList(), not(hasItem("project-admin")));
            assertThat(acme.getRolesStream().map(RoleModel::getName).toList(), hasItem("renamed-project-admin"));
        });

        String recreatedRoleId = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel role = session.roles().getRoleById(acme, ids[0]);
            UserModel user = session.users().getUserById(realm, ids[5]);

            user.deleteRoleMapping(role);
            role.getCompositesStream().toList().forEach(role::removeCompositeRole);
            session.roles().removeRole(role);
            assertThat(session.roles().getRoleById(acme, ids[0]), nullValue());
            assertThat(acme.getRole("renamed-project-admin"), nullValue());

            return acme.addRole("renamed-project-admin").getId();
        }, String.class);

        runOnServer.run(session -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);

            assertNotEquals(ids[0], recreatedRoleId);
            assertThat(session.roles().getRoleById(acme, ids[0]), nullValue());
            assertThat(acme.getRole("renamed-project-admin").getId(), is(recreatedRoleId));
            assertThat(acme.searchForRolesStream("renamed", null, null).map(RoleModel::getId).toList(),
                    containsInAnyOrder(recreatedRoleId));
        });
    }

    private static OrganizationModel getOrganization(KeycloakSession session, String id) {
        return session.getProvider(OrganizationProvider.class).getById(id);
    }

    public static final class OrganizationRoleCacheRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true)
                    .clients(ClientBuilder.create(CLIENT_ID));
        }
    }
}
