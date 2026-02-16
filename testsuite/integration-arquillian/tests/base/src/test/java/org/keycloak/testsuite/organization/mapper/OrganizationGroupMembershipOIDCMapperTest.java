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

package org.keycloak.testsuite.organization.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationGroupMembershipMapper;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationMembershipMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

public class OrganizationGroupMembershipOIDCMapperTest extends AbstractOrganizationTest {

    @Before
    public void addGroupMapper() {
        // Reset to defaults
        setMapperConfig(ProtocolMapperUtils.MULTIVALUED, null);
        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, null);
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ATTRIBUTES, null);
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ID, null);

        // Add the organization group membership mapper to the organization scope
        ClientScopeRepresentation orgScope = testRealm().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();

        ClientScopeResource orgScopeResource = testRealm().clientScopes().get(orgScope.getId());

        ProtocolMapperRepresentation groupMapper = new ProtocolMapperRepresentation();
        groupMapper.setName("organization-groups");
        groupMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        groupMapper.setProtocolMapper(OrganizationGroupMembershipMapper.PROVIDER_ID);

        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        groupMapper.setConfig(config);

        orgScopeResource.getProtocolMappers().createMapper(groupMapper).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNestedGroupsWithRelativePaths() throws Exception {
        // Create organization
        OrganizationRepresentation orgRep = createOrganization("acme");
        OrganizationResource org = testRealm().organizations().get(orgRep.getId());

        // Add member
        MemberRepresentation member = addMember(org);

        // Create nested groups: engineering -> backend
        GroupRepresentation engineering = new GroupRepresentation();
        engineering.setName("engineering");
        String engineeringId;
        try (Response response = org.groups().addTopLevelGroup(engineering)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation backend = new GroupRepresentation();
        backend.setName("backend");
        String backendId;
        try (Response response = org.groups().group(engineeringId).addSubGroup(backend)) {
            backendId = response.readEntity(GroupRepresentation.class).getId();
        }

        // Add member to both engineering and backend groups
        org.groups().group(engineeringId).addMember(member.getId());
        org.groups().group(backendId).addMember(member.getId());

        // Test single String scenario: MULTIVALUED=false causes OrganizationMembershipMapper to output "acme" instead of ["acme"]
        setMapperConfig(ProtocolMapperUtils.MULTIVALUED, Boolean.FALSE.toString());

        // Authenticate
        oauth.client("direct-grant", "password");
        oauth.scope("openid organization");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);

        // Verify nested paths are relative (no org UUID prefix)
        AccessToken token = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        Map<String, Object> orgClaims = (Map<String, Object>) token.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        Map<String, Object> acmeData = (Map<String, Object>) orgClaims.get("acme");
        List<String> groups = (List<String>) acmeData.get("groups");

        assertThat(groups, hasSize(2));
        assertThat(groups, containsInAnyOrder("/engineering", "/engineering/backend"));

        // Verify paths don't contain organization UUID
        groups.forEach(path -> {
            assertThat(path, startsWith("/"));
            assertThat("Path should not contain UUID", path, not(containsString(orgRep.getId())));
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmptyGroupsWhenUserHasNoGroups() throws Exception {
        // Create organization
        OrganizationRepresentation orgRep = createOrganization("acme");
        OrganizationResource org = testRealm().organizations().get(orgRep.getId());

        // Add member (but don't add to any groups)
        addMember(org);

        // Authenticate
        oauth.client("direct-grant", "password");
        oauth.scope("openid organization");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);

        // Verify groups array is empty
        AccessToken token = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        Map<String, Object> orgClaims = (Map<String, Object>) token.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        Map<String, Object> acmeData = (Map<String, Object>) orgClaims.get("acme");
        List<String> groups = (List<String>) acmeData.get("groups");

        assertThat(groups, notNullValue());
        assertThat(groups, empty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCompositionWithAttributesAndId() throws Exception {
        // Test that groups are added alongside attributes and ID from OrganizationMembershipMapper
        OrganizationRepresentation orgA = createOrganization("org-a");
        OrganizationRepresentation orgB = createOrganization("org-b");

        OrganizationResource orgAResource = testRealm().organizations().get(orgA.getId());
        OrganizationResource orgBResource = testRealm().organizations().get(orgB.getId());

        // Add member to both orgs
        MemberRepresentation member = addMember(orgAResource);
        orgBResource.members().addMember(member.getId()).close();

        // Create groups in both orgs
        GroupRepresentation engineering = new GroupRepresentation();
        engineering.setName("engineering");
        String engineeringId;
        try (Response response = orgAResource.groups().addTopLevelGroup(engineering)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }
        orgAResource.groups().group(engineeringId).addMember(member.getId());

        GroupRepresentation sales = new GroupRepresentation();
        sales.setName("sales");
        String salesId;
        try (Response response = orgBResource.groups().addTopLevelGroup(sales)) {
            salesId = ApiUtil.getCreatedId(response);
        }
        orgBResource.groups().group(salesId).addMember(member.getId());

        // Enable organization attributes and ID in the OrganizationMembershipMapper
        setMapperConfig(ProtocolMapperUtils.MULTIVALUED, Boolean.TRUE.toString());
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ATTRIBUTES, Boolean.TRUE.toString());
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ID, Boolean.TRUE.toString());

        // Authenticate with organization:* scope
        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);

        // Verify both orgs have their groups alongside attributes and id
        AccessToken token = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        Map<String, Object> orgClaims = (Map<String, Object>) token.getOtherClaims().get(OAuth2Constants.ORGANIZATION);

        // Verify org-a has groups, attributes and id
        Map<String, Object> orgAData = (Map<String, Object>) orgClaims.get("org-a");
        assertThat(orgAData.keySet(), containsInAnyOrder("id", "key", "groups"));
        assertThat(orgAData.values(), everyItem(notNullValue()));
        List<String> orgAGroups = (List<String>) orgAData.get("groups");
        assertThat(orgAGroups, hasSize(1));
        assertThat(orgAGroups, hasItem("/engineering"));

        // Verify org-b has groups, attributes and id
        Map<String, Object> orgBData = (Map<String, Object>) orgClaims.get("org-b");
        assertThat(orgBData.keySet(), containsInAnyOrder("id", "key", "groups"));
        assertThat(orgBData.values(), everyItem(notNullValue()));
        List<String> orgBGroups = (List<String>) orgBData.get("groups");
        assertThat(orgBGroups, hasSize(1));
        assertThat(orgBGroups, hasItem("/sales"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCompositionWithStringTypeOrgMapper() throws Exception {
        // OrganizationMembershipMapper outputs organizations: ["org-a", "org-b", "org-c"] to the token
        // OrganizationGroupMembershipMapper must preserve all 3 orgs while adding groups
        OrganizationRepresentation orgA = createOrganization("org-a");
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationRepresentation orgC = createOrganization("org-c");

        OrganizationResource orgAResource = testRealm().organizations().get(orgA.getId());
        OrganizationResource orgBResource = testRealm().organizations().get(orgB.getId());
        OrganizationResource orgCResource = testRealm().organizations().get(orgC.getId());

        // Add member to all THREE orgs
        MemberRepresentation member = addMember(orgAResource);
        orgBResource.members().addMember(member.getId()).close();
        orgCResource.members().addMember(member.getId()).close();

        // Create group ONLY in org-a
        GroupRepresentation engineering = new GroupRepresentation();
        engineering.setName("engineering");
        String engineeringId;
        try (Response response = orgAResource.groups().addTopLevelGroup(engineering)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }
        orgAResource.groups().group(engineeringId).addMember(member.getId());

        // Make sure OrganizationMembershipMapper uses String type
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ATTRIBUTES, null);
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ID, null);

        // Authenticate with organization:* to get all orgs
        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);

        // Verify all 3 orgs are in the token
        AccessToken token = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        Map<String, Object> orgClaimsMap = (Map<String, Object>) token.getOtherClaims().get(OAuth2Constants.ORGANIZATION);

        assertThat(orgClaimsMap.keySet(), containsInAnyOrder("org-a", "org-b", "org-c"));

        // org-a has groups
        Map<String, Object> orgAData = (Map<String, Object>) orgClaimsMap.get("org-a");
        assertThat(orgAData, hasKey("groups"));
        List<String> orgAGroups = (List<String>) orgAData.get("groups");
        assertThat(orgAGroups, hasSize(1));
        assertThat(orgAGroups, hasItem("/engineering"));

        // org-b has no groups
        Map<String, Object> orgBData = (Map<String, Object>) orgClaimsMap.get("org-b");
        assertThat(orgBData, hasKey("groups"));
        List<String> orgBGroups = (List<String>) orgBData.get("groups");
        assertThat(orgBGroups, empty());

        // org-c has no groups
        Map<String, Object> orgCData = (Map<String, Object>) orgClaimsMap.get("org-c");
        assertThat(orgCData, hasKey("groups"));
        List<String> orgCGroups = (List<String>) orgCData.get("groups");
        assertThat(orgCGroups, empty());
    }
}
