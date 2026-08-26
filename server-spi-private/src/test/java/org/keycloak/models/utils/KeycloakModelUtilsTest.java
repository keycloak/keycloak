/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.utils;

import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class KeycloakModelUtilsTest {

    @Test
    public void testGenerateId() {
        final String id = KeycloakModelUtils.generateId();
        Assert.assertEquals(36, id.length());
        final String shortId = KeycloakModelUtils.generateShortId(UUID.fromString(id));
        final UUID uuid = fromShortId(shortId);
        Assert.assertEquals(id, uuid.toString());
    }

    @Test
    public void testGetRequiredClientSecretLength() {
        Assert.assertEquals(
                SecretGenerator.equivalentEntropySize(SecretGenerator.SECRET_LENGTH_512_BITS, SecretGenerator.ALPHANUM.length),
                KeycloakModelUtils.getRequiredClientSecretLength());
    }

    @Test
    public void testGetSecretLengthByAuthenticationTypeAlwaysUsesHs512Entropy() {
        int requiredLength = KeycloakModelUtils.getRequiredClientSecretLength();
        Assert.assertEquals(requiredLength, KeycloakModelUtils.getSecretLengthByAuthenticationType("client-secret", null));
        Assert.assertEquals(requiredLength, KeycloakModelUtils.getSecretLengthByAuthenticationType("client-secret-jwt", Algorithm.HS256));
        Assert.assertEquals(requiredLength, KeycloakModelUtils.getSecretLengthByAuthenticationType("client-secret-jwt", Algorithm.HS512));
    }

    @Test
    public void testGenerateShortId() {
        final String shortId = KeycloakModelUtils.generateShortId();
        final UUID uuid = fromShortId(shortId);
        Assert.assertEquals(shortId, KeycloakModelUtils.generateShortId(uuid));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRemoveTransientAdminRolesClientRoles() {
        // User actually holds manage-users on the realm-management client (admin client)
        RoleModel manageUsers = role("manage-users", "role-manage-users", "realm-management");
        RoleModel impersonation = role("impersonation", "role-impersonation", "realm-management");
        RoleModel someRole = role("some-role", "role-some-role", "realm-management");

        Map<String, RoleModel> clientRoles = new HashMap<>();
        clientRoles.put("manage-users", manageUsers);
        clientRoles.put("impersonation", impersonation);
        clientRoles.put("some-role", someRole);

        ClientModel realmManagement = client("realm-management", clientRoles);
        RealmModel realm = realm("master", realmManagement);

        UserModel user = userProxy(Stream.of(manageUsers));

        // Token claims manage-users (user has it, keep), impersonation (user does NOT have it, strip),
        // some-role (not an admin role, keep)
        AccessToken.Access access = new AccessToken.Access();
        access.roles(new HashSet<>(Set.of("manage-users", "impersonation", "some-role")));

        KeycloakModelUtils.removeTransientAdminRoles(realm, "realm-management", user, access);

        Assert.assertEquals(Set.of("manage-users", "some-role"), access.getRoles());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRemoveTransientAdminRolesRealmLevel() {
        // Realm-level (clientId == null) admin role in master realm
        RoleModel admin = role("admin", "role-admin", null);
        RoleModel createRealm = role("create-realm", "role-create-realm", null);
        RoleModel viewRealm = role("view-realm", "role-view-realm", null);

        Map<String, RoleModel> realmRoles = new HashMap<>();
        realmRoles.put("admin", admin);
        realmRoles.put("create-realm", createRealm);
        realmRoles.put("view-realm", viewRealm);

        RealmModel realm = realm("master", null, realmRoles);

        UserModel user = userProxy(Stream.of(admin));

        AccessToken.Access access = new AccessToken.Access();
        access.roles(new HashSet<>(Set.of("admin", "create-realm", "view-realm")));

        KeycloakModelUtils.removeTransientAdminRoles(realm, null, user, access);

        // admin is held by the user -> kept; create-realm and view-realm are not -> stripped
        Assert.assertEquals(Set.of("admin"), access.getRoles());
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == Stream.class) return Stream.empty();
        if (returnType == Set.class) return new HashSet<>();
        return null;
    }

    private UserModel userProxy(Stream<? extends RoleModel> roles) {
        return (UserModel) Proxy.newProxyInstance(UserModel.class.getClassLoader(),
                new Class<?>[]{UserModel.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getRoleMappingsStream":
                            return roles;
                        case "getGroupsStream":
                            return Stream.empty();
                        default:
                            return defaultValue(method.getReturnType());
                    }
                });
    }

    private RoleModel role(String name, String id, String clientId) {
        final Object containerRef = clientId != null
                ? client(clientId, new HashMap<>())
                : realm("master", null);
        return (RoleModel) Proxy.newProxyInstance(RoleModel.class.getClassLoader(),
                new Class<?>[]{RoleModel.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getName":
                            return name;
                        case "getId":
                            return id;
                        case "isComposite":
                            return false;
                        case "getCompositesStream":
                            return Stream.<RoleModel>empty();
                        case "getContainer":
                            return containerRef;
                        default:
                            return defaultValue(method.getReturnType());
                    }
                });
    }

    private ClientModel client(String clientId, Map<String, RoleModel> roles) {
        return (ClientModel) Proxy.newProxyInstance(ClientModel.class.getClassLoader(),
                new Class<?>[]{ClientModel.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getClientId":
                            return clientId;
                        case "getRole":
                            return roles.get(args[0]);
                        case "getRealm":
                            return realm("master", null);
                        default:
                            return defaultValue(method.getReturnType());
                    }
                });
    }

    private RealmModel realm(String name, ClientModel realmManagement) {
        return realm(name, realmManagement, new HashMap<>());
    }

    private RealmModel realm(String name, ClientModel realmManagement, Map<String, RoleModel> realmRoles) {
        return (RealmModel) Proxy.newProxyInstance(RealmModel.class.getClassLoader(),
                new Class<?>[]{RealmModel.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getName":
                            return name;
                        case "getClientByClientId":
                            return realmManagement;
                        case "getRole":
                            return realmRoles.get(args[0]);
                        default:
                            return defaultValue(method.getReturnType());
                    }
                });
    }

    private UUID fromShortId(String shortId) {
        Assert.assertEquals(22, shortId.length());
        final byte[] bytes = Base64.getUrlDecoder().decode(shortId);
        Assert.assertEquals(Long.BYTES * 2, bytes.length);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        final long msb = bb.getLong();
        final long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }
}