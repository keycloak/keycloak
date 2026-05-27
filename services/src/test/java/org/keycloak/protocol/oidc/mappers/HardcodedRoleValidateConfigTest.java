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
package org.keycloak.protocol.oidc.mappers;

import java.lang.reflect.Proxy;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.representations.AccessToken;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class HardcodedRoleValidateConfigTest {

    private static final String TARGET_REALM_USER_ID = "target-realm-user";

    private final HardcodedRole mapper = new HardcodedRole();

    @Test
    public void nullConfigAllowsConfiguration() throws ProtocolMapperConfigException {
        mapper.validateConfig(stubSession(null, null), stubRealm(null, null, null, null), null, mapperModelWithNullConfig());
    }

    @Test
    public void nullTokenAllowsConfiguration() throws ProtocolMapperConfigException {
        mapper.validateConfig(stubSession(null, null), stubRealm(null, null, null, null), null,
                mapperModel("realm-management.realm-admin"));
    }

    @Test
    public void userHoldingRoleInTargetRealmAllowsConfiguration() throws ProtocolMapperConfigException {
        RoleModel role = stubRole();
        RealmModel realm = stubRealm("realm-management", stubClient("realm-admin", role), null, null);
        mapper.validateConfig(
                stubSession(stubUsers(realm, TARGET_REALM_USER_ID, stubUser(role, true)), token(TARGET_REALM_USER_ID)),
                realm, null, mapperModel("realm-management.realm-admin"));
    }

    @Test
    public void userNotHoldingRoleBlocksConfiguration() {
        String configuredRole = "realm-management.realm-admin\nforged";
        RoleModel role = stubRole();
        RealmModel realm = stubRealm("realm-management", stubClient("realm-admin\nforged", role), null, null);

        ProtocolMapperConfigException ex = assertThrows(ProtocolMapperConfigException.class, () ->
                mapper.validateConfig(
                        stubSession(stubUsers(realm, TARGET_REALM_USER_ID, stubUser(role, false)), token(TARGET_REALM_USER_ID)),
                        realm, null, mapperModel(configuredRole)));

        assertFalse(ex.getMessage().contains(configuredRole));
    }

    @Test
    public void tokenWithoutSubjectBlocksConfiguration() {
        ProtocolMapperConfigException ex = assertThrows(ProtocolMapperConfigException.class, () ->
                mapper.validateConfig(stubSession(null, new AccessToken()), stubRealm(null, null, null, null), null,
                        mapperModel("realm-management.realm-admin")));

        assertFalse(ex.getMessage().contains("realm-management.realm-admin"));
    }

    @Test
    public void unresolvedTargetRealmUserBlocksConfiguration() {
        RoleModel role = stubRole();
        RealmModel realm = stubRealm("realm-management", stubClient("realm-admin", role), null, null);

        assertThrows(ProtocolMapperConfigException.class, () ->
                mapper.validateConfig(
                        stubSession(stubUsers(realm, TARGET_REALM_USER_ID, null), token(TARGET_REALM_USER_ID)),
                        realm, null, mapperModel("realm-management.realm-admin")));
    }

    @Test
    public void unknownRoleClientAllowsConfiguration() throws ProtocolMapperConfigException {
        RealmModel realm = stubRealm("nonexistent-client", null, null, null);
        mapper.validateConfig(
                stubSession(stubUsers(realm, TARGET_REALM_USER_ID, stubUser(null, false)), token(TARGET_REALM_USER_ID)),
                realm, null, mapperModel("nonexistent-client.some-role"));
    }

    @Test
    public void unknownRealmRoleAllowsConfiguration() throws ProtocolMapperConfigException {
        RealmModel realm = stubRealm(null, null, "nonexistent-role", null);
        mapper.validateConfig(
                stubSession(stubUsers(realm, TARGET_REALM_USER_ID, stubUser(null, false)), token(TARGET_REALM_USER_ID)),
                realm, null, mapperModel("nonexistent-role"));
    }

    @Test
    public void emptyRoleConfigAllowsConfiguration() throws ProtocolMapperConfigException {
        mapper.validateConfig(stubSession(null, null), stubRealm(null, null, null, null), null, mapperModel(""));
    }

    private ProtocolMapperModel mapperModel(String role) {
        return HardcodedRole.create("test", role);
    }

    private ProtocolMapperModel mapperModelWithNullConfig() {
        ProtocolMapperModel mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(null);
        return mapperModel;
    }

    private AccessToken token(String subject) {
        AccessToken token = new AccessToken();
        token.setSubject(subject);
        return token;
    }

    private KeycloakSession stubSession(UserProvider users, AccessToken token) {
        KeycloakContext context = (KeycloakContext) Proxy.newProxyInstance(
                KeycloakContext.class.getClassLoader(), new Class[] { KeycloakContext.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getBearerToken")) return token;
                    throw new AssertionError("unexpected context method: " + method.getName());
                });
        return (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(), new Class[] { KeycloakSession.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getContext")) return context;
                    if (method.getName().equals("users")) return users;
                    throw new AssertionError("unexpected session method: " + method.getName());
                });
    }

    private UserProvider stubUsers(RealmModel realm, String targetUserId, UserModel user) {
        return (UserProvider) Proxy.newProxyInstance(
                UserProvider.class.getClassLoader(), new Class[] { UserProvider.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getUserById") && args[0] == realm && targetUserId.equals(args[1])) {
                        return user;
                    }
                    throw new AssertionError("unexpected users method: " + method.getName());
                });
    }

    private RealmModel stubRealm(String targetClientId, ClientModel client, String targetRoleName, RoleModel role) {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(), new Class[] { RealmModel.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getClientByClientId") && targetClientId != null
                            && targetClientId.equals(args[0])) {
                        return client;
                    }
                    if (method.getName().equals("getRole") && targetRoleName != null
                            && targetRoleName.equals(args[0])) {
                        return role;
                    }
                    throw new AssertionError("unexpected realm method: " + method.getName());
                });
    }

    private ClientModel stubClient(String targetRoleName, RoleModel role) {
        return (ClientModel) Proxy.newProxyInstance(
                ClientModel.class.getClassLoader(), new Class[] { ClientModel.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getRole") && targetRoleName != null
                            && targetRoleName.equals(args[0])) {
                        return role;
                    }
                    throw new AssertionError("unexpected client method: " + method.getName());
                });
    }

    private RoleModel stubRole() {
        return (RoleModel) Proxy.newProxyInstance(
                RoleModel.class.getClassLoader(), new Class[] { RoleModel.class },
                (proxy, method, args) -> {
                    if (method.getReturnType() == boolean.class) return false;
                    throw new AssertionError("unexpected role method: " + method.getName());
                });
    }

    private UserModel stubUser(RoleModel targetRole, boolean hasRole) {
        return (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(), new Class[] { UserModel.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("hasRole") && args[0] == targetRole) return hasRole;
                    if (method.getReturnType() == boolean.class) return false;
                    throw new AssertionError("unexpected user method: " + method.getName());
                });
    }
}
