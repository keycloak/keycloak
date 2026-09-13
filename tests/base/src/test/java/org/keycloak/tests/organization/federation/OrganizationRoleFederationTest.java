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

package org.keycloak.tests.organization.federation;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.tests.providers.federation.UserMapStorage;
import org.keycloak.tests.providers.federation.UserMapStorageFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class OrganizationRoleFederationTest extends AbstractOrganizationTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void shouldKeepProviderManagedGroupMembershipByDefault() {
        realm.dirty();
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel provider = new ComponentModel();
            provider.setId("provider-managed-groups");
            provider.getConfig().putSingle(UserStorageProviderModel.IMPORT_ENABLED, "false");
            UserMapStorage storage = new UserMapStorage(session, provider, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
            session.enlistForClose(storage);
            UserModel member = storage.addUser(realm, "provider-group-member");
            GroupModel group = realm.createGroup("provider-group");
            member.joinGroup(group);
            assertEquals(List.of(group.getId()), storage.getGroupsStream(realm, member.getUsername())
                    .map(GroupModel::getId).toList());
            assertEquals(List.of(member.getUsername()), storage.getMembershipStream(realm, group, 0, 10).toList());
            member.leaveGroup(group);
            assertEquals(List.of(), storage.getGroupsStream(realm, member.getUsername()).toList());
            assertEquals(List.of(), storage.getMembershipStream(realm, group, 0, 10).toList());
        });
    }

    @Test
    public void shouldSearchAndPageLocalAndFederatedRoleMembersTogether() {
        realm.dirty();
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel localMember = session.users().addUser(realm, "local-role-member");
            ComponentModel provider = new ComponentModel();
            provider.setName("organization-role-members");
            provider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
            provider.setProviderType(UserStorageProvider.class.getName());
            provider.getConfig().putSingle(UserStorageProviderModel.IMPORT_ENABLED, "false");
            provider.getConfig().putSingle("federatedStorage", "true");
            realm.addComponentModel(provider);
            UserModel federatedMember = session.users().addUser(realm, "thor");
            assertFalse(StorageId.isLocalStorage(federatedMember.getId()));

            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.create("federated-role-members", "Federated members", "federated-role-members");
            assertTrue(organizations.addMember(organization, localMember));
            assertTrue(organizations.addMember(organization, federatedMember));
            RoleModel role = organization.addRole("federated-member");
            localMember.grantRole(role);
            federatedMember.grantRole(role);
        });
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.getByAlias("federated-role-members");
            RoleModel role = organization.getRole("federated-member");
            UserModel federatedMember = session.users().getUserByUsername(realm, "thor");
            assertEquals(List.of(organization.getId()), organizations.getByMember(federatedMember)
                    .map(OrganizationModel::getId).toList());
            assertEquals(List.of(federatedMember.getId()), organizations.getRoleMembersStream(organization, role, "tho", 0, 1)
                    .map(UserModel::getId).toList());
            assertEquals(List.of(federatedMember.getId()), organizations.getRoleMembersStream(organization, role, null, 1, 1)
                    .map(UserModel::getId).toList());
            assertTrue(organizations.removeMember(organization, federatedMember));
            assertFalse(organization.isMember(federatedMember));
            assertFalse(federatedMember.hasRole(role));
            assertEquals(List.of("local-role-member"), organizations.getRoleMembersStream(organization, role, null, 0, null)
                    .map(UserModel::getUsername).toList());
        });
    }
}
