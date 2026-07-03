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
package org.keycloak.services.resources.admin.fgap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.stream.Stream;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CacheRealmProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OrganizationRolePermissionsTest {

    private static final KeycloakSession SESSION = mockSession();
    private static final RealmModel REALM = mockRealm();

    @BeforeClass
    public static void beforeClass() {
        Profile.configure();
    }

    @AfterClass
    public static void afterClass() {
        Profile.reset();
    }

    @Test
    public void classicPermissionsUseOrganizationPermissionsForOrganizationRoles() {
        TestMgmtPermissions root = new TestMgmtPermissions();
        RolePermissions permissions = new RolePermissions(SESSION, REALM, null, root);
        OrganizationModel organization = mockOrganization("org-1");
        RoleModel role = mockOrganizationRole("role-1", organization);

        assertFalse(permissions.canView(role));
        assertFalse(permissions.canManage(role));
        assertFalse(permissions.canMapRole(role));
        assertFalse(permissions.canMapComposite(role));
        assertFalse(permissions.canList(organization));
        assertFalse(permissions.canMapClientScope(role));
        assertThrows(ForbiddenException.class, () -> permissions.requireView(role));
        assertThrows(ForbiddenException.class, () -> permissions.requireManage(role));
        assertThrows(ForbiddenException.class, () -> permissions.requireMapRole(role));
        assertThrows(ForbiddenException.class, () -> permissions.requireMapComposite(role));
        assertThrows(ForbiddenException.class, () -> permissions.requireMapClientScope(role));
        assertThrows(ForbiddenException.class, () -> permissions.requireList(organization));
        assertThrows(ForbiddenException.class, () -> root.organizations.requireQuery());

        root.manageUsers = true;
        assertFalse(permissions.canMapRole(role));
        assertFalse(permissions.canMapComposite(role));

        root.organizations.canView = true;
        assertTrue(permissions.canView(role));
        assertTrue(permissions.canList(organization));
        assertFalse(permissions.canManage(role));

        root.organizations.canView = false;
        root.organizations.canQuery = true;
        assertTrue(permissions.canList(organization));

        root.organizations.canManage = true;
        assertTrue(permissions.canManage(role));
        assertTrue(permissions.canManage(organization));
        assertTrue(permissions.canMapRole(role));
        assertTrue(permissions.canMapComposite(role));
        assertTrue(permissions.canManageDefault(role));
        permissions.requireView(role);
        permissions.requireManage(role);
        permissions.requireMapRole(role);
        permissions.requireMapComposite(role);
        permissions.requireList(organization);
        root.organizations.requireQuery();

        RoleModel adminNamedOrganizationRole = mockOrganizationRole("role-2", AdminRoles.MANAGE_USERS, organization);
        assertTrue(permissions.canMapRole(adminNamedOrganizationRole));
        assertTrue(permissions.canMapComposite(adminNamedOrganizationRole));

        ClientModel application = mockClient("application", REALM);
        assertTrue(permissions.canMapRole(mockClientRole("role-3", "application-role", application)));
        assertTrue(permissions.canMapRole(mockClientRole("role-4", AdminRoles.MANAGE_USERS, application)));
    }

    @Test
    public void v2PermissionsUseOrganizationPermissionsForOrganizationRoles() {
        RealmModel realm = mockRealm(true);
        TestMgmtPermissions root = new TestMgmtPermissions(realm);
        RolePermissionsV2 permissions = new RolePermissionsV2(SESSION, realm, null, root);
        OrganizationModel organization = mockOrganization("org-1", realm);
        RoleModel role = mockOrganizationRole("role-1", organization);

        assertFalse(permissions.canMapRole(role));
        assertFalse(permissions.canMapComposite(role));
        assertFalse(permissions.canMapClientScope(role));
        assertThrows(ForbiddenException.class, () -> permissions.requireMapRole(role));
        assertThrows(ForbiddenException.class, () -> permissions.requireMapComposite(role));
        assertThrows(ForbiddenException.class, () -> permissions.requireMapClientScope(role));

        root.manageUsers = true;
        assertFalse(permissions.canMapRole(role));
        assertFalse(permissions.canMapComposite(role));

        root.organizations.canManage = true;
        assertTrue(permissions.canManage(role));
        assertTrue(permissions.canMapRole(role));
        assertTrue(permissions.canMapComposite(role));
        permissions.requireMapRole(role);
        permissions.requireMapComposite(role);

        RoleModel adminNamedOrganizationRole = mockOrganizationRole("role-2", AdminRoles.MANAGE_USERS, organization);
        assertTrue(permissions.canMapRole(adminNamedOrganizationRole));
        assertTrue(permissions.canMapComposite(adminNamedOrganizationRole));
    }

    @Test
    public void lightweightAdminRoleRefreshOnlyMatchesClientRoleComposites() throws Exception {
        ClientModel client = mockClient("realm-management", REALM);
        RoleModel matchingClientRole = mockClientRole("role-1", AdminRoles.MANAGE_USERS, client);
        RoleModel realmRole = mockRealmRole("role-2", AdminRoles.MANAGE_USERS, REALM);
        RoleModel organizationRole = mockOrganizationRole("role-3", AdminRoles.MANAGE_USERS, mockOrganization("org-1"));
        RoleModel masterAdminRole = mockCompositeRole(AdminRoles.ADMIN, matchingClientRole, realmRole, organizationRole);
        RealmModel masterRealm = proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getName" -> "master";
            case "getRole" -> AdminRoles.ADMIN.equals(args[0]) ? masterAdminRole : null;
            default -> defaultValue(method.getReturnType());
        });
        UserModel admin = proxy(UserModel.class, (userProxy, method, args) -> switch (method.getName()) {
            case "hasRole" -> args[0] == masterAdminRole;
            default -> defaultValue(method.getReturnType());
        });
        CacheRealmProvider cache = proxy(CacheRealmProvider.class, (cacheProxy, method, args) -> switch (method.getName()) {
            case "refreshMasterAdminRole" -> true;
            default -> defaultValue(method.getReturnType());
        });
        TestMgmtPermissions root = new TestMgmtPermissions(mockSession(cache, null), REALM) {
            @Override
            RealmModel getMasterRealm() {
                return masterRealm;
            }

            @Override
            public UserModel admin() {
                return admin;
            }
        };
        Method method = MgmtPermissions.class.getDeclaredMethod("hasNewAdminRoles", RealmModel.class, String.class, String[].class);
        method.setAccessible(true);

        org.junit.Assert.assertTrue((Boolean) method.invoke(root, REALM, client.getId(), new String[] { AdminRoles.MANAGE_USERS }));
        org.junit.Assert.assertFalse((Boolean) method.invoke(root, REALM, client.getId(), new String[] { AdminRoles.MANAGE_CLIENTS }));
    }

    @Test
    public void rolePermissionsResolveResourceServerOnlyForClientRoles() throws Exception {
        ResourceServer resourceServer = proxy(ResourceServer.class, (serverProxy, method, args) -> defaultValue(method.getReturnType()));
        ResourceServerStore resourceServerStore = proxy(ResourceServerStore.class, (storeProxy, method, args) -> switch (method.getName()) {
            case "findById" -> resourceServer;
            default -> defaultValue(method.getReturnType());
        });
        StoreFactory storeFactory = proxy(StoreFactory.class, (factoryProxy, method, args) -> switch (method.getName()) {
            case "getResourceServerStore" -> resourceServerStore;
            default -> defaultValue(method.getReturnType());
        });
        KeycloakSession session = mockSession(null, storeFactory);
        RolePermissions permissions = new RolePermissions(session, REALM, null, new TestMgmtPermissions(session, REALM));
        ClientModel client = mockClient("client-id", REALM);
        Method method = RolePermissions.class.getDeclaredMethod("getResourceServer", RoleModel.class);
        method.setAccessible(true);

        org.junit.Assert.assertSame(resourceServer, method.invoke(permissions, mockClientRole("role-1", "client-role", client)));
        org.junit.Assert.assertNull(method.invoke(permissions, mockOrganizationRole("role-2", mockOrganization("org-1"))));
    }

    private static class TestMgmtPermissions extends MgmtPermissions {
        private final TestOrganizationPermissions organizations = new TestOrganizationPermissions(this);
        private final ClientModel realmManagementClient = mockClient("realm-management", REALM);
        private final UserModel admin = proxy(UserModel.class,
                (adminProxy, method, args) -> defaultValue(method.getReturnType()));
        private boolean manageUsers;

        TestMgmtPermissions() {
            super(SESSION, REALM);
        }

        TestMgmtPermissions(RealmModel realm) {
            super(SESSION, realm);
        }

        TestMgmtPermissions(KeycloakSession session, RealmModel realm) {
            super(session, realm);
        }

        @Override
        public TestOrganizationPermissions orgs() {
            return organizations;
        }

        @Override
        public boolean isAdminSameRealm() {
            return true;
        }

        @Override
        public boolean hasOneAdminRole(String... adminRoles) {
            return manageUsers && Arrays.asList(adminRoles).contains(AdminRoles.MANAGE_USERS);
        }

        @Override
        ClientModel getRealmManagementClient() {
            return realmManagementClient;
        }

        @Override
        public UserModel admin() {
            return admin;
        }
    }

    private static class TestOrganizationPermissions extends OrganizationPermissions {
        private boolean canManage;
        private boolean canView;
        private boolean canQuery;

        TestOrganizationPermissions(MgmtPermissions root) {
            super(SESSION, null, root);
        }

        @Override
        public boolean canManage() {
            return canManage;
        }

        @Override
        public boolean canManage(OrganizationModel organization) {
            return canManage;
        }

        @Override
        public boolean canView() {
            return canView;
        }

        @Override
        public boolean canView(OrganizationModel organization) {
            return canView || canManage;
        }

        @Override
        public boolean canQuery() {
            return canQuery || canView();
        }
    }

    private static KeycloakSession mockSession() {
        return mockSession(null, null);
    }

    private static KeycloakSession mockSession(CacheRealmProvider cache, StoreFactory storeFactory) {
        KeycloakSessionFactory factory = proxy(KeycloakSessionFactory.class, (sessionFactoryProxy, method, args) -> {
            if ("getProviderFactory".equals(method.getName())) {
                return proxy(AuthorizationProviderFactory.class, (authorizationFactoryProxy, authorizationMethod, authorizationArgs) -> {
                    if ("create".equals(authorizationMethod.getName())) {
                        return null;
                    }
                    return defaultValue(authorizationMethod.getReturnType());
                });
            }
            return defaultValue(method.getReturnType());
        });

        return proxy(KeycloakSession.class, (sessionProxy, method, args) -> {
            if ("getKeycloakSessionFactory".equals(method.getName())) {
                return factory;
            }
            if ("getProvider".equals(method.getName())) {
                if (args[0].equals(CacheRealmProvider.class)) {
                    return cache;
                }
                if (args[0].equals(StoreFactory.class)) {
                    return storeFactory;
                }
                if (args[0].equals(AuthorizationProvider.class)) {
                    return new AuthorizationProvider((KeycloakSession) sessionProxy, REALM, null);
                }
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static RealmModel mockRealm() {
        return mockRealm(false);
    }

    private static RealmModel mockRealm(boolean adminPermissionsEnabled) {
        return proxy(RealmModel.class, (realmProxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm-1";
            case "getName" -> "realm";
            case "isAdminPermissionsEnabled" -> adminPermissionsEnabled;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static OrganizationModel mockOrganization(String id) {
        return mockOrganization(id, REALM);
    }

    private static OrganizationModel mockOrganization(String id, RealmModel realm) {
        return proxy(OrganizationModel.class, (organizationProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getRealm" -> realm;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel mockOrganizationRole(String id, OrganizationModel organization) {
        return mockOrganizationRole(id, "organization-role", organization);
    }

    private static RoleModel mockOrganizationRole(String id, String name, OrganizationModel organization) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getName" -> name;
            case "getContainer" -> organization;
            case "getContainerId" -> organization.getId();
            case "getType" -> RoleModel.Type.ORGANIZATION;
            case "isOrganizationRole" -> true;
            case "isRealmRole", "isClientRole" -> false;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel mockRealmRole(String id, String name, RealmModel realm) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getName" -> name;
            case "getContainer" -> realm;
            case "getContainerId" -> realm.getId();
            case "getType" -> RoleModel.Type.REALM;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static ClientModel mockClient(String id, RealmModel realm) {
        return proxy(ClientModel.class, (clientProxy, method, args) -> switch (method.getName()) {
            case "getId", "getClientId" -> id;
            case "getRealm" -> realm;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel mockClientRole(String id, String name, ClientModel client) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getName" -> name;
            case "getContainer" -> client;
            case "getContainerId" -> client.getId();
            case "getType" -> RoleModel.Type.CLIENT;
            case "isClientRole" -> true;
            case "isOrganizationRole", "isRealmRole" -> false;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RoleModel mockCompositeRole(String name, RoleModel... composites) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId", "getName" -> name;
            case "getCompositesStream" -> Stream.of(composites);
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            if (method.getDeclaringClass().equals(Object.class)) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> null;
                };
            }
            return handler.invoke(proxy, method, args);
        });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (char.class.equals(type)) {
            return '\0';
        }
        return 0;
    }
}
