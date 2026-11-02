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


package org.keycloak.tests.organization.mapper;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class ClaimToGroupsAndRolesMapperOrganizationTest extends AbstractOrganizationTest {

    private static final String CLAIM = "entitlements";
    private static final String CLAIM_MAPPER = "entitlements-claim";

    @InjectRealm(ref = "provider", config = ProviderRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectUser(ref = "alice", realmRef = "provider", config = AliceUserConf.class)
    ManagedUser alice;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @InjectPage
    LoginUpdateProfilePage loginUpdateProfilePage;

    @BeforeEach
    public void onBefore() {
        createTestClients();
    }

    @Test
    public void mapsEntriesToSubgroupsOfAnOrganizationGroup() {
        OrganizationResource organization = createOrganizationWithBroker(providerRealm, true, MembershipType.UNMANAGED);
        String tenantsId = addTopLevelGroup(organization, "tenants");
        addMapper(organization, organization.groups().group(tenantsId).toRepresentation(false).getPath());
        setClaim("[\"acme,admin\"]");

        UserRepresentation user = loginViaBroker(alice.getEmail(), alice.getUsername(), alice.getPassword(),
                oauth, loginUsernamePage, loginPage, providerRealm);

        assertThat(subgroupNames(organization, tenantsId), contains("acme"));
        assertThat(memberIds(organization, subgroupId(organization, tenantsId, "acme")), contains(user.getId()));

        setClaim("[\"globex,admin\"]");
        realm.admin().users().get(user.getId()).logout();
        providerRealm.admin().logoutAll();
        loginViaBroker(alice.getEmail(), alice.getUsername(), alice.getPassword(),
                oauth, loginUsernamePage, loginPage, providerRealm);

        assertThat(subgroupNames(organization, tenantsId), containsInAnyOrder("acme", "globex"));
        assertThat(memberIds(organization, subgroupId(organization, tenantsId, "acme")), empty());
        assertThat(memberIds(organization, subgroupId(organization, tenantsId, "globex")), contains(user.getId()));
    }

    @Test
    public void mapsNothingForUserWhoIsNotAMember() {
        OrganizationResource organization = createOrganizationWithBroker(providerRealm, false, MembershipType.UNMANAGED);
        String tenantsId = addTopLevelGroup(organization, "tenants");
        addMapper(organization, organization.groups().group(tenantsId).toRepresentation(false).getPath());
        setClaim("[\"acme,admin\"]");

        UserRepresentation user = loginViaBroker(alice.getEmail(), alice.getUsername(), alice.getPassword(),
                alice.getEmail(), oauth, loginUsernamePage, loginPage, loginUpdateProfilePage, providerRealm);

        assertThrows(NotFoundException.class, () -> organization.members().member(user.getId()).toRepresentation());
        assertThat(subgroupNames(organization, tenantsId), empty());
        assertThat(memberIds(organization, tenantsId), empty());
        assertThat(realm.admin().users().get(user.getId()).groups(), empty());
    }

    private static String addTopLevelGroup(OrganizationResource organization, String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        try (Response response = organization.groups().addTopLevelGroup(group)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            return ApiUtil.getCreatedId(response);
        }
    }

    private void addMapper(OrganizationResource organization, String parentGroupPath) {
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("entitlements");
        mapper.setIdentityProviderAlias(organizationName + "-identity-provider");
        mapper.setIdentityProviderMapper("oidc-claim-to-groups-and-roles-idp-mapper");
        mapper.setConfig(Map.of(
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.FORCE.name(),
                "claim", CLAIM,
                "delimiter", ",",
                "group.field.index", "0",
                ConfigConstants.GROUP, parentGroupPath,
                ConfigConstants.GROUP_TYPE, GroupModel.Type.ORGANIZATION.name(),
                ConfigConstants.ORGANIZATION_ID, organization.toRepresentation().getId()));
        try (Response response = realm.admin().identityProviders().get(mapper.getIdentityProviderAlias()).addMapper(mapper)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }
    }

    private void setClaim(String json) {
        String clientId = providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0).getId();
        ProtocolMapperRepresentation mapper = providerRealm.admin().clients().get(clientId).getProtocolMappers().getMappers().stream()
                .filter(candidate -> CLAIM_MAPPER.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        mapper.getConfig().put("claim.value", json);
        providerRealm.admin().clients().get(clientId).getProtocolMappers().update(mapper.getId(), mapper);
    }

    private static List<String> subgroupNames(OrganizationResource organization, String groupId) {
        return organization.groups().group(groupId).getSubGroups(null, null, null, null).stream()
                .map(GroupRepresentation::getName)
                .toList();
    }

    private static String subgroupId(OrganizationResource organization, String groupId, String name) {
        return organization.groups().group(groupId).getSubGroups(null, null, null, null).stream()
                .filter(group -> name.equals(group.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private static List<String> memberIds(OrganizationResource organization, String groupId) {
        return organization.groups().group(groupId).getMembers(null, null, false).stream()
                .map(MemberRepresentation::getId)
                .toList();
    }

    public static class ProviderRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(ClientBuilder.create(CLIENT_ID)
                    .name(CLIENT_ID)
                    .secret(CLIENT_SECRET)
                    .redirectUris("*")
                    .protocolMappers(ProtocolMapperBuilder.create()
                            .name(CLAIM_MAPPER)
                            .protocol("openid-connect")
                            .protocolMapper("oidc-hardcoded-claim-mapper")
                            .config("claim.name", CLAIM)
                            .config("claim.value", "[]")
                            .config("jsonType.label", "JSON")
                            .config("id.token.claim", "true")
                            .config("access.token.claim", "true")
                            .config("userinfo.token.claim", "true")
                            .build()));
        }
    }
}
