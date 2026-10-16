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

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPGroupStorageMapper;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPGroupStorageMapperFactory;
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
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class LDAPGroupMapperFgapTest {

    private static LDAPEmbeddedServer ldap;
    private String ldapId;

    @InjectRealm(config = RealmConfiguration.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectOAuthClient(config = ClientConfiguration.class, lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests.federation.ldap", "org.keycloak.testsuite.util"}, lifecycle = LifeCycle.METHOD)
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

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
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("test-ldap");
        provider.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>(Map.of(
                LDAPConstants.CONNECTION_URL, List.of("ldap://localhost:" + ldap.getBindPort()),
                LDAPConstants.USERS_DN, List.of("ou=People,dc=keycloak,dc=org"),
                LDAPConstants.BIND_DN, List.of("uid=admin,ou=system"),
                LDAPConstants.BIND_CREDENTIAL, List.of("secret"),
                LDAPConstants.EDIT_MODE, List.of(UserStorageProvider.EditMode.WRITABLE.name()),
                // Each login must resolve LDAP memberships instead of reusing a cached user.
                UserStorageProviderModel.CACHE_POLICY, List.of(UserStorageProviderModel.CachePolicy.NO_CACHE.name()))));
        ldapId = ApiUtil.getCreatedId(managedRealm.admin().components().add(provider));
        runOnServer.run(ServerCode.seedLdapUser());
        oauth.scope("openid");
    }

    @Test
    public void testFlatGroupsUnderCustomPath() throws Exception {
        assertUserOperations(configureGroups(false, LDAPGroupMapperMode.READ_ONLY, "/Applications/App1"), false);
    }

    @Test
    public void testInheritedGroupsAndAutomaticSync() throws Exception {
        assertUserOperations(configureGroups(true, LDAPGroupMapperMode.LDAP_ONLY, ""), true);
    }

    @Test
    public void testHardcodedGroup() throws Exception {
        String parentId = createGroupHierarchy("hardcoded-parent", "hardcoded-child");
        ComponentRepresentation mapper = new ComponentRepresentation();
        mapper.setName("hardcodedGroupsMapper");
        mapper.setProviderId(HardcodedLDAPGroupStorageMapperFactory.PROVIDER_ID);
        mapper.setProviderType(LDAPStorageMapper.class.getName());
        mapper.setParentId(ldapId);
        mapper.setConfig(new MultivaluedHashMap<>(Map.of(
                HardcodedLDAPGroupStorageMapper.GROUP, List.of("/hardcoded-parent/hardcoded-child"))));
        ApiUtil.getCreatedId(managedRealm.admin().components().add(mapper));
        grantRoles(parentId);
        assertUserOperations("/hardcoded-parent/hardcoded-child", false);
    }

    private String configureGroups(boolean preserveInheritance, LDAPGroupMapperMode mode, String groupsPath) {
        String groupPath = groupsPath + (preserveInheritance ? "/group1/group11" : "/group11");
        if (!groupsPath.isEmpty()) {
            createGroupHierarchy("Applications", "App1");
        }
        ComponentRepresentation mapper = new ComponentRepresentation();
        mapper.setName("groupsMapper");
        mapper.setProviderId(GroupLDAPStorageMapperFactory.PROVIDER_ID);
        mapper.setProviderType(LDAPStorageMapper.class.getName());
        mapper.setParentId(ldapId);
        mapper.setConfig(new MultivaluedHashMap<>(Map.of(
                GroupMapperConfig.GROUPS_DN, List.of("ou=RealmRoles,dc=keycloak,dc=org"),
                GroupMapperConfig.PRESERVE_GROUP_INHERITANCE, List.of(Boolean.toString(preserveInheritance)),
                GroupMapperConfig.MODE, List.of(mode.name()),
                GroupMapperConfig.LDAP_GROUPS_PATH, List.of(groupsPath))));
        String mapperId = ApiUtil.getCreatedId(managedRealm.admin().components().add(mapper));
        runOnServer.run(ServerCode.seedLdapGroups());
        assertEquals(0, managedRealm.admin().userStorage().syncMapperData(ldapId, mapperId, "fedToKeycloak").getFailed());
        grantRoles(managedRealm.admin().getGroupByPath(groupPath).getId());
        return groupPath;
    }

    private String createGroupHierarchy(String parentName, String childName) {
        String parentId = ApiUtil.getCreatedId(managedRealm.admin().groups().add(GroupBuilder.create(parentName).build()));
        ApiUtil.getCreatedId(managedRealm.admin().groups().group(parentId).subGroup(GroupBuilder.create(childName).build()));
        return parentId;
    }

    private void grantRoles(String groupId) {
        managedRealm.admin().groups().group(groupId).roles().realmLevel()
                .add(List.of(managedRealm.admin().roles().get("ldap-group-role").toRepresentation()));
        String managementId = managedRealm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        String userId = managedRealm.admin().users().searchByUsername("johnkeycloak", true).get(0).getId();
        managedRealm.admin().users().get(userId).roles().clientLevel(managementId)
                .add(List.of(managedRealm.admin().clients().get(managementId).roles().get(AdminRoles.QUERY_CLIENTS).toRepresentation()));
    }

    private void assertToken(AccessTokenResponse response, String groupPath) throws Exception {
        assertEquals(200, response.getStatusCode(), response.getErrorDescription());
        AccessToken token = oauth.verifyToken(response.getAccessToken());
        assertTrue(token.getRealmAccess().getRoles().contains("ldap-group-role"));
        assertTrue(((List<?>) token.getOtherClaims().get("ldap-groups")).contains(groupPath));
        assertTrue(((List<?>) oauth.verifyIDToken(response.getIdToken()).getOtherClaims().get("ldap-groups")).contains(groupPath));
    }

    private void assertUserOperations(String groupPath, boolean addNewGroup) throws Exception {
        driver.cookies().deleteAll();
        // Clear group lookups populated by setup so the first login reaches the database.
        managedRealm.admin().clearRealmCache();
        AccessTokenResponse response = oauth.doPasswordGrantRequest("johnkeycloak", "Password1");
        assertToken(response, groupPath);
        // Repeat without clearing the groups cached by the first login.
        assertToken(oauth.doPasswordGrantRequest("johnkeycloak", "Password1"), groupPath);
        assertToken(oauth.doRefreshTokenRequest(response.getRefreshToken()), groupPath);

        managedRealm.admin().clearRealmCache();
        oauth.openLoginForm();
        oauth.fillLoginForm("johnkeycloak", "Password1");
        assertToken(oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode()), groupPath);

        if (addNewGroup) {
            runOnServer.run(ServerCode.addGroup("new-group"));
            // Discovering a new LDAP membership must sync it without losing sight of existing groups.
            managedRealm.admin().clearRealmCache();
            assertToken(oauth.doPasswordGrantRequest("johnkeycloak", "Password1"), groupPath);
            assertNotNull(managedRealm.admin().getGroupByPath("/new-group"));
        }

        // Group visibility must still be enforced after login and after clearing cached group lookups.
        try (Keycloak delegatedAdmin = KeycloakBuilder.builder().serverUrl(oauth.getBaseUrl())
                .realm(managedRealm.getName()).username("johnkeycloak").password("Password1")
                .clientId("test-app").clientSecret("password").build()) {
            assertThrows(ForbiddenException.class, () -> delegatedAdmin.realm(managedRealm.getName()).getGroupByPath(groupPath));
            managedRealm.admin().clearRealmCache();
            assertThrows(ForbiddenException.class, () -> delegatedAdmin.realm(managedRealm.getName()).getGroupByPath(groupPath));
        }
    }

    public static class RealmConfiguration implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.adminPermissionsEnabled(true).roles("ldap-group-role");
        }
    }

    public static class ClientConfiguration implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            ProtocolMapperModel mapper = GroupMembershipMapper.create("ldap-groups", "ldap-groups", false, null, true, true, false);
            mapper.getConfig().put("full.path", "true");
            return client.clientId("test-app").secret("password").directAccessGrantsEnabled(true)
                    .protocolMappers(ModelToRepresentation.toRepresentation(mapper));
        }
    }

    // Keep server-side lambdas separate from the client-side LDAP server and admin client dependencies.
    public static class ServerCode {

        public static RunOnServer seedLdapUser() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                ComponentModel model = LDAPTestUtils.getLdapProviderModel(realm);
                LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, model);
                LDAPTestUtils.removeAllLDAPUsers(provider, realm);
                LDAPObject user = LDAPTestUtils.addLDAPUser(provider, realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
                LDAPTestUtils.updateLDAPPassword(provider, user, "Password1");
            };
        }

        public static RunOnServer seedLdapGroups() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                ComponentModel model = LDAPTestUtils.getLdapProviderModel(realm);
                LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, model);
                LDAPTestUtils.removeAllLDAPGroups(session, realm, model, "groupsMapper");
                LDAPObject parent = LDAPTestUtils.createLDAPGroup(session, realm, model, "group1");
                LDAPObject child = LDAPTestUtils.createLDAPGroup(session, realm, model, "group11");
                LDAPUtils.addMember(provider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", parent, child);
                // READ_ONLY membership must originate in LDAP, not in the local user's group mappings.
                LDAPObject user = provider.loadLDAPUserByUsername(realm, "johnkeycloak");
                LDAPUtils.addMember(provider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", child, user);
            };
        }

        public static RunOnServer addGroup(String name) {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                ComponentModel model = LDAPTestUtils.getLdapProviderModel(realm);
                LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, model);
                LDAPObject group = LDAPTestUtils.createLDAPGroup(session, realm, model, name);
                LDAPObject user = provider.loadLDAPUserByUsername(realm, "johnkeycloak");
                LDAPUtils.addMember(provider, MembershipType.DN, LDAPConstants.MEMBER, "not-used", group, user);
            };
        }
    }
}
