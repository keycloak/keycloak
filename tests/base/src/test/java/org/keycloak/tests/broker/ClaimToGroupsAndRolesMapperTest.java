/*
 * Copyright 2026 Andrzej Świętek
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


package org.keycloak.tests.broker;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@KeycloakIntegrationTest
public class ClaimToGroupsAndRolesMapperTest {

    private static final String IDP_ALIAS = "kc-oidc-idp";
    private static final String CLIENT_ID = "broker-client";
    private static final String CLIENT_SECRET = "broker-secret";
    private static final String USER_LOGIN = "testuser";
    private static final String USER_PASSWORD = "password";
    private static final String CLAIM = "entitlements";
    private static final String CLAIM_MAPPER = "entitlements-claim";
    private static final String USERINFO_CLAIM = "userinfo_entitlement";
    private static final String USERINFO_CLAIM_MAPPER = "userinfo-entitlement-claim";
    private static final String SINGLE_CLAIM = "single_entitlement";
    private static final String SINGLE_CLAIM_MAPPER = "single-entitlement-claim";

    private static final Map<String, String> ROLE_TABLE = Map.of(
            "admin", "tenant-admin",
            "viewer", "tenant-viewer",
            "editor", "tenant-editor");

    @InjectRealm(ref = "provider", config = ProviderRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @BeforeEach
    void createIdentityProvider() {
        String providerBase = providerRealm.getBaseUrl();
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(IDP_ALIAS);
        idp.setProviderId(OIDCIdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        idp.setTrustEmail(true);
        idp.setConfig(Map.of(
                "clientId", CLIENT_ID,
                "clientSecret", CLIENT_SECRET,
                "authorizationUrl", providerBase + "/protocol/openid-connect/auth",
                "tokenUrl", providerBase + "/protocol/openid-connect/token",
                "userInfoUrl", providerBase + "/protocol/openid-connect/userinfo",
                "jwksUrl", providerBase + "/protocol/openid-connect/certs",
                "defaultScope", "openid email profile",
                IdentityProviderModel.SYNC_MODE, "IMPORT"));
        try (Response response = consumerRealm.admin().identityProviders().create(idp)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }
    }

    @Test
    public void mapsAcceptedEntriesToSubgroupsAndRoles() {
        addMapper("FORCE", ROLE_TABLE);
        setClaim("acme,admin,active",
                "globex,viewer,active",
                "initech,editor,suspended",
                "bad name,editor,active",
                "zero\u200bwidth,editor,active",
                "slash/name,editor,active",
                "hooli,unknown,active",
                "short,admin");

        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(groupPaths(user), containsInAnyOrder("/tenants/acme", "/tenants/globex", "/tenants/hooli"));
        assertThat(realmRoles(user), hasItem("tenant-admin"));
        assertThat(realmRoles(user), hasItem("tenant-viewer"));
        assertThat(realmRoles(user), not(hasItem("tenant-editor")));
        assertThat(subgroupNames("/tenants"), containsInAnyOrder("acme", "globex", "hooli"));
    }

    @Test
    public void forceSyncRevokesOnlyWhatTheMapperOwns() {
        addMapper("FORCE", ROLE_TABLE);
        setClaim("acme,admin,active", "globex,viewer,active");
        loginThroughBroker();

        UserResource user = consumerUser();
        GroupRepresentation acme = consumerRealm.admin().getGroupByPath("/tenants/acme");
        GroupRepresentation nested = new GroupRepresentation();
        nested.setName("nested");
        try (Response response = consumerRealm.admin().groups().group(acme.getId()).subGroup(nested)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }
        user.joinGroup(consumerRealm.admin().getGroupByPath("/tenants/acme/nested").getId());
        user.joinGroup(consumerRealm.admin().getGroupByPath("/other").getId());
        user.roles().realmLevel().add(List.of(consumerRealm.admin().roles().get("unrelated").toRepresentation()));

        setClaim("globex,admin,active");
        logout();
        loginThroughBroker();

        assertThat(groupPaths(user), containsInAnyOrder("/tenants/globex", "/tenants/acme/nested", "/other"));
        assertThat(realmRoles(user), hasItem("tenant-admin"));
        assertThat(realmRoles(user), hasItem("unrelated"));
        assertThat(realmRoles(user), not(hasItem("tenant-viewer")));
    }

    @Test
    public void importSyncKeepsEarlierGrants() {
        addMapper("IMPORT", ROLE_TABLE);
        setClaim("acme,viewer,active");
        loginThroughBroker();

        setClaim("globex,admin,active");
        logout();
        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(groupPaths(user), containsInAnyOrder("/tenants/acme"));
        assertThat(realmRoles(user), hasItem("tenant-viewer"));
        assertThat(realmRoles(user), not(hasItem("tenant-admin")));
    }

    @Test
    public void adminRoleNotGrantedWhileAdminRoleMappingDisabled() {
        addMapper("FORCE", Map.of("admin", "tenant-admin", "viewer", "tenant-viewer"));
        String realmManagementId = realmManagementId();
        consumerRealm.admin().roles().get("tenant-admin").addComposites(List.of(manageRealmRole(realmManagementId)));
        setClaim("acme,admin,active", "globex,viewer,active");

        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(realmRoles(user), hasItem("tenant-viewer"));
        assertThat(realmRoles(user), not(hasItem("tenant-admin")));
        assertThat(user.roles().clientLevel(realmManagementId).listAll(), empty());
    }

    @Test
    public void mapperWithAdminRoleInTableIsRefusedWhileAdminRoleMappingDisabled() {
        IdentityProviderMapperRepresentation mapper = mapper("FORCE", Map.of(
                "admin", Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_REALM,
                "viewer", "tenant-viewer"));

        try (Response response = consumerRealm.admin().identityProviders().get(IDP_ALIAS).addMapper(mapper)) {
            assertThat(response.getStatus(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
        }
        assertThat(consumerRealm.admin().identityProviders().get(IDP_ALIAS).getMappers(), empty());
    }

    @Test
    public void parentGroupPathIsUsedAsSaved() {
        IdentityProviderMapperRepresentation mapper = mapper("FORCE", ROLE_TABLE);
        mapper.getConfig().put("group", "/tenants ");
        createMapper(mapper);
        setClaim("acme,admin,active");

        loginThroughBroker();

        assertThat(groupPaths(consumerUser()), containsInAnyOrder("/tenants /acme"));
    }

    @Test
    public void roleReferenceIsUsedAsSaved() {
        addMapper("FORCE", Map.of("admin", "tenant-admin", "viewer", "tenant-viewer "));
        setClaim("globex,viewer,active");
        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(realmRoles(user), hasItem("tenant-viewer "));
        assertThat(realmRoles(user), not(hasItem("tenant-viewer")));

        user.roles().realmLevel().add(List.of(consumerRealm.admin().roles().get("tenant-viewer").toRepresentation()));
        setClaim("globex,admin,active");
        logout();
        loginThroughBroker();

        assertThat(realmRoles(user), hasItem("tenant-viewer"));
        assertThat(realmRoles(user), not(hasItem("tenant-viewer ")));
    }

    @Test
    public void claimNameIsUsedAsSaved() {
        IdentityProviderMapperRepresentation mapper = mapper("FORCE", ROLE_TABLE);
        mapper.getConfig().put("claim", CLAIM + " ");
        createMapper(mapper);
        setClaim("acme,admin,active");

        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(groupPaths(user), empty());
        assertThat(realmRoles(user), not(hasItem("tenant-admin")));
    }

    @Test
    public void userInfoStringIsReadAsSent() {
        IdentityProviderMapperRepresentation mapper = mapper("FORCE", ROLE_TABLE);
        mapper.getConfig().put("claim", USERINFO_CLAIM);
        createMapper(mapper);
        setUserInfoClaim("globex,viewer,active");
        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(groupPaths(user), containsInAnyOrder("/tenants/globex"));

        setUserInfoClaim(" acme,admin,active");
        logout();
        loginThroughBroker();

        assertThat(groupPaths(user), empty());
        assertThat(realmRoles(user), not(hasItem("tenant-admin")));
    }

    @Test
    public void invalidConfigurationMapsNothing() {
        IdentityProviderMapperRepresentation mapper = mapper("FORCE", ROLE_TABLE);
        mapper.getConfig().put("role.field.index", "0");
        createMapper(mapper);
        setClaim("acme,admin,active");

        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(groupPaths(user), empty());
        assertThat(realmRoles(user), not(hasItem("tenant-admin")));
    }

    @Test
    public void legacySyncRevokesWhatTheClaimNoLongerGrants() {
        addMapper("LEGACY", ROLE_TABLE);
        setClaim("acme,admin,active");
        loginThroughBroker();

        setClaim("globex,viewer,active");
        logout();
        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(groupPaths(user), containsInAnyOrder("/tenants/globex"));
        assertThat(realmRoles(user), hasItem("tenant-viewer"));
        assertThat(realmRoles(user), not(hasItem("tenant-admin")));
    }

    @Test
    public void adminParentGroupIsNeitherExtendedNorJoined() {
        IdentityProviderMapperRepresentation mapper = mapper("FORCE", ROLE_TABLE);
        mapper.getConfig().put("group", "/admins");
        createMapper(mapper);
        String adminsId = consumerRealm.admin().getGroupByPath("/admins").getId();
        String realmManagementId = realmManagementId();
        consumerRealm.admin().groups().group(adminsId).roles().clientLevel(realmManagementId).add(List.of(manageRealmRole(realmManagementId)));
        setClaim("acme,admin,active");

        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(groupPaths(user), empty());
        assertThat(consumerRealm.admin().groups().group(adminsId).getSubGroups(0, 100, true), empty());
        assertThat(realmRoles(user), hasItem("tenant-admin"));
    }

    @Test
    public void singleStringClaimInTokenIsOneEntry() {
        IdentityProviderMapperRepresentation mapper = mapper("FORCE", ROLE_TABLE);
        mapper.getConfig().put("claim", SINGLE_CLAIM);
        createMapper(mapper);
        setProviderClaim(SINGLE_CLAIM_MAPPER, "acme,admin,active");

        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(groupPaths(user), containsInAnyOrder("/tenants/acme"));
        assertThat(realmRoles(user), hasItem("tenant-admin"));
    }

    @Test
    public void nonStringItemsAreSkipped() {
        addMapper("FORCE", ROLE_TABLE);
        setProviderClaim(CLAIM_MAPPER, "[\"acme,admin,active\", 5, {\"a\": 1}, null, true]");

        loginThroughBroker();

        assertThat(groupPaths(consumerUser()), containsInAnyOrder("/tenants/acme"));
    }

    @Test
    public void indexedClaimPathSelectsOneEntry() {
        IdentityProviderMapperRepresentation mapper = mapper("FORCE", ROLE_TABLE);
        mapper.getConfig().put("claim", CLAIM + ".current[1]");
        createMapper(mapper);
        setProviderClaim(CLAIM_MAPPER, "{\"current\": [\"globex,viewer,active\", \"acme,admin,active\"]}");

        loginThroughBroker();

        UserResource user = consumerUser();
        assertThat(groupPaths(user), containsInAnyOrder("/tenants/acme"));
        assertThat(realmRoles(user), hasItem("tenant-admin"));
        assertThat(realmRoles(user), not(hasItem("tenant-viewer")));
    }

    @Test
    public void missingDelimiterDefaultsToComma() {
        IdentityProviderMapperRepresentation mapper = mapper("FORCE", ROLE_TABLE);
        mapper.getConfig().remove("delimiter");
        createMapper(mapper);
        setClaim("acme,admin,active");

        loginThroughBroker();

        assertThat(groupPaths(consumerUser()), containsInAnyOrder("/tenants/acme"));
    }

    @Test
    public void realmRoleRenameUpdatesRoleTable() {
        addMapper("FORCE", ROLE_TABLE);

        RoleRepresentation role = consumerRealm.admin().roles().get("tenant-viewer").toRepresentation();
        role.setName("tenant-reader");
        consumerRealm.admin().roles().get("tenant-viewer").update(role);

        assertThat(roleTableRows(), containsInAnyOrder("admin=tenant-admin", "viewer=tenant-reader", "editor=tenant-editor"));
    }

    @Test
    public void clientIdChangeUpdatesRoleTable() {
        String clientUuid = createClient("tenant-app");
        createClientRole(clientUuid, "app-viewer");
        addMapper("FORCE", Map.of("viewer", "tenant-app.app-viewer", "editor", "tenant-app.missing", "admin", "tenant-admin"));

        renameClient(clientUuid, "tenant-app-renamed");

        assertThat(roleTableRows(), containsInAnyOrder(
                "viewer=tenant-app-renamed.app-viewer", "editor=tenant-app-renamed.missing", "admin=tenant-admin"));
    }

    @Test
    public void clientIdChangeRewritesOnlyReferencesThatNameTheRenamedClient() {
        String appUuid = createClient("app");
        createClientRole(appUuid, "child.viewer");
        createClientRole(appUuid, "viewer");
        createClientRole(createClient("app.child"), "viewer");
        addMapper("FORCE", Map.of("viewer", "app.child.viewer", "admin", "app.viewer"));

        renameClient(appUuid, "app-renamed");

        assertThat(roleTableRows(), containsInAnyOrder("viewer=app.child.viewer", "admin=app-renamed.viewer"));
    }

    @Test
    public void roleRenameRewritesOnlyReferencesThatNameTheRenamedRole() {
        String appUuid = createClient("app");
        createClientRole(appUuid, "child.viewer");
        String childUuid = createClient("app.child");
        createClientRole(childUuid, "viewer");
        addMapper("FORCE", Map.of("viewer", "app.child.viewer"));

        renameClientRole(appUuid, "child.viewer", "child.reader");

        assertThat(roleTableRows(), containsInAnyOrder("viewer=app.child.viewer"));

        renameClientRole(childUuid, "viewer", "reader");

        assertThat(roleTableRows(), containsInAnyOrder("viewer=app.child.reader"));
    }

    @Test
    public void roleRenameKeepsReferenceWhoseNewSpellingNamesAnotherRole() {
        String appUuid = createClient("app");
        createClientRole(appUuid, "viewer");
        createClientRole(createClient("app.child"), "viewer");
        addMapper("FORCE", Map.of("admin", "app.viewer"));

        renameClientRole(appUuid, "viewer", "child.viewer");

        assertThat(roleTableRows(), containsInAnyOrder("admin=app.viewer"));
    }

    @Test
    public void clientIdChangeKeepsReferenceWhoseNewSpellingNamesAnotherClient() {
        String appUuid = createClient("app");
        createClientRole(appUuid, "child.viewer");
        createClientRole(appUuid, "other");
        createClientRole(createClient("dest.child"), "viewer");
        addMapper("FORCE", Map.of("viewer", "app.child.viewer", "admin", "app.other"));

        renameClient(appUuid, "dest");

        assertThat(roleTableRows(), containsInAnyOrder("viewer=app.child.viewer", "admin=dest.other"));
    }

    @Test
    public void clientUpdateWithoutClientIdLeavesRoleTableUnchanged() {
        String clientUuid = createClient("tenant-app");
        createClientRole(clientUuid, "app-viewer");
        addMapper("FORCE", Map.of("viewer", "tenant-app.app-viewer"));
        ClientRepresentation descriptionOnly = new ClientRepresentation();
        descriptionOnly.setDescription("updated");

        consumerRealm.admin().clients().get(clientUuid).update(descriptionOnly);

        assertThat(consumerRealm.admin().clients().get(clientUuid).toRepresentation().getDescription(), equalTo("updated"));
        assertThat(roleTableRows(), containsInAnyOrder("viewer=tenant-app.app-viewer"));
    }

    @Test
    public void unreadableRoleTableDoesNotBlockRenames() {
        IdentityProviderMapperRepresentation broken = mapper("FORCE", ROLE_TABLE);
        broken.setName("broken");
        broken.getConfig().put("roles", "[null]");
        createMapper(broken);
        addMapper("FORCE", ROLE_TABLE);

        RoleRepresentation role = consumerRealm.admin().roles().get("tenant-viewer").toRepresentation();
        role.setName("tenant-reader");
        consumerRealm.admin().roles().get("tenant-viewer").update(role);

        assertThat(consumerRealm.admin().roles().get("tenant-reader").toRepresentation().getName(), equalTo("tenant-reader"));
        assertThat(roleTableRows(), containsInAnyOrder("admin=tenant-admin", "viewer=tenant-reader", "editor=tenant-editor"));
        assertThat(mapperNamed("broken").getConfig().get("roles"), equalTo("[null]"));
    }

    private void createClientRole(String clientUuid, String roleName) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        consumerRealm.admin().clients().get(clientUuid).roles().create(role);
    }

    private void renameClient(String clientUuid, String newClientId) {
        ClientRepresentation client = consumerRealm.admin().clients().get(clientUuid).toRepresentation();
        client.setClientId(newClientId);
        consumerRealm.admin().clients().get(clientUuid).update(client);
    }

    private void renameClientRole(String clientUuid, String roleName, String newRoleName) {
        RoleRepresentation role = consumerRealm.admin().clients().get(clientUuid).roles().get(roleName).toRepresentation();
        role.setName(newRoleName);
        consumerRealm.admin().clients().get(clientUuid).roles().get(roleName).update(role);
    }

    private String realmManagementId() {
        return consumerRealm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
    }

    private RoleRepresentation manageRealmRole(String realmManagementId) {
        return consumerRealm.admin().clients().get(realmManagementId).roles().get(AdminRoles.MANAGE_REALM).toRepresentation();
    }

    private String createClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        try (Response response = consumerRealm.admin().clients().create(client)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            return ApiUtil.getCreatedId(response);
        }
    }

    private void addMapper(String syncMode, Map<String, String> roleTable) {
        createMapper(mapper(syncMode, roleTable));
    }

    private IdentityProviderMapperRepresentation mapper(String syncMode, Map<String, String> roleTable) {
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("entitlements");
        mapper.setIdentityProviderAlias(IDP_ALIAS);
        mapper.setIdentityProviderMapper("oidc-claim-to-groups-and-roles-idp-mapper");
        mapper.setConfig(new HashMap<>(Map.of(
                "syncMode", syncMode,
                "claim", CLAIM,
                "delimiter", ",",
                "group.field.index", "0",
                "group", "/tenants",
                "role.field.index", "1",
                "roles", serialize(roleTable),
                "state.field.index", "2",
                "state.value", "active")));
        return mapper;
    }

    private void createMapper(IdentityProviderMapperRepresentation mapper) {
        try (Response response = consumerRealm.admin().identityProviders().get(IDP_ALIAS).addMapper(mapper)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }
    }

    private static String serialize(Map<String, String> roleTable) {
        List<Map<String, String>> pairs = roleTable.entrySet().stream()
                .map(row -> Map.of("key", row.getKey(), "value", row.getValue()))
                .toList();
        try {
            return JsonSerialization.writeValueAsString(pairs);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> roleTableRows() {
        return roleTable(mapperNamed("entitlements")).stream()
                .map(row -> row.get("key") + "=" + row.get("value"))
                .toList();
    }

    private IdentityProviderMapperRepresentation mapperNamed(String name) {
        return consumerRealm.admin().identityProviders().get(IDP_ALIAS).getMappers().stream()
                .filter(candidate -> name.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static List<Map<String, String>> roleTable(IdentityProviderMapperRepresentation mapper) {
        try {
            return JsonSerialization.readValue(mapper.getConfig().get("roles"), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> subgroupNames(String parentPath) {
        String parentId = consumerRealm.admin().getGroupByPath(parentPath).getId();
        return consumerRealm.admin().groups().group(parentId).getSubGroups(0, 100, true).stream()
                .map(GroupRepresentation::getName)
                .toList();
    }

    private void setClaim(String... entries) {
        try {
            setProviderClaim(CLAIM_MAPPER, JsonSerialization.writeValueAsString(List.of(entries)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setUserInfoClaim(String entry) {
        setProviderClaim(USERINFO_CLAIM_MAPPER, entry);
    }

    private void setProviderClaim(String mapperName, String value) {
        ClientResource client = providerRealm.admin().clients().get(
                providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0).getId());
        ProtocolMapperRepresentation mapper = client.getProtocolMappers().getMappers().stream()
                .filter(candidate -> mapperName.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        mapper.getConfig().put("claim.value", value);
        client.getProtocolMappers().update(mapper.getId(), mapper);
    }

    private void loginThroughBroker() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(USER_LOGIN, USER_PASSWORD);
        loginPage.submit();
        assertThat(oauth.parseLoginResponse().isSuccess(), equalTo(true));
    }

    private void logout() {
        AccountHelper.logout(consumerRealm.admin(), USER_LOGIN);
        AccountHelper.logout(providerRealm.admin(), USER_LOGIN);
    }

    private UserResource consumerUser() {
        List<UserRepresentation> users = consumerRealm.admin().users().search(USER_LOGIN, true);
        assertThat(users.size(), equalTo(1));
        return consumerRealm.admin().users().get(users.get(0).getId());
    }

    private static Set<String> groupPaths(UserResource user) {
        return user.groups().stream().map(GroupRepresentation::getPath).collect(Collectors.toSet());
    }

    private static Set<String> realmRoles(UserResource user) {
        return user.roles().realmLevel().listAll().stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }

    public static class ProviderRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .clients(ClientBuilder.create(CLIENT_ID)
                            .secret(CLIENT_SECRET)
                            .redirectUris("*")
                            .protocolMappers(
                                    ProtocolMapperBuilder.create()
                                            .name(CLAIM_MAPPER)
                                            .protocol("openid-connect")
                                            .protocolMapper("oidc-hardcoded-claim-mapper")
                                            .config("claim.name", CLAIM)
                                            .config("claim.value", "[]")
                                            .config("jsonType.label", "JSON")
                                            .config("id.token.claim", "true")
                                            .config("access.token.claim", "true")
                                            .config("userinfo.token.claim", "true")
                                            .build(),
                                    ProtocolMapperBuilder.create()
                                            .name(USERINFO_CLAIM_MAPPER)
                                            .protocol("openid-connect")
                                            .protocolMapper("oidc-hardcoded-claim-mapper")
                                            .config("claim.name", USERINFO_CLAIM)
                                            .config("claim.value", "")
                                            .config("jsonType.label", "String")
                                            .config("id.token.claim", "false")
                                            .config("access.token.claim", "false")
                                            .config("userinfo.token.claim", "true")
                                            .build(),
                                    ProtocolMapperBuilder.create()
                                            .name(SINGLE_CLAIM_MAPPER)
                                            .protocol("openid-connect")
                                            .protocolMapper("oidc-hardcoded-claim-mapper")
                                            .config("claim.name", SINGLE_CLAIM)
                                            .config("claim.value", "")
                                            .config("jsonType.label", "String")
                                            .config("id.token.claim", "true")
                                            .config("access.token.claim", "true")
                                            .config("userinfo.token.claim", "false")
                                            .build()))
                    .users(UserBuilder.create(USER_LOGIN)
                            .password(USER_PASSWORD)
                            .email(USER_LOGIN + "@example.com")
                            .emailVerified(true)
                            .firstName("Test")
                            .lastName("User")
                            .enabled(true));
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .realmRoles("tenant-admin", "tenant-viewer", "tenant-viewer ", "tenant-editor", "unrelated")
                    .groups("tenants", "tenants ", "other", "admins");
        }
    }
}
