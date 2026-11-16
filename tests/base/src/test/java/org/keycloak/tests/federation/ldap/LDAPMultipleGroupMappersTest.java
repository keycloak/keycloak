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

package org.keycloak.tests.federation.ldap;

import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.LDAPGroupMapperMode;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class LDAPMultipleGroupMappersTest {

    private static LDAPEmbeddedServer ldap;
    private String ldapId;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient(config = ClientConfiguration.class, lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests.federation.ldap", "org.keycloak.testsuite.util"}, lifecycle = LifeCycle.METHOD)
    RunOnServerClient runOnServer;

    @BeforeAll
    public static void startLdap() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        try (ServerSocket socket = new ServerSocket(0)) {
            properties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_PORT, Integer.toString(socket.getLocalPort()));
        }
        ldap = new LDAPEmbeddedServer(properties);
        ldap.init();
        ldap.start();
    }

    @AfterAll
    public static void stopLdap() throws Exception {
        if (ldap != null) {
            ldap.stop();
        }
    }

    @BeforeEach
    public void configureLdap() {
        String ldapUrl = UriBuilder.newInstance().scheme("ldap").host(ldap.getBindHost()).port(ldap.getBindPort()).build().toString();
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("test-ldap");
        provider.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>(Map.of(
                LDAPConstants.CONNECTION_URL, List.of(ldapUrl),
                LDAPConstants.USERS_DN, List.of("ou=People,dc=keycloak,dc=org"),
                LDAPConstants.BIND_DN, List.of("uid=admin,ou=system"),
                LDAPConstants.BIND_CREDENTIAL, List.of("secret"),
                LDAPConstants.EDIT_MODE, List.of(UserStorageProvider.EditMode.WRITABLE.name()),
                LDAPConstants.PAGINATION, List.of("false"),
                UserStorageProviderModel.CACHE_POLICY, List.of(UserStorageProviderModel.CachePolicy.NO_CACHE.name()))));
        ldapId = ApiUtil.getCreatedId(realm.admin().components().add(provider));
        runOnServer.run(ServerCode.seedUser());
        oauth.scope("openid");
    }

    @ParameterizedTest
    @EnumSource(LDAPGroupMapperMode.class)
    public void multipleRootGroupMappers(LDAPGroupMapperMode mode) throws Exception {
        addMapper("mapper-a", "RealmRoles", "group-a", mode, "/");
        addMapper("mapper-b", "FinanceRoles", "group-b", mode, "/");
        runOnServer.run(ServerCode.addLocalMembership());

        List<String> expected = mode == LDAPGroupMapperMode.LDAP_ONLY
                ? List.of("/group-a", "/group-b")
                : List.of("/group-a", "/group-b", "/local-group");
        assertMemberships(expected);
    }

    @Test
    public void singleRootLdapOnlyMapperIgnoresLocalMemberships() throws Exception {
        addMapper("mapper-a", "RealmRoles", "group-a", LDAPGroupMapperMode.LDAP_ONLY, "/");
        runOnServer.run(ServerCode.addLocalMembership());

        assertMemberships(List.of("/group-a"));
    }

    @Test
    public void overlappingRootGroupMappersDoNotDuplicateMemberships() throws Exception {
        addMapper("mapper-a", "RealmRoles", "shared-group", LDAPGroupMapperMode.LDAP_ONLY, "/");
        addMapper("mapper-b", "FinanceRoles", "shared-group", LDAPGroupMapperMode.LDAP_ONLY, "/");

        assertMemberships(List.of("/shared-group"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void rootAndNestedGroupMappers(boolean rootFirst) throws Exception {
        ApiUtil.getCreatedId(realm.admin().groups().add(GroupBuilder.create("nested").build()));
        addMapper("mapper-a", "RealmRoles", "group-a", LDAPGroupMapperMode.LDAP_ONLY, rootFirst ? "/" : "/nested");
        addMapper("mapper-b", "FinanceRoles", "group-b", LDAPGroupMapperMode.LDAP_ONLY, rootFirst ? "/nested" : "/");
        runOnServer.run(ServerCode.addLocalMembership());

        assertMemberships(rootFirst ? List.of("/group-a", "/nested/group-b") : List.of("/group-b", "/nested/group-a"));
    }

    @Test
    public void nestedGroupMappersKeepLocalMemberships() throws Exception {
        ApiUtil.getCreatedId(realm.admin().groups().add(GroupBuilder.create("nested").build()));
        addMapper("mapper-a", "RealmRoles", "group-a", LDAPGroupMapperMode.LDAP_ONLY, "/nested");
        addMapper("mapper-b", "FinanceRoles", "group-b", LDAPGroupMapperMode.LDAP_ONLY, "/nested");
        runOnServer.run(ServerCode.addLocalMembership());

        assertMemberships(List.of("/local-group", "/nested/group-a", "/nested/group-b"));
    }

    private void addMapper(String name, String ou, String groupName, LDAPGroupMapperMode mode, String groupsPath) {
        ComponentRepresentation mapper = new ComponentRepresentation();
        // Stable IDs exercise both orders when a root mapper is combined with a nested mapper.
        mapper.setId(realm.getId() + "-" + name);
        mapper.setName(name);
        mapper.setProviderId(GroupLDAPStorageMapperFactory.PROVIDER_ID);
        mapper.setProviderType(LDAPStorageMapper.class.getName());
        mapper.setParentId(ldapId);
        mapper.setConfig(new MultivaluedHashMap<>(Map.of(
                GroupMapperConfig.GROUPS_DN, List.of("ou=" + ou + ",dc=keycloak,dc=org"),
                GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, List.of("false"),
                GroupMapperConfig.MODE, List.of(mode.name()),
                GroupMapperConfig.LDAP_GROUPS_PATH, List.of(groupsPath))));
        String mapperId = ApiUtil.getCreatedId(realm.admin().components().add(mapper));
        runOnServer.run(ServerCode.seedGroups(name, groupName));
        assertEquals(0, realm.admin().userStorage().syncMapperData(ldapId, mapperId, "fedToKeycloak").getFailed());
    }

    private void assertMemberships(List<String> expected) throws Exception {
        String userId = realm.admin().users().searchByUsername("johnkeycloak", true).get(0).getId();
        assertEquals(expected.stream().sorted().toList(), realm.admin().users().get(userId).groups().stream()
                .map(GroupRepresentation::getPath).sorted().toList());
        runOnServer.run(ServerCode.assertGroupCount(expected.size()));

        AccessTokenResponse response = oauth.doPasswordGrantRequest("johnkeycloak", "Password1");
        assertEquals(200, response.getStatusCode(), response.getErrorDescription());
        assertEquals(expected.stream().sorted().toList(), ((List<?>) oauth.verifyToken(response.getAccessToken())
                .getOtherClaims().get("groups")).stream().map(Object::toString).sorted().toList());
        assertEquals(expected.stream().sorted().toList(), ((List<?>) oauth.verifyIDToken(response.getIdToken())
                .getOtherClaims().get("groups")).stream().map(Object::toString).sorted().toList());
    }

    public static class ClientConfiguration implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            ProtocolMapperModel mapper = GroupMembershipMapper.create("groups", "groups", false, null, true, true, false);
            mapper.getConfig().put("full.path", "true");
            return client.clientId("test-app").secret("password").directAccessGrantsEnabled(true)
                    .protocolMappers(ModelToRepresentation.toRepresentation(mapper));
        }
    }

    public static class ServerCode {

        public static RunOnServer seedUser() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, LDAPTestUtils.getLdapProviderModel(realm));
                LDAPTestUtils.removeAllLDAPUsers(provider, realm);
                LDAPObject user = LDAPTestUtils.addLDAPUser(provider, realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
                LDAPTestUtils.updateLDAPPassword(provider, user, "Password1");
            };
        }

        public static RunOnServer seedGroups(String mapperName, String groupName) {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                ComponentModel model = LDAPTestUtils.getLdapProviderModel(realm);
                LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, model);
                LDAPTestUtils.removeAllLDAPGroups(session, realm, model, mapperName);
                LDAPObject group = LDAPTestUtils.createLDAPGroup(mapperName, session, realm, model, groupName);
                LDAPObject user = provider.loadLDAPUserByUsername(realm, "johnkeycloak");
                LDAPUtils.addMember(provider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group, user);
            };
        }

        public static RunOnServer addLocalMembership() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserByUsername(realm, "johnkeycloak");
                GroupModel group = realm.createGroup("local-group");
                UserStoragePrivateUtil.userLocalStorage(session).getUserById(realm, user.getId()).joinGroup(group);
            };
        }

        public static RunOnServer assertGroupCount(int expected) {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                assertEquals(expected, session.users().getUserByUsername(realm, "johnkeycloak").getGroupsCount());
            };
        }
    }
}
