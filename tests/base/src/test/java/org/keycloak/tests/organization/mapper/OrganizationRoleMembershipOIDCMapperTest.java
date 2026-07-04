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
package org.keycloak.tests.organization.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.protocol.mappers.OrganizationRoleMapperUtils;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationMembershipMapper;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationRoleMembershipMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest
public class OrganizationRoleMembershipOIDCMapperTest extends AbstractOrganizationTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @BeforeEach
    public void addRoleMapper() {
        ensureDirectGrantClient();
        setMapperConfig(ProtocolMapperUtils.MULTIVALUED, null);
        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, null);
        setMapperConfig(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, null);
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ATTRIBUTES, null);
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ID, null);

        ClientScopeResource organizationScope = organizationScope();
        ProtocolMapperRepresentation roleMapper = organizationScope.getProtocolMappers().getMappers().stream()
                .filter(mapper -> OrganizationRoleMembershipMapper.PROVIDER_ID.equals(mapper.getProtocolMapper()))
                .findAny()
                .orElse(null);

        if (roleMapper == null) {
            roleMapper = new ProtocolMapperRepresentation();
            roleMapper.setName("organization-roles");
            roleMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            roleMapper.setProtocolMapper(OrganizationRoleMembershipMapper.PROVIDER_ID);
            roleMapper.setConfig(new HashMap<>());
            setIncludedTokenTypes(roleMapper);
            organizationScope.getProtocolMappers().createMapper(roleMapper).close();
        } else {
            setIncludedTokenTypes(roleMapper);
            organizationScope.getProtocolMappers().update(roleMapper.getId(), roleMapper);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRolesAndCompositesAcrossTokenSurfaces() throws Exception {
        OrganizationRepresentation acmeRepresentation = createOrganization("acme");
        OrganizationRepresentation otherRepresentation = createOrganization("other");
        OrganizationResource acme = realm.admin().organizations().get(acmeRepresentation.getId());
        OrganizationResource other = realm.admin().organizations().get(otherRepresentation.getId());
        MemberRepresentation member = addMember(acme);
        other.members().addMember(member.getId()).close();

        RoleRepresentation acmeRole = createOrganizationRole(acme, "acme-admin");
        RoleRepresentation acmeChildRole = createOrganizationRole(acme, "acme-auditor");
        RoleRepresentation otherRole = createOrganizationRole(other, "other-admin");
        RoleRepresentation realmRole = createRealmRole("organization-realm-composite");
        ClientRole clientRole = createClientRole("organization-role-client", "organization-client-composite");

        acme.roles().get(acmeRole.getId()).addComposites(List.of(acmeChildRole, realmRole, clientRole.role()));
        assignOrganizationRole(acme, acmeRole, member.getId());
        assignOrganizationRole(other, otherRole, member.getId());
        addDirectGrantAudience();

        AccessTokenResponse response = authenticate("openid organization:*");
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        IDToken idToken = TokenVerifier.create(response.getIdToken(), IDToken.class).getToken();
        UserInfo userInfo = oauth.doUserInfoRequest(response.getAccessToken()).getUserInfo();
        TokenMetadataRepresentation introspection = oauth.doIntrospectionAccessTokenRequest(response.getAccessToken()).asTokenMetadata();
        assertThat(introspection.isActive(), is(true));

        for (Map<String, Object> claims : List.of(accessToken.getOtherClaims(), idToken.getOtherClaims(), userInfo.getOtherClaims(), introspection.getOtherClaims())) {
            assertRoleClaims(claims, OAuth2Constants.ORGANIZATION, "acme", "acme-admin", "acme-auditor", realmRole.getName(),
                    clientRole.clientId(), clientRole.role().getName());

            Map<String, Object> otherData = organizationData(claims, OAuth2Constants.ORGANIZATION, "other");
            List<String> otherRoles = (List<String>) otherData.get(OrganizationRoleMapperUtils.ORGANIZATION_ROLES);
            assertThat(otherRoles, hasItem("other-admin"));
            assertThat(otherRoles, not(hasItem("acme-admin")));
            assertThat(otherRoles, not(hasItem("acme-auditor")));
        }

        assertThat(accessToken.getRealmAccess().isUserInRole(realmRole.getName()), is(true));
        assertThat(accessToken.getRealmAccess().isUserInRole(acmeRole.getName()), is(false));
        assertThat(accessToken.getResourceAccess(clientRole.clientId()).isUserInRole(clientRole.role().getName()), is(true));
        assertThat(accessToken.getResourceAccess(), not(hasKey("acme")));
    }

    @Test
    public void testCustomClaimName() throws Exception {
        assertCustomClaimName("my_orgs");
    }

    @Test
    public void testDottedClaimName() throws Exception {
        assertCustomClaimName("custom.org");
    }

    @Test
    public void testNoClaimWithoutOrganizationScope() throws Exception {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization("acme").getId());
        MemberRepresentation member = addMember(organization);
        assignOrganizationRole(organization, createOrganizationRole(organization, "member"), member.getId());

        AccessTokenResponse response = authenticate("openid email profile");
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();

        assertThat(accessToken.getOtherClaims(), not(hasKey(OAuth2Constants.ORGANIZATION)));
    }

    @SuppressWarnings("unchecked")
    private void assertCustomClaimName(String claimName) throws Exception {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization("acme").getId());
        MemberRepresentation member = addMember(organization);
        RoleRepresentation role = createOrganizationRole(organization, "member");
        assignOrganizationRole(organization, role, member.getId());
        setMapperConfig(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, claimName);

        AccessTokenResponse response = authenticate("openid organization");
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        Map<String, Object> organizationData = organizationData(accessToken.getOtherClaims(), claimName, "acme");

        List<String> organizationRoles = (List<String>) organizationData.get(OrganizationRoleMapperUtils.ORGANIZATION_ROLES);
        assertThat(organizationRoles, hasItem("member"));
        assertThat(accessToken.getOtherClaims(), not(hasKey(OAuth2Constants.ORGANIZATION)));
        if (claimName.contains(".")) {
            assertThat(accessToken.getOtherClaims(), not(hasKey(claimName)));
            assertThat(accessToken.getOtherClaims(), hasKey(claimName.substring(0, claimName.indexOf('.'))));
        } else {
            assertThat(accessToken.getOtherClaims(), hasKey(claimName));
        }
    }

    private AccessTokenResponse authenticate(String scope) {
        oauth.client("direct-grant", "password");
        oauth.scope(scope);
        return oauth.doPasswordGrantRequest(memberEmail, memberPassword);
    }

    private void addDirectGrantAudience() {
        ClientRepresentation client = realm.admin().clients().findByClientId("direct-grant").get(0);
        ClientResource clientResource = realm.admin().clients().get(client.getId());
        ProtocolMapperRepresentation mapper = ModelToRepresentation.toRepresentation(AudienceProtocolMapper.createClaimMapper(
                "direct-grant-audience", "direct-grant", null, true, false, false));
        String mapperId;
        try (Response response = clientResource.getProtocolMappers().createMapper(mapper)) {
            mapperId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(client.getId()).getProtocolMappers().delete(mapperId));
    }

    private ClientScopeResource organizationScope() {
        ClientScopeRepresentation scope = realm.admin().clientScopes().findAll().stream()
                .filter(candidate -> OIDCLoginProtocolFactory.ORGANIZATION.equals(candidate.getName()))
                .findAny()
                .orElseThrow();
        return realm.admin().clientScopes().get(scope.getId());
    }

    private void ensureDirectGrantClient() {
        if (!realm.admin().clients().findByClientId("direct-grant").isEmpty()) {
            return;
        }

        ClientRepresentation client = ClientBuilder.create("direct-grant")
                .secret("password")
                .directAccessGrantsEnabled()
                .optionalClientScopes(OIDCLoginProtocolFactory.ORGANIZATION)
                .build();

        try (Response response = realm.admin().clients().create(client)) {
            String clientId = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.clients().get(clientId).remove());
        }
    }

    private static void setIncludedTokenTypes(ProtocolMapperRepresentation mapper) {
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.TRUE.toString());
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, Boolean.TRUE.toString());
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, Boolean.TRUE.toString());
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, Boolean.TRUE.toString());
    }

    private RoleRepresentation createOrganizationRole(OrganizationResource organization, String name) {
        RoleRepresentation role = new RoleRepresentation(name, "", false);
        try (Response response = organization.roles().create(role)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            return organization.roles().get(ApiUtil.getCreatedId(response)).toRepresentation();
        }
    }

    private RoleRepresentation createRealmRole(String name) {
        realm.admin().roles().create(new RoleRepresentation(name, "", false));
        return realm.admin().roles().get(name).toRepresentation();
    }

    private ClientRole createClientRole(String clientId, String roleName) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setEnabled(true);
        String clientUuid;
        try (Response response = realm.admin().clients().create(client)) {
            clientUuid = ApiUtil.getCreatedId(response);
        }
        realm.admin().clients().get(clientUuid).roles().create(new RoleRepresentation(roleName, "", false));
        RoleRepresentation role = realm.admin().clients().get(clientUuid).roles().get(roleName).toRepresentation();
        return new ClientRole(clientId, role);
    }

    private static void assignOrganizationRole(OrganizationResource organization, RoleRepresentation role, String userId) {
        UserRepresentation user = new UserRepresentation();
        user.setId(userId);
        organization.roles().get(role.getId()).addUserMembers(List.of(user));
    }

    @SuppressWarnings("unchecked")
    private static void assertRoleClaims(Map<String, Object> claims, String claimName, String organizationAlias,
            String directRole, String organizationComposite, String realmComposite, String clientId, String clientComposite) {
        Map<String, Object> data = organizationData(claims, claimName, organizationAlias);
        List<String> organizationRoles = (List<String>) data.get(OrganizationRoleMapperUtils.ORGANIZATION_ROLES);
        assertThat(organizationRoles, hasItem(directRole));
        assertThat(organizationRoles, hasItem(organizationComposite));

        Map<String, Object> realmAccess = (Map<String, Object>) data.get(OrganizationRoleMapperUtils.REALM_ACCESS);
        assertThat((List<String>) realmAccess.get(OrganizationRoleMapperUtils.ROLES), hasItem(realmComposite));

        Map<String, Object> resourceAccess = (Map<String, Object>) data.get(OrganizationRoleMapperUtils.RESOURCE_ACCESS);
        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
        assertThat((List<String>) clientAccess.get(OrganizationRoleMapperUtils.ROLES), hasItem(clientComposite));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> organizationData(Map<String, Object> claims, String claimName, String organizationAlias) {
        Object current = claims;
        for (String segment : claimName.split("\\.")) {
            assertThat(current, notNullValue());
            current = ((Map<String, Object>) current).get(segment);
        }
        assertThat(current, notNullValue());
        Map<String, Object> organizationClaims = (Map<String, Object>) current;
        assertThat(organizationClaims, hasKey(organizationAlias));
        return (Map<String, Object>) organizationClaims.get(organizationAlias);
    }

    private record ClientRole(String clientId, RoleRepresentation role) {
    }
}
