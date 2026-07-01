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

import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RoleModel;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class AbstractUserAdapterFederatedStorageTest {

    @Test
    public void grantRoleRejectsOrganizationRolesForNonMembers() {
        AbstractUserAdapterFederatedStorage user = new AbstractUserAdapterFederatedStorage.Streams(null, null, null) {
            @Override
            public String getUsername() {
                return "user-1";
            }

            @Override
            public void setUsername(String username) {
            }
        };
        OrganizationModel organization = (OrganizationModel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { OrganizationModel.class }, (proxy, method, args) -> false);
        RoleModel role = (RoleModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { RoleModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContainer" -> organization;
                    case "isOrganizationRole" -> true;
                    default -> null;
                });

        assertThrows(ModelException.class, () -> user.grantRole(role));

        AbstractUserAdapterFederatedStorage member = new AbstractUserAdapterFederatedStorage.Streams(null, null, null) {
            @Override
            public String getUsername() {
                return "user-2";
            }

            @Override
            public void setUsername(String username) {
            }

            @Override
            public boolean hasDirectRole(RoleModel role) {
                return true;
            }
        };
        OrganizationModel memberOrganization = (OrganizationModel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { OrganizationModel.class }, (proxy, method, args) -> true);
        RoleModel memberRole = (RoleModel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { RoleModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getContainer" -> memberOrganization;
                    case "isOrganizationRole" -> true;
                    default -> null;
                });

        member.grantRole(memberRole);
    }
}
