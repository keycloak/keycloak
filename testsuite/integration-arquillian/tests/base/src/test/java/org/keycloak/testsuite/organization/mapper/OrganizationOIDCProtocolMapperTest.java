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

package org.keycloak.testsuite.organization.mapper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationMembershipMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.KcOidcBrokerConfiguration;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrganizationOIDCProtocolMapperTest extends AbstractOrganizationTest {

    @Before
    public void onBefore() {
        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, null);
        setMapperConfig(ProtocolMapperUtils.MULTIVALUED, null);
    }

    @Test
    public void testPasswordGrantType() throws Exception {
        OrganizationResource orga = testRealm().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = testRealm().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        Assert.assertTrue(orga.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        Assert.assertTrue(orgb.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));

        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getScope(), containsString("organization"));

        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();

        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        List<String> claim = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(claim, notNullValue());
        String orgaName = orga.toRepresentation().getName();
        String orgbName = orgb.toRepresentation().getName();
        assertThat(claim.contains(orgaName), is(true));
        assertThat(claim.contains(orgbName), is(true));
    }

    @Test
    public void testMultipleOrganizationScopes() throws Exception {
        OrganizationResource orga = testRealm().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = testRealm().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        Assert.assertTrue(orga.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        Assert.assertTrue(orgb.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));

        oauth.clientId("test-app");
        oauth.scope("openid organization organization:org-a");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());

        oauth.scope("openid organization organization:*");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());

        oauth.scope("openid organization:org-a organization:*");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testOrganizationNotAddedByGroupMapper() throws Exception {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        addMember(organization);
        ClientRepresentation client = testRealm().clients().findByClientId("direct-grant").get(0);
        ClientResource clientResource = testRealm().clients().get(client.getId());
        clientResource.getProtocolMappers().createMapper(createGroupMapper()).close();

        oauth.client("direct-grant", "password");
        oauth.scope("openid organization");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(accessToken.getOtherClaims().get("groups"), nullValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOrganizationScopeMapsSpecificOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        // resolve organization based on the organization scope value
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization:" + orgA.getAlias());
        loginPage.open(bc.consumerRealmName());
        org.keycloak.testsuite.Assert.assertFalse(loginPage.isPasswordInputPresent());
        assertTrue(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        assertFalse(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));
        assertFalse(driver.getPageSource().contains("Your email domain matches"));

        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        String orgScope = "organization:" + orgB.getAlias();
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        assertFalse(loginPage.isPasswordInputPresent());
        assertTrue(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));
        assertFalse(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        loginPage.loginUsername(member.getEmail());
        assertTrue(loginPage.isPasswordInputPresent());
        assertTrue(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));
        assertFalse(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        loginPage.login(memberPassword);
        assertScopeAndClaims(orgScope, orgB);
    }

    @Test
    public void testOrganizationScopeMapsAllOrganizations() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        // resolve organization based on the organization scope value
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization:" + orgA.getAlias());
        loginPage.open(bc.consumerRealmName());
        assertFalse(loginPage.isPasswordInputPresent());
        assertTrue(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        assertFalse(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));

        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization:*";
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        assertThat(organizations.contains(orgB.getAlias()), is(true));
        assertThat(response.getRefreshToken(), notNullValue());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString(orgScope));
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), containsString(orgScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        assertThat(organizations.contains(orgB.getAlias()), is(true));
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString(orgScope));
    }

    @Test
    public void testOrganizationScopeAnyMapsSingleOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());

        // resolve organization based on the organization scope value
        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        String orgScope = "organization";
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);

        assertScopeAndClaims(orgScope, orgA);
    }

    @Test
    public void testOrganizationScopeAnyAskUserToSelectOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        oauth.scope("organization");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        assertTrue(selectOrganizationPage.isCurrent());
        assertFalse(driver.getPageSource().contains("kc-select-try-another-way-form"));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        // for now, return the organization scope in the response and access token even though no organization is mapped into the token
        // once we support the user to select an organization, the selected organization will be mapped
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(organizations.contains(orgA.getAlias()), is(false));
        assertThat(organizations.contains(orgB.getAlias()), is(true));
    }

    @Test
    public void testRefreshTokenWithAllOrganizationsAskingForSpecificOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        String orgScope = "organization:*";
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        orgScope = "organization:orga";
        oauth.scope(orgScope).openid(false);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), containsString(orgScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
    }

    @Test
    public void testRefreshTokenWithSingleOrganizationsAskingAllOrganizations() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        String originalScope = "organization:orga";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        orgScope = "organization:*";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), containsString(originalScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(originalScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
    }

    @Test
    public void testRefreshTokenWithSingleOrganizationsAskingDifferentOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        String originalScope = "organization:orga";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        orgScope = "organization:orgb";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), not(containsString(originalScope)));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), not(containsString(orgScope)));
        assertThat(accessToken.getScope(), not(containsString(originalScope)));
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    @Test
    public void testRefreshTokenScopeAnyAskingAllOrganizations() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        String originalScope = "organization";
        oauth.scope(originalScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        assertTrue(selectOrganizationPage.isCurrent());
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        // for now, return the organization scope in the response and access token even though no organization is mapped into the token
        // once we support the user to select an organization, the selected organization will be mapped
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.contains(orgB.getAlias()), is(true));
        String orgScope = "organization:*";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), containsString(originalScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(originalScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.contains(orgB.getAlias()), is(true));
    }

    @Test
    public void testRefreshTokenScopeAnyAskingSingleOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization";
        oauth.scope(originalScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        assertTrue(selectOrganizationPage.isCurrent());
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.contains(orgB.getAlias()), is(true));
        String orgScope = "organization:orgb";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), not(containsString(orgScope)));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), not(containsString(orgScope)));
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    @Test
    public void testIncludeOrganizationAttributes() throws Exception {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        addMember(organization);
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ATTRIBUTES, Boolean.TRUE.toString());

        oauth.client("direct-grant", "password");
        oauth.scope("openid organization");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        Map<String, Map<String, List<String>>> organizations = (Map<String, Map<String, List<String>>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(organizationName));
        assertThat(organizations.get(organizationName).keySet(), hasItem("key"));
        assertThat(organizations.get(organizationName).get("key"), containsInAnyOrder("value1", "value2"));

        // when attributes are added to tokens, the claim type is a json regardless of the value set in the config
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ATTRIBUTES, Boolean.TRUE.toString());
        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, "boolean");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Map<String, List<String>>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(organizationName));
        assertThat(organizations.get(organizationName).keySet(), hasItem("key"));
        assertThat(organizations.get(organizationName).get("key"), containsInAnyOrder("value1", "value2"));

        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ATTRIBUTES, Boolean.FALSE.toString());
        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, "JSON");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Map<String, List<String>>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(organizationName));
        assertThat(organizations.get(organizationName).keySet().isEmpty(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIncludeOrganizationId() throws Exception {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource organization = testRealm().organizations().get(orgRep.getId());
        addMember(organization);
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ID, Boolean.TRUE.toString());

        oauth.client("direct-grant", "password");
        oauth.scope("openid organization");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        Map<String, Map<String, String>> organizations = (Map<String, Map<String, String>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(organizationName));
        assertThat(organizations.get(organizationName).keySet(), hasItem("id"));
        assertThat(organizations.get(organizationName).get("id"), equalTo(orgRep.getId()));

        // when id is added to tokens, the claim type is a json regardless of the value set in the config
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ID, Boolean.TRUE.toString());
        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, "boolean");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Map<String, String>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(organizationName));
        assertThat(organizations.get(organizationName).keySet(), hasItem("id"));
        assertThat(organizations.get(organizationName).get("id"), equalTo(orgRep.getId()));

        // disabling the attribute should result in no ids in the claims.
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ID, Boolean.FALSE.toString());
        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, "JSON");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Map<String, String>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(organizationName));
        assertThat(organizations.get(organizationName).keySet().isEmpty(), is(true));
    }

    @Test
    public void testOrganizationsClaimAsList() throws Exception {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(member.getEmail(), memberPassword);
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations, containsInAnyOrder("orga", "orgb"));
    }

    @Test
    public void testOrganizationsClaimSingleValued() throws Exception {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        setMapperConfig(ProtocolMapperUtils.MULTIVALUED, Boolean.FALSE.toString());
        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(member.getEmail(), memberPassword);
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        String organization = (String) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organization, is(oneOf("orga", "orgb")));
    }

    @Test
    public void testInvalidOrganizationScope() throws MalformedURLException {
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization:unknown");
        oauth.realm(TEST_REALM_NAME);
        oauth.openLoginForm();
        MultivaluedHashMap<String, String> queryParams = UriUtils.decodeQueryString(new URL(driver.getCurrentUrl()).getQuery());
        assertEquals("invalid_scope", queryParams.getFirst("error"));
    }

    @Test
    public void testAuthenticatingUsingBroker() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);

        oauth.scope(OAuth2Constants.ORGANIZATION);
        openIdentityFirstLoginPage(bc.getUserEmail(), true, idp.getAlias(), false, false);

        loginOrgIdp(bc.getUserEmail(), bc.getUserEmail(),true, true);

        assertIsMember(bc.getUserEmail(), organization);
        UserRepresentation user = testRealm().users().search(bc.getUserEmail()).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = testRealm().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(bc.getIDPAlias(), federatedIdentities.get(0).getIdentityProvider());
    }

    @Test
    public void testMapDifferentOrganizationWhenReAuthenticating() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization:orga";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        orgScope = "organization:orgb";
        oauth.scope(orgScope);
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();
        response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgB.getAlias()), is(true));
    }

    @Test
    public void testSelectOrganizationMapDifferentOrganizationWhenReAuthenticating() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        selectOrganizationPage.selectOrganization(orgA.getAlias());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        orgScope = "organization:orgb";
        oauth.scope(orgScope);
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();
        response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgB.getAlias()), is(true));
    }

    @Test
    public void testForceSelectingOrganizationWhenReAuthenticatingUsingDifferentClient() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        ClientRepresentation client = testRealm().clients().findByClientId("broker-app").get(0);
        client.setId(null);
        client.setClientId("broker-app2");
        testRealm().clients().create(client).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization:orga";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        orgScope = "organization";
        oauth.client("broker-app2", "broker-app-secret");
        oauth.scope(orgScope);
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgB.getAlias()), is(true));
    }

    @Test
    public void testReAuthenticationUserMemberOfSingleOrganizationUsingDifferentClient() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        ClientRepresentation client = testRealm().clients().findByClientId("broker-app").get(0);
        client.setId(null);
        client.setClientId("broker-app2");
        testRealm().clients().create(client).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        assertSuccessfulCodeGrant();
        oauth.client("broker-app2", "broker-app-secret");
        oauth.openLoginForm();
        assertSuccessfulCodeGrant();
        oauth.scope("organization");
        oauth.openLoginForm();
        assertSuccessfulCodeGrant();
        oauth.scope("organization:" + orgA.getAlias());
        oauth.openLoginForm();
        assertSuccessfulCodeGrant();
        oauth.scope("organization:*");
        oauth.openLoginForm();
        assertSuccessfulCodeGrant();
    }

    @Test
    public void testReAuthenticationUserNotMemberOfOrganizationUsingDifferentClient() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        testRealm().organizations().get(orgA.getId()).members().member(member.getId()).delete().close();
        ClientRepresentation client = testRealm().clients().findByClientId("broker-app").get(0);
        client.setId(null);
        client.setClientId("broker-app2");
        testRealm().clients().create(client).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        assertSuccessfulCodeGrant();
        oauth.client("broker-app2", "broker-app-secret");
        oauth.openLoginForm();
        assertSuccessfulCodeGrant();
        oauth.scope("organization");
        oauth.openLoginForm();
        assertSuccessfulCodeGrant();
        oauth.client("broker-app2", "broker-app-secret");
        oauth.scope("organization:" + orgA.getAlias());
        oauth.openLoginForm();
        assertSuccessfulCodeGrant();
        oauth.scope("organization:*");
        oauth.openLoginForm();
        assertSuccessfulCodeGrant();
    }

    @Test
    public void testDoNotAskToSelectOrganizationIfOrganizationScopeNotPresent() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization";
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        selectOrganizationPage.selectOrganization(orgA.getAlias());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        oauth.scope("openid");
        oauth.openLoginForm();
        response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), not(containsString(orgScope)));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), not(containsString(orgScope)));
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    @Test
    public void testSelectDifferentOrganizationWhenReAuthenticating() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization";
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        selectOrganizationPage.selectOrganization(orgA.getAlias());
        loginPage.login(memberPassword);
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        oauth.openLoginForm();
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgB.getAlias()), is(true));
    }

    @Test
    public void testCustomOrganizationScopeName() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        ClientScopeRepresentation orgScope = testRealm().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();
        ClientScopeResource orgScopeResource = testRealm().clientScopes().get(orgScope.getId());
        ProtocolMapperRepresentation orgMapper = orgScopeResource.getProtocolMappers().getMappers().stream()
                .filter(m -> OIDCLoginProtocolFactory.ORGANIZATION.equals(m.getName()))
                .findAny()
                .orElseThrow();
        orgMapper.setId(null);
        orgScope.setProtocolMappers(List.of(orgMapper));
        orgScope.setId(null);
        orgScope.setName("org");
        String createdId = ApiUtil.getCreatedId(testRealm().clientScopes().create(orgScope));
        testRealm().addDefaultDefaultClientScope(createdId);
        ClientRepresentation client = testRealm().clients().findByClientId("broker-app").get(0);
        testRealm().clients().get(client.getId()).addDefaultClientScope(createdId);
        getCleanup().addCleanup(() -> testRealm().clientScopes().get(createdId).remove());

        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        String scopeName = "org:" + orgA.getAlias();
        oauth.scope(scopeName);
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);

        assertScopeAndClaims(scopeName, orgA);
    }

    @Test
    public void testCustomOrganizationScopeNameAllOrganizations() {
        OrganizationResource orga = testRealm().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = testRealm().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        Assert.assertTrue(orga.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        Assert.assertTrue(orgb.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));

        ClientScopeRepresentation orgScope = testRealm().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();
        ClientScopeResource orgScopeResource = testRealm().clientScopes().get(orgScope.getId());
        ProtocolMapperRepresentation orgMapper = orgScopeResource.getProtocolMappers().getMappers().stream()
                .filter(m -> OIDCLoginProtocolFactory.ORGANIZATION.equals(m.getName()))
                .findAny()
                .orElseThrow();
        orgMapper.setId(null);
        orgScope.setProtocolMappers(List.of(orgMapper));
        orgScope.setId(null);
        orgScope.setName("org");
        String createdId = ApiUtil.getCreatedId(testRealm().clientScopes().create(orgScope));
        testRealm().addDefaultDefaultClientScope(createdId);
        ClientRepresentation client = testRealm().clients().findByClientId("broker-app").get(0);
        testRealm().clients().get(client.getId()).addDefaultClientScope(createdId);
        getCleanup().addCleanup(() -> testRealm().clientScopes().get(createdId).remove());

        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        String scopeName = "org:*";
        oauth.scope(scopeName);
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertThat(response.getScope(), containsString(scopeName));
        assertThat(List.of(response.getScope().split(" ")), not(hasItem("org")));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(scopeName));
        assertThat(List.of(accessToken.getScope().split(" ")), not(hasItem("org")));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat((List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION), hasSize(2));
    }

    @Test
    public void testClaimNotMappedIfUserNotMemberWhenDefaultClientScope() {
        OrganizationRepresentation orgARep = createOrganization("orga", true);
        OrganizationResource orgA = testRealm().organizations().get(orgARep.getId());
        MemberRepresentation member = addMember(orgA, "member@" + orgARep.getDomains().iterator().next().getName());
        orgA.members().member(member.getId()).delete().close();

        ClientRepresentation clientRep = testRealm().clients().findByClientId("broker-app").get(0);
        ClientResource client = testRealm().clients().get(clientRep.getId());
        ClientScopeRepresentation orgScopeRep = client.getOptionalClientScopes().stream().filter(scope -> "organization".equals(scope.getName())).findAny().orElse(null);
        client.removeOptionalClientScope(orgScopeRep.getId());
        client.addDefaultClientScope(orgScopeRep.getId());
        getCleanup().addCleanup(() -> {
            client.removeDefaultClientScope(orgScopeRep.getId());
            client.addOptionalClientScope(orgScopeRep.getId());
        });
        // resolve organization based on the organization scope value
        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        oauth.scope(null);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertThat(response.getScope(), containsString(orgScopeRep.getName()));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScopeRep.getName()));
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    @Test
    public void testClaimNotMappedIfUserNotMemberWhenScopeOrgAliasRequested() {
        OrganizationRepresentation orgARep = createOrganization("orga", true);
        assertClaimNotMapped("organization:" + orgARep.getAlias(), orgARep, false);
    }

    @Test
    public void testClaimNotMappedIfUserNotMemberWhenScopeOrgAllRequested() {
        assertClaimNotMapped("organization:*", createOrganization("orga", true), false);
    }

    @Test
    public void testClaimNotMappedIfUserNotMemberWhenScopeOrgRequested() {
        assertClaimNotMapped("organization", createOrganization("orga", true), true);
    }

    @Test
    public void testOrganizationsClaimMappedIfScopeInTokenDisabled() throws Exception {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        ClientRepresentation clientRep = testRealm().clients().findByClientId("broker-app").get(0);
        ClientResource client = testRealm().clients().get(clientRep.getId());
        ClientScopeRepresentation orgScopeRep = client.getOptionalClientScopes().stream().filter(scope -> "organization".equals(scope.getName())).findAny().orElse(null);
        orgScopeRep.setAttributes(Map.of(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "false"));
        getCleanup().addCleanup(() -> {
            orgScopeRep.setAttributes(Map.of(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "true"));
            testRealm().clientScopes().get(orgScopeRep.getId()).update(orgScopeRep);
        });
        testRealm().clientScopes().get(orgScopeRep.getId()).update(orgScopeRep);

        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(member.getEmail(), memberPassword);
        assertThat(response.getScope(), not(containsString("organization")));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organization = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organization, containsInAnyOrder("orga", "orgb"));
    }

    private AccessTokenResponse assertSuccessfulCodeGrant() {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertThat(Status.OK, is(Status.fromStatusCode(response.getStatusCode())));
        return response;
    }

    private ProtocolMapperRepresentation createGroupMapper() {
        ProtocolMapperRepresentation groupMapper = new ProtocolMapperRepresentation();
        groupMapper.setName("groups");
        groupMapper.setProtocolMapper(GroupMembershipMapper.PROVIDER_ID);
        groupMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "groups.groups");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        groupMapper.setConfig(config);
        return groupMapper;
    }

    private void assertScopeAndClaims(String orgScope, OrganizationRepresentation org) {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.contains(org.getAlias()), is(true));
        assertThat(response.getRefreshToken(), notNullValue());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString(orgScope));
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), containsString(orgScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(org.getAlias()), is(true));
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString(orgScope));
    }

    private void setMapperConfig(String key, String value) {
        ClientScopeRepresentation orgScope = testRealm().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();
        ClientScopeResource orgScopeResource = testRealm().clientScopes().get(orgScope.getId());
        ProtocolMapperRepresentation orgMapper = orgScopeResource.getProtocolMappers().getMappers().stream()
                .filter(m -> OIDCLoginProtocolFactory.ORGANIZATION.equals(m.getName()))
                .findAny()
                .orElseThrow();

        Map<String, String> config = orgMapper.getConfig();

        if (value == null) {
            config.remove(key);
        } else {
            config.put(key, value);
        }

        orgScopeResource.getProtocolMappers().update(orgMapper.getId(), orgMapper);
    }

    private void assertClaimNotMapped(String orgScope, OrganizationRepresentation orgARep, boolean grantScope) {
        OrganizationResource orgA = testRealm().organizations().get(orgARep.getId());
        MemberRepresentation member = addMember(orgA, "member@" + orgARep.getDomains().iterator().next().getName());
        orgA.members().member(member.getId()).delete().close();
        oauth.client("broker-app", KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertThat(response.getScope(), grantScope ? containsString(orgScope) : not(containsString(orgScope)));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), grantScope ? containsString(orgScope) : not(containsString(orgScope)));
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }
}
