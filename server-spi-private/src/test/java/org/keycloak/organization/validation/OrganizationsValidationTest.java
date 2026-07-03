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
package org.keycloak.organization.validation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.storage.adapter.AbstractInMemoryUserAdapter;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class OrganizationsValidationTest {

    @Test
    public void validateOrganizationRoleMapping() {
        UserModel member = mockUser("member");
        OrganizationModel organization = mockOrganization("org-1", member);
        RoleModel organizationRole = mockRole("role-1", organization, true);

        OrganizationsValidation.validateOrganizationRoleMapping(member, organizationRole);
        OrganizationsValidation.validateOrganizationRoleMapping(member, mockRole("realm-role", mockRealm(), false));
        OrganizationsValidation.validateOrganizationRoleMapping(member, null);

        assertThrows(ModelException.class, () -> OrganizationsValidation.validateOrganizationRoleMapping(null, organizationRole));
        assertThrows(ModelException.class, () -> OrganizationsValidation.validateOrganizationRoleMapping(mockUser("non-member"), organizationRole));
        assertThrows(ModelException.class, () -> OrganizationsValidation.validateOrganizationRoleMapping(member,
                mockRole("invalid-role", mockRealm(), true)));
    }

    @Test
    public void validateOrganizationRoleMappingsForGroupsAndScopes() {
        RoleModel organizationRole = mockRole("role-1", mockOrganization("org-1", null), true);

        OrganizationsValidation.validateOrganizationRoleGroupMapping(null);
        OrganizationsValidation.validateOrganizationRoleScopeMapping(null);
        assertThrows(ModelException.class, () -> OrganizationsValidation.validateOrganizationRoleGroupMapping(organizationRole));
        assertThrows(ModelException.class, () -> OrganizationsValidation.validateOrganizationRoleScopeMapping(organizationRole));
    }

    @Test
    public void validateOrganizationRoleComposites() {
        OrganizationModel organization = mockOrganization("org-1", null);
        RoleModel parent = mockRole("parent", organization, true);
        RoleModel child = mockRole("child", organization, true);
        RoleModel realmRole = mockRole("realm-role", mockRealm(), false);
        RoleModel clientRole = mockRole("client-role", mockClient(), false);
        RoleModel otherRole = mockRole("other-role", mockOrganization("org-2", null), true);

        OrganizationsValidation.validateOrganizationRoleComposite(parent, child);
        OrganizationsValidation.validateOrganizationRoleComposite(parent, realmRole);
        OrganizationsValidation.validateOrganizationRoleComposite(parent, clientRole);
        OrganizationsValidation.validateOrganizationRoleComposite(realmRole, null);
        OrganizationsValidation.validateOrganizationRoleComposite(realmRole, realmRole);
        OrganizationsValidation.validateOrganizationRoleComposite(clientRole, realmRole);

        assertThrows(ModelException.class, () -> OrganizationsValidation.validateOrganizationRoleComposite(realmRole, child));
        assertThrows(ModelException.class, () -> OrganizationsValidation.validateOrganizationRoleComposite(clientRole, child));
        assertThrows(ModelException.class, () -> OrganizationsValidation.validateOrganizationRoleComposite(parent, otherRole));
    }

    @Test
    public void inMemoryUsersRejectOrganizationRolesForNonMembers() {
        AbstractInMemoryUserAdapter user = new AbstractInMemoryUserAdapter(null, null, "user-1") {
            @Override
            public SubjectCredentialManager credentialManager() {
                return null;
            }
        };
        RoleModel role = mockRole("role-1", mockOrganization("org-1", null), true);

        assertThrows(ModelException.class, () -> user.grantRole(role));

        RoleModel memberRole = mockRole("role-2", mockOrganization("org-1", user), true);
        user.grantRole(memberRole);
    }

    private static UserModel mockUser(String id) {
        return proxy(UserModel.class, (userProxy, method, args) -> "getId".equals(method.getName()) ? id : defaultValue(method.getReturnType()));
    }

    private static OrganizationModel mockOrganization(String id, UserModel member) {
        return proxy(OrganizationModel.class, (organizationProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "isMember" -> args[0] == member;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static RealmModel mockRealm() {
        return proxy(RealmModel.class, (realmProxy, method, args) -> "getId".equals(method.getName()) ? "realm-1" : defaultValue(method.getReturnType()));
    }

    private static ClientModel mockClient() {
        return proxy(ClientModel.class,
                (clientProxy, method, args) -> "getId".equals(method.getName()) ? "client-1" : defaultValue(method.getReturnType()));
    }

    private static RoleModel mockRole(String id, Object container, boolean organizationRole) {
        return proxy(RoleModel.class, (roleProxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getContainer" -> container;
            case "isOrganizationRole" -> organizationRole;
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (boolean.class.equals(type)) return false;
        if (char.class.equals(type)) return '\0';
        return 0;
    }
}
