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
package org.keycloak.storage.adapter;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class AbstractUserAdapterFederatedStorageTest {

    @Test
    public void grantRoleRejectsOrganizationRolesForNonMembers() {
        RecordingFederatedStorage nonMemberFederatedStorage = new RecordingFederatedStorage();
        AbstractUserAdapterFederatedStorage user = new AbstractUserAdapterFederatedStorage.Streams(null, null, null) {
            @Override
            public String getUsername() {
                return "user-1";
            }

            @Override
            public void setUsername(String username) {
            }

            @Override
            public UserFederatedStorageProvider getFederatedStorage() {
                return nonMemberFederatedStorage.provider();
            }
        };
        OrganizationModel organization = organization(false);
        RoleModel role = organizationRole(organization);

        assertThrows(ModelException.class, () -> user.grantRole(role));
        assertEquals(0, nonMemberFederatedStorage.grants);

        RealmModel realm = (RealmModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> null);
        RecordingFederatedStorage memberFederatedStorage = new RecordingFederatedStorage();
        AbstractUserAdapterFederatedStorage member = new AbstractUserAdapterFederatedStorage.Streams(null, realm, null) {
            @Override
            public String getUsername() {
                return "user-2";
            }

            @Override
            public void setUsername(String username) {
            }

            @Override
            public String getId() {
                return "user-2";
            }

            @Override
            public boolean hasDirectRole(RoleModel role) {
                return false;
            }

            @Override
            public UserFederatedStorageProvider getFederatedStorage() {
                return memberFederatedStorage.provider();
            }
        };
        OrganizationModel memberOrganization = organization(true);
        RoleModel memberRole = organizationRole(memberOrganization);

        member.grantRole(memberRole);

        assertEquals(1, memberFederatedStorage.grants);
        assertSame(realm, memberFederatedStorage.realm);
        assertEquals("user-2", memberFederatedStorage.userId);
        assertSame(memberRole, memberFederatedStorage.role);
    }

    @Test
    public void abstractUserAdapterFiltersClientRoleMappingsByTypeAndContainer() {
        ClientModel client = client("client-id");
        RoleModel clientRole = role("client-role", RoleModel.Type.CLIENT, "client-id");
        RoleModel otherClientRole = role("other-client-role", RoleModel.Type.CLIENT, "other-client-id");
        RoleModel realmRole = role("realm-role", RoleModel.Type.REALM, "realm-id");

        AbstractUserAdapter user = new AbstractUserAdapter(null, null, null) {
            @Override
            public String getUsername() {
                return "user-3";
            }

            @Override
            public void setUsername(String username) {
            }

            @Override
            public Set<RoleModel> getRoleMappings() {
                return Set.of(clientRole, otherClientRole, realmRole);
            }

            @Override
            public SubjectCredentialManager credentialManager() {
                return null;
            }
        };

        assertEquals(Set.of(clientRole), user.getClientRoleMappings(client));
    }

    @Test
    public void abstractUserAdapterStreamsFilterClientRoleMappingsByTypeAndContainer() {
        ClientModel client = client("client-id");
        RoleModel clientRole = role("client-role", RoleModel.Type.CLIENT, "client-id");
        RoleModel organizationRole = role("organization-role", RoleModel.Type.ORGANIZATION, "client-id");

        AbstractUserAdapter.Streams user = new AbstractUserAdapter.Streams(null, null, null) {
            @Override
            public String getUsername() {
                return "user-4";
            }

            @Override
            public void setUsername(String username) {
            }

            @Override
            public Stream<RoleModel> getRoleMappingsStream() {
                return Stream.of(clientRole, organizationRole);
            }

            @Override
            public SubjectCredentialManager credentialManager() {
                return null;
            }
        };

        assertEquals(Set.of(clientRole), user.getClientRoleMappingsStream(client).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    public void federatedStorageFiltersClientRoleMappingsByTypeAndContainer() {
        ClientModel client = client("client-id");
        RoleModel clientRole = role("client-role", RoleModel.Type.CLIENT, "client-id");
        RoleModel realmRole = role("realm-role", RoleModel.Type.REALM, "client-id");

        AbstractUserAdapterFederatedStorage user = new AbstractUserAdapterFederatedStorage.Streams(null, null, null) {
            @Override
            public String getUsername() {
                return "user-5";
            }

            @Override
            public void setUsername(String username) {
            }

            @Override
            public Stream<RoleModel> getRoleMappingsStream() {
                return Stream.of(clientRole, realmRole);
            }
        };

        assertEquals(Set.of(clientRole), user.getClientRoleMappingsStream(client).collect(java.util.stream.Collectors.toSet()));
    }

    private ClientModel client(String id) {
        return (ClientModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ClientModel.class },
                (proxy, method, args) -> "getId".equals(method.getName()) ? id : null);
    }

    private OrganizationModel organization(boolean member) {
        return (OrganizationModel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { OrganizationModel.class }, (proxy, method, args) -> "isMember".equals(method.getName()) ? member : null);
    }

    private RoleModel organizationRole(OrganizationModel organization) {
        return (RoleModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { RoleModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContainer" -> organization;
                    case "isOrganizationRole" -> true;
                    default -> null;
                });
    }

    private RoleModel role(String name, RoleModel.Type type, String containerId) {
        return (RoleModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { RoleModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getType" -> type;
                    case "getContainerId" -> containerId;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> name;
                    default -> null;
                });
    }

    private class RecordingFederatedStorage {
        private int grants;
        private RealmModel realm;
        private String userId;
        private RoleModel role;

        private UserFederatedStorageProvider provider() {
            return (UserFederatedStorageProvider) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { UserFederatedStorageProvider.class }, (proxy, method, args) -> {
                        if ("grantRole".equals(method.getName())) {
                            grants++;
                            realm = (RealmModel) args[0];
                            userId = (String) args[1];
                            role = (RoleModel) args[2];
                        }
                        return null;
                    });
        }
    }
}
