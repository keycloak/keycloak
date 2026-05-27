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
package org.keycloak.services.clientpolicy.context;

import java.lang.reflect.Proxy;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.ScopeContainerModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resources.admin.AdminAuth;

class ContextTestStubs {

    static AdminAuth stubAdminAuth() {
        return new AdminAuth(null, new AccessToken(), stub(UserModel.class), stub(ClientModel.class));
    }

    static ProtocolMapperContainerModel stubContainer() {
        return stub(ProtocolMapperContainerModel.class);
    }

    static ScopeContainerModel stubScopeContainer() {
        return stub(ScopeContainerModel.class);
    }

    static RoleMapperModel stubRoleMapper() {
        return stub(RoleMapperModel.class);
    }

    static ClientModel stubClient() {
        return stub(ClientModel.class);
    }

    @SuppressWarnings("unchecked")
    static <T> T stub(Class<T> iface) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[] { iface },
                (proxy, method, args) -> {
                    Class<?> ret = method.getReturnType();
                    if (ret == boolean.class) return false;
                    if (ret == int.class || ret == long.class || ret == short.class || ret == byte.class) return 0;
                    if (ret == char.class) return '\0';
                    if (ret == float.class || ret == double.class) return 0.0;
                    return null;
                });
    }
}
