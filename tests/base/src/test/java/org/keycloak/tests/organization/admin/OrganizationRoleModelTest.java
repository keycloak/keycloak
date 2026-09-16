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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.BadRequestException;

import org.keycloak.connections.jpa.JpaConnectionProvider;
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
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.GroupRoleMappingEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.protocol.mappers.OrganizationRoleMapperUtils;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.jpa.entity.FederatedUserRoleMappingEntity;
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

    private static final class OrganizationRoleEventCollector implements ProviderEventListener, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final OrganizationRoleEventCollector INSTANCE = new OrganizationRoleEventCollector();

        private final List<String> events = new CopyOnWriteArrayList<>();

        @Override
        public void onEvent(ProviderEvent event) {
            if (event instanceof GroupModel.GroupMemberJoinEvent) {
                events.add("GROUP_JOIN");
            } else if (event instanceof GroupModel.GroupMemberLeaveEvent) {
                events.add("GROUP_LEAVE");
            } else if (event instanceof OrganizationModel.OrganizationMemberJoinEvent) {
                events.add("ORGANIZATION_JOIN");
            } else if (event instanceof OrganizationModel.OrganizationMemberLeaveEvent) {
                events.add("ORGANIZATION_LEAVE");
            } else if (event instanceof RoleModel.RoleGrantedEvent) {
                events.add("ROLE_GRANTED");
            } else if (event instanceof RoleModel.RoleRevokedEvent) {
                events.add("ROLE_REVOKED");
            } else if (event instanceof GroupModel.GroupUpdatedEvent) {
                events.add("GROUP_UPDATED");
            }
        }

        private void clear() {
            events.clear();
        }

        private long count(String type) {
            return events.stream().filter(type::equals).count();
        }
    }

    private static final class FailingGroupUpdateListener implements ProviderEventListener, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final FailingGroupUpdateListener INSTANCE = new FailingGroupUpdateListener();

        private volatile boolean armed;

        @Override
        public void onEvent(ProviderEvent event) {
            if (armed && event instanceof GroupModel.GroupUpdatedEvent) {
                armed = false;
                throw new IllegalStateException("injected group mapping event failure");
            }
        }

        private void arm() {
            armed = true;
        }

        private void disarm() {
            armed = false;
        }
    }

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
            assertThat(defaultRole.getName(), is(Constants.DEFAULT_ORGANIZATION_ROLES_ROLE_PREFIX + "-acme"));
            GroupModel root = organizations(session).getOrganizationGroup(acme);
            assertThat(root.getRoleMappingsStream().map(RoleModel::getId).toList(), contains(defaultRole.getId()));
            assertThrows(ModelDuplicateException.class, () -> acme.addRole("shared"));
            assertThat(session.roles().getRole(acme, "shared").getId(), is(acmeRole.getId()));
            assertThat(session.roles().getRole(other, "shared").getId(), is(otherRole.getId()));
            assertThat(session.roles().getRoleInContainerById(acme, acmeRole.getId()).getId(), is(acmeRole.getId()));
            assertThat(session.roles().getRoleInContainerById(acme, otherRole.getId()), nullValue());
            assertThat(session.roles().getRoleInContainerById(acme, realmRole.getId()), nullValue());
            assertThat(session.roles().getRoleInContainerById(acme, clientRole.getId()), nullValue());
            assertThat(session.roles().getRoleById(realm, acmeRole.getId()).getType(), is(RoleModel.Type.ORGANIZATION));
            assertThat(session.roles().getRealmRole(realm, "shared").getId(), is(realmRole.getId()));
            assertThat(session.roles().getClientRole(client, "shared").getId(), is(clientRole.getId()));
            assertThat(acme.searchForRolesStream("shared", null, null).map(RoleModel::getName).toList(), contains("shared"));
            assertThat(acme.searchForRolesStream(null, null, null).map(RoleModel::getName).toList(),
                    containsInAnyOrder(Constants.DEFAULT_ORGANIZATION_ROLES_ROLE_PREFIX + "-acme", "shared"));
            assertThat(acme.getRolesStream().map(RoleModel::getName).toList(), containsInAnyOrder(Constants.DEFAULT_ORGANIZATION_ROLES_ROLE_PREFIX + "-acme", "shared"));
            assertThat(session.roles().getRolesCount(acme, "shared"), is(1L));
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
            assertThrows(ModelException.class, () -> user.grantRole(acme.getDefaultRole()));
            user.grantRole(acmeRole);
            user.grantRole(clientRole);
            assertThat(user.hasDirectRole(acme.getDefaultRole()), is(false));
            assertThat(user.hasRole(acme.getDefaultRole()), is(true));
            assertThat(user.hasDirectRole(acmeRole), is(true));
            assertThat(user.getClientRoleMappingsStream(client).map(RoleModel::getId).toList(),
                    contains(clientRole.getId()));

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
            assertThat(user.hasRole(acme.getDefaultRole()), is(false));
            assertThat(user.hasDirectRole(acmeRole), is(false));
        });
    }

    @Test
    public void shouldReplaceAndRepairDefaultRoleOnTheInternalGroup() {
        String[] state = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = organizations(session);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            GroupModel root = organizations.getOrganizationGroup(acme);
            UserModel user = session.users().addUser(realm, "default-role-switch-member");
            RoleModel original = acme.getDefaultRole();
            RoleModel replacement = acme.addRole("replacement-default");

            organizations.addMember(acme, user);
            acme.setDefaultRole(original);
            assertThat(root.getRoleMappingsStream().map(RoleModel::getId).toList(), contains(original.getId()));
            return new String[] { original.getId(), replacement.getId(), user.getId() };
        }, String[].class);

        runOnServer.run(session -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            GroupModel root = organizations(session).getOrganizationGroup(acme);
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            em.createQuery("delete from GroupRoleMappingEntity mapping where mapping.group.id = :groupId and mapping.roleId = :roleId")
                    .setParameter("groupId", root.getId())
                    .setParameter("roleId", state[0])
                    .executeUpdate();
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            GroupModel root = organizations(session).getOrganizationGroup(acme);
            RoleModel original = realm.getRoleById(state[0]);
            RoleModel replacement = realm.getRoleById(state[1]);
            UserModel user = session.users().getUserById(realm, state[2]);

            acme.setDefaultRole(original);
            assertThat(root.getRoleMappingsStream().map(RoleModel::getId).toList(), contains(original.getId()));

            acme.setDefaultRole(replacement);
            assertThat(acme.getDefaultRole().getId(), is(replacement.getId()));
            assertThat(root.getRoleMappingsStream().map(RoleModel::getId).toList(), contains(replacement.getId()));
            assertThat(user.hasDirectRole(replacement), is(false));
            assertThat(user.hasRole(replacement), is(true));
            assertThat(user.hasRole(original), is(false));
            assertThrows(ModelException.class, () -> acme.setDefaultRole(null));
        });
    }

    @Test
    public void shouldRejectCorruptDefaultMappingDuringClaimResolutionUntilRepaired() {
        String[] state = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            UserModel user = session.users().addUser(realm, "corrupt-default-claim-member");
            organizations(session).addMember(acme, user);
            return new String[] { organizations(session).getOrganizationGroup(acme).getId(),
                    acme.getDefaultRole().getId(), user.getId() };
        }, String[].class);

        runOnServer.run(session -> session.getProvider(JpaConnectionProvider.class).getEntityManager()
                .createQuery("delete from GroupRoleMappingEntity mapping where mapping.group.id = :groupId and mapping.roleId = :roleId")
                .setParameter("groupId", state[0])
                .setParameter("roleId", state[1])
                .executeUpdate());

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            UserModel user = session.users().getUserById(realm, state[2]);

            assertThrows(ModelException.class, () -> OrganizationRoleMapperUtils.resolveRoleClaims(acme, user, session));
            acme.setDefaultRole(acme.getDefaultRole());
            assertThat(OrganizationRoleMapperUtils.resolveRoleClaims(acme, user, session).getOrganizationRoles(),
                    contains(acme.getDefaultRole().getName()));
        });
    }

    @Test
    public void shouldRejectPromotionOfDirectlyAssignedRoleAtomically() {
        String[] state = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = organizations(session);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            UserModel user = session.users().addUser(realm, "direct-candidate-member");
            RoleModel candidate = acme.addRole("direct-candidate");

            organizations.addMember(acme, user);
            user.grantRole(candidate);
            return new String[] { acme.getDefaultRole().getId(), candidate.getId(), user.getId() };
        }, String[].class);

        runOnServer.run(session -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel candidate = session.getContext().getRealm().getRoleById(state[1]);
            assertThrows(ModelException.class, () -> acme.setDefaultRole(candidate));
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            GroupModel root = organizations(session).getOrganizationGroup(acme);
            RoleModel original = realm.getRoleById(state[0]);
            RoleModel candidate = realm.getRoleById(state[1]);
            UserModel user = session.users().getUserById(realm, state[2]);

            assertThat(acme.getDefaultRole().getId(), is(original.getId()));
            assertThat(root.getRoleMappingsStream().map(RoleModel::getId).toList(), contains(original.getId()));
            assertThat(user.hasDirectRole(candidate), is(true));
        });
    }

    @Test
    public void shouldRejectPromotionOfRoleMappedToVisibleGroupOrUsedAsCompositeChild() {
        runOnServer.run(session -> {
            OrganizationProvider organizations = organizations(session);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            GroupModel root = organizations.getOrganizationGroup(acme);
            RoleModel original = acme.getDefaultRole();
            RoleModel groupCandidate = acme.addRole("visible-group-default-candidate");
            RoleModel compositeCandidate = acme.addRole("composite-default-candidate");
            RoleModel parent = acme.addRole("candidate-parent");
            GroupModel group = organizations.createGroup(acme, "candidate-group", null);
            group.grantRole(groupCandidate);
            parent.addCompositeRole(compositeCandidate);

            assertThrows(ModelException.class, () -> acme.setDefaultRole(groupCandidate));
            assertThrows(ModelException.class, () -> acme.setDefaultRole(compositeCandidate));

            assertThat(acme.getDefaultRole().getId(), is(original.getId()));
            assertThat(root.getRoleMappingsStream().map(RoleModel::getId).toList(), contains(original.getId()));
            assertThat(group.hasDirectRole(groupCandidate), is(true));
            assertThat(parent.hasRole(compositeCandidate), is(true));
        });
    }

    @Test
    public void shouldRejectPromotionOfRoleAssignedThroughFederatedStorage() {
        String[] state = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel candidate = acme.addRole("federated-direct-candidate");
            FederatedUserRoleMappingEntity mapping = new FederatedUserRoleMappingEntity();
            mapping.setUserId("f:test:federated-direct-candidate");
            mapping.setRoleId(candidate.getId());
            mapping.setRealmId(realm.getId());
            mapping.setStorageProviderId("test");
            session.getProvider(JpaConnectionProvider.class).getEntityManager().persist(mapping);
            return new String[] { acme.getDefaultRole().getId(), candidate.getId() };
        }, String[].class);

        runOnServer.run(session -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel candidate = session.getContext().getRealm().getRoleById(state[1]);

            assertThrows(ModelException.class, () -> acme.setDefaultRole(candidate));
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            GroupModel root = organizations(session).getOrganizationGroup(acme);

            assertThat(acme.getDefaultRole().getId(), is(state[0]));
            assertThat(root.getRoleMappingsStream().map(RoleModel::getId).toList(), contains(state[0]));
        });
    }

    @Test
    public void shouldPublishMembershipAndSwitchEventsWithoutPerMemberRoleEvents() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = organizations(session);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel replacement = acme.addRole("event-default-replacement");
            UserModel first = session.users().addUser(realm, "event-member-first");
            UserModel second = session.users().addUser(realm, "event-member-second");
            RoleModel eventRole = session.roles().addRealmRole(realm, "event-control-role");
            OrganizationRoleEventCollector collector = OrganizationRoleEventCollector.INSTANCE;

            session.getKeycloakSessionFactory().unregister(collector);
            collector.clear();
            session.getKeycloakSessionFactory().register(collector);
            try {
                first.grantRole(eventRole);
                first.deleteRoleMapping(eventRole);
                assertThat(collector.count("ROLE_GRANTED"), is(1L));
                assertThat(collector.count("ROLE_REVOKED"), is(1L));
                collector.clear();

                organizations.addMember(acme, first);
                assertThat(collector.count("GROUP_JOIN"), is(1L));
                assertThat(collector.count("ORGANIZATION_JOIN"), is(1L));
                assertThat(collector.count("ROLE_GRANTED"), is(0L));

                collector.clear();
                organizations.addMember(acme, second);
                acme.setDefaultRole(replacement);
                assertThat(collector.count("GROUP_JOIN"), is(1L));
                assertThat(collector.count("ORGANIZATION_JOIN"), is(1L));
                assertThat(collector.count("GROUP_UPDATED"), is(2L));
                assertThat(collector.count("ROLE_GRANTED"), is(0L));
                assertThat(collector.count("ROLE_REVOKED"), is(0L));

                collector.clear();
                organizations.removeMember(acme, first);
                assertThat(collector.count("GROUP_LEAVE"), is(1L));
                assertThat(collector.count("ORGANIZATION_LEAVE"), is(1L));
                assertThat(collector.count("ROLE_REVOKED"), is(0L));
            } finally {
                session.getKeycloakSessionFactory().unregister(collector);
                collector.clear();
            }
        });
    }

    @Test
    public void shouldRollbackDefaultSwitchWhenTheNewMappingEventFails() {
        String[] state = runOnServer.fetch(session -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            return new String[] { acme.getDefaultRole().getId(), acme.addRole("event-failure-candidate").getId() };
        }, String[].class);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            FailingGroupUpdateListener listener = FailingGroupUpdateListener.INSTANCE;
            OrganizationModel previousContext = session.getContext().getOrganization();

            session.getKeycloakSessionFactory().unregister(listener);
            session.getKeycloakSessionFactory().register(listener);
            listener.arm();
            try {
                UserModel control = session.users().addUser(realm, "event-failure-control");
                control.grantRole(session.roles().addRealmRole(realm, "event-failure-control-role"));
                RoleModel replacement = realm.getRoleById(state[1]);
                assertThrows(RuntimeException.class, () -> acme.setDefaultRole(replacement));
                assertThat(session.getContext().getOrganization(), is(previousContext));
            } finally {
                listener.disarm();
                session.getKeycloakSessionFactory().unregister(listener);
            }
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            GroupModel root = organizations(session).getOrganizationGroup(acme);

            assertThat(acme.getDefaultRole().getId(), is(state[0]));
            assertThat(root.getRoleMappingsStream().map(RoleModel::getId).toList(), contains(state[0]));
            assertThat(root.hasDirectRole(realm.getRoleById(state[1])), is(false));
        });
    }

    @Test
    public void shouldProtectInternalGroupMutationsAndOrganizationMembership() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = organizations(session);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);
            GroupModel root = organizations.getOrganizationGroup(acme);
            GroupModel visible = organizations.createGroup(acme, "visible", null);
            UserModel user = session.users().addUser(realm, "guarded-membership");
            RoleModel realmRole = session.roles().addRealmRole(realm, "forbidden-root-role");

            assertThrows(ModelException.class, () -> user.joinGroup(root));
            assertThrows(ModelException.class, () -> user.joinGroup(visible));
            assertThrows(ModelException.class, () -> root.grantRole(realmRole));
            assertThrows(ModelException.class, () -> root.grantRole(other.getDefaultRole()));
            assertThrows(BadRequestException.class, () -> root.deleteRoleMapping(acme.getDefaultRole()));

            organizations.addMember(acme, user);
            user.joinGroup(visible);
            assertThat(user.isMemberOf(visible), is(true));
            assertThrows(ModelException.class, () -> user.leaveGroup(root));
            assertThat(organizations.removeMember(acme, user), is(true));
            assertThat(user.isMemberOf(root), is(false));
            assertThat(user.isMemberOf(visible), is(false));
        });
    }

    @Test
    public void shouldRejectDefaultRoleFromAnotherContainer() {
        runOnServer.run(session -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationModel other = getOrganization(session, OTHER_ID);

            assertThrows(ModelException.class, () -> acme.setDefaultRole(other.getDefaultRole()));
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
            group.deleteRoleMapping(organizationRole);
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
    public void shouldRejectCorruptStoredDefaultRoleStates() {
        runOnServer.run(session -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel candidate = acme.addRole("null-reference-candidate");
            OrganizationEntity entity = organizationEntity(session);
            entity.setDefaultRoleId(null);
            entityManager(session).flush();

            assertThrows(ModelException.class, () -> acme.setDefaultRole(candidate));
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel candidate = acme.addRole("invalid-current-candidate");
            RoleModel realmRole = session.roles().addRealmRole(realm, "invalid-current-realm-role");
            EntityManager em = entityManager(session);
            OrganizationEntity entity = organizationEntity(session);
            GroupEntity root = organizationRootEntity(em, entity);
            clearRootMappings(em, root);
            addRootMapping(em, root, realmRole.getId());
            entity.setDefaultRoleId(realmRole.getId());
            em.flush();

            assertThrows(ModelException.class, () -> acme.setDefaultRole(candidate));
        });

        runOnServer.run(session -> {
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel candidate = acme.addRole("missing-mapping-candidate");
            EntityManager em = entityManager(session);
            OrganizationEntity entity = organizationEntity(session);
            clearRootMappings(em, organizationRootEntity(em, entity));

            assertThrows(ModelException.class, () -> acme.setDefaultRole(candidate));
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel candidate = acme.addRole("extra-mapping-candidate");
            RoleModel realmRole = session.roles().addRealmRole(realm, "extra-mapping-realm-role");
            EntityManager em = entityManager(session);
            OrganizationEntity entity = organizationEntity(session);
            addRootMapping(em, organizationRootEntity(em, entity), realmRole.getId());

            assertThrows(ModelException.class, () -> acme.setDefaultRole(candidate));
        });
    }

    @Test
    public void shouldRejectCorruptStoredDefaultRoleRemovalStates() {
        runOnServer.run(session -> {
            OrganizationProvider organizations = organizations(session);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            OrganizationEntity entity = organizationEntity(session);
            entity.setDefaultRoleId(null);
            entityManager(session).flush();

            assertThrows(ModelException.class, () -> organizations.remove(acme));
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = organizations(session);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel realmRole = session.roles().addRealmRole(realm, "removal-extra-mapping-role");
            EntityManager em = entityManager(session);
            OrganizationEntity entity = organizationEntity(session);
            addRootMapping(em, organizationRootEntity(em, entity), realmRole.getId());

            assertThrows(ModelException.class, () -> organizations.remove(acme));
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = organizations(session);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel realmRole = session.roles().addRealmRole(realm, "removal-invalid-default-role");
            EntityManager em = entityManager(session);
            OrganizationEntity entity = organizationEntity(session);
            GroupEntity root = organizationRootEntity(em, entity);
            clearRootMappings(em, root);
            addRootMapping(em, root, realmRole.getId());
            entity.setDefaultRoleId(realmRole.getId());
            em.flush();

            assertThrows(ModelException.class, () -> organizations.remove(acme));
        });

        runOnServer.run(session -> {
            OrganizationProvider organizations = organizations(session);
            OrganizationModel acme = getOrganization(session, ACME_ID);
            EntityManager em = entityManager(session);
            OrganizationEntity entity = organizationEntity(session);
            clearRootMappings(em, organizationRootEntity(em, entity));
            entity.setDefaultRoleId(null);
            em.flush();

            assertThat(organizations.remove(acme), is(true));
        });
    }

    @Test
    public void shouldRejectPersistedInvalidClaimRootAndFederatedDefaultWrites() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            UserFederatedStorageProvider storage = UserStorageUtil.userFederatedStorage(session);

            assertThrows(ModelException.class,
                    () -> storage.grantRole(realm, "f:integration:default", acme.getDefaultRole()));

            UserModel user = session.users().addUser(realm, "invalid-claim-root-member");
            organizations(session).addMember(acme, user);
            GroupEntity root = organizationRootEntity(entityManager(session), organizationEntity(session));
            root.setType(GroupModel.Type.REALM.intValue());
            entityManager(session).flush();
        });
        realm.admin().clearRealmCache();

        try {
            runOnServer.run(session -> {
                OrganizationModel acme = getOrganization(session, ACME_ID);
                UserModel user = session.users().getUserByUsername(session.getContext().getRealm(),
                        "invalid-claim-root-member");
                assertThrows(ModelException.class,
                        () -> OrganizationRoleMapperUtils.resolveRoleClaims(acme, user, session));
            });
        } finally {
            runOnServer.run(session -> {
                EntityManager em = entityManager(session);
                OrganizationEntity entity = organizationEntity(session);
                organizationRootEntity(em, entity).setType(GroupModel.Type.ORGANIZATION.intValue());
                em.flush();
            });
            realm.admin().clearRealmCache();
        }
    }

    @Test
    public void shouldProtectDefaultRoleAndRemoveRolesWithOrganization() {
        String[] state = runOnServer.fetch(session -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel acme = getOrganization(session, ACME_ID);
            RoleModel defaultRole = acme.getDefaultRole();
            RoleModel customRole = acme.addRole("project-admin");
            RoleModel independentRole = session.roles().addRealmRole(realm, "teardown-independent-role");
            GroupModel root = organizations.getOrganizationGroup(acme);
            GroupModel visible = organizations.createGroup(acme, "teardown-visible", null);
            UserModel unmanaged = session.users().addUser(realm, "teardown-unmanaged");
            UserModel managed = session.users().addUser(realm, "teardown-managed");
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            UserModel serviceAccount = session.users().getServiceAccount(client);

            defaultRole.addCompositeRole(customRole);
            organizations.addMember(acme, unmanaged);
            organizations.addManagedMember(acme, managed);
            organizations.addMember(acme, serviceAccount);
            unmanaged.grantRole(customRole);
            unmanaged.grantRole(independentRole);
            serviceAccount.grantRole(customRole);
            unmanaged.joinGroup(visible);
            managed.joinGroup(visible);
            serviceAccount.joinGroup(visible);
            assertThrows(ModelException.class, () -> session.roles().removeRole(defaultRole));
            assertThrows(ModelException.class, () -> session.roles().removeRoles(acme));
            organizations.remove(acme);

            return new String[] { defaultRole.getId(), customRole.getId(), independentRole.getId(), root.getId(),
                    visible.getId(), unmanaged.getId(), managed.getId(), serviceAccount.getId() };
        }, String[].class);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel unmanaged = session.users().getUserById(realm, state[5]);
            UserModel serviceAccount = session.users().getUserById(realm, state[7]);
            RoleModel independentRole = realm.getRoleById(state[2]);

            assertThat(getOrganization(session, ACME_ID), nullValue());
            assertThat(session.roles().getRoleById(realm, state[0]), nullValue());
            assertThat(session.roles().getRoleById(realm, state[1]), nullValue());
            assertThat(session.roles().searchForRolesStream(realm, "default-roles-org-acme", null, null).toList(), empty());
            assertThat(realm.getGroupById(state[3]), nullValue());
            assertThat(realm.getGroupById(state[4]), nullValue());
            assertThat(unmanaged, notNullValue());
            assertThat(session.users().getUserById(realm, state[6]), nullValue());
            assertThat(serviceAccount, notNullValue());
            assertThat(unmanaged.hasDirectRole(independentRole), is(true));
            assertThat(unmanaged.getRoleMappingsGroupsStream().toList(), empty());
            assertThat(serviceAccount.getRoleMappingsGroupsStream().toList(), empty());
        });
    }

    private static OrganizationModel getOrganization(KeycloakSession session, String id) {
        return organizations(session).getById(id);
    }

    private static OrganizationProvider organizations(KeycloakSession session) {
        return session.getProvider(OrganizationProvider.class);
    }

    private static EntityManager entityManager(KeycloakSession session) {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private static OrganizationEntity organizationEntity(KeycloakSession session) {
        return entityManager(session).find(OrganizationEntity.class, ACME_ID);
    }

    private static GroupEntity organizationRootEntity(EntityManager em, OrganizationEntity organization) {
        return em.find(GroupEntity.class, organization.getGroupId());
    }

    private static void clearRootMappings(EntityManager em, GroupEntity root) {
        em.createNamedQuery("groupRoleMappings", GroupRoleMappingEntity.class).setParameter("group", root)
                .getResultList().forEach(em::remove);
        em.flush();
    }

    private static void addRootMapping(EntityManager em, GroupEntity root, String roleId) {
        GroupRoleMappingEntity mapping = new GroupRoleMappingEntity();
        mapping.setGroup(root);
        mapping.setRoleId(roleId);
        em.persist(mapping);
        em.flush();
    }

    public static final class OrganizationRoleRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true)
                    .clients(ClientBuilder.create(CLIENT_ID));
        }
    }
}
