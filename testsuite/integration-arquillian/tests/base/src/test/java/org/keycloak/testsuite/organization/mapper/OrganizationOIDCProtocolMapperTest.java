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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.OrganizationModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.broker.KcOidcBrokerConfiguration;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationOIDCProtocolMapperTest extends AbstractOrganizationTest {

    @Test
    public void testPasswordGrantType() throws Exception {
        OrganizationResource orga = testRealm().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = testRealm().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        Assert.assertTrue(orga.members().getAll().stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        Assert.assertTrue(orgb.members().getAll().stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));

        oauth.clientId("direct-grant");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", memberEmail, memberPassword);
        assertThat(response.getScope(), containsString("organization"));

        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();

        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        Map<String, Object> claim = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(claim, notNullValue());
        assertThat(claim.get(orga.toRepresentation().getName()), notNullValue());
        String orgaId = orga.toRepresentation().getName();
        String orgbId = orgb.toRepresentation().getName();
        assertThat(claim.get(orgaId), notNullValue());
        assertThat(claim.get(orgbId), notNullValue());
    }

    @Test
    public void testOrganizationNotAddedByGroupMapper() throws Exception {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        addMember(organization);
        ClientRepresentation client = testRealm().clients().findByClientId("direct-grant").get(0);
        ClientResource clientResource = testRealm().clients().get(client.getId());
        clientResource.getProtocolMappers().createMapper(createGroupMapper()).close();

        oauth.clientId("direct-grant");
        oauth.scope("openid organization");
        AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", memberEmail, memberPassword);
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(accessToken.getOtherClaims().get("groups"), nullValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOrganizationScopeMapsSpecificOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        // resolve organization based on the organization scope value
        oauth.clientId("broker-app");
        oauth.scope("organization:" + orgA.getAlias());
        loginPage.open(bc.consumerRealmName());
        org.keycloak.testsuite.Assert.assertFalse(loginPage.isPasswordInputPresent());
        org.keycloak.testsuite.Assert.assertTrue(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        org.keycloak.testsuite.Assert.assertFalse(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));

        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.clientId("broker-app");
        String orgScope = "organization:" + orgB.getAlias();
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        org.keycloak.testsuite.Assert.assertFalse(loginPage.isPasswordInputPresent());
        org.keycloak.testsuite.Assert.assertTrue(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));
        org.keycloak.testsuite.Assert.assertFalse(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        loginPage.loginUsername(member.getEmail());
        org.keycloak.testsuite.Assert.assertTrue(loginPage.isPasswordInputPresent());
        org.keycloak.testsuite.Assert.assertTrue(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));
        org.keycloak.testsuite.Assert.assertFalse(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        loginPage.login(memberPassword);
        assertScopeAndClaims(orgScope, orgB);
    }

    @Test
    public void testOrganizationScopeMapsAllOrganizations() {
        OrganizationRepresentation orgA = createOrganization("orga", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        // resolve organization based on the organization scope value
        oauth.clientId("broker-app");
        oauth.scope("organization:" + orgA.getAlias());
        loginPage.open(bc.consumerRealmName());
        org.keycloak.testsuite.Assert.assertFalse(loginPage.isPasswordInputPresent());
        org.keycloak.testsuite.Assert.assertTrue(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        org.keycloak.testsuite.Assert.assertFalse(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));

        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.clientId("broker-app");
        String orgScope = "organization:*";
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        Map<String, Object> organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        assertThat(organizations.containsKey(orgA.getAlias()), is(true));
        assertThat(organizations.containsKey(orgB.getAlias()), is(true));
        assertThat(response.getRefreshToken(), notNullValue());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString(orgScope));
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(orgScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        assertThat(organizations.containsKey(orgA.getAlias()), is(true));
        assertThat(organizations.containsKey(orgB.getAlias()), is(true));
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString(orgScope));
    }

    @Test
    public void testOrganizationScopeAnyMapsSingleOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());

        // resolve organization based on the organization scope value
        oauth.clientId("broker-app");
        String orgScope = "organization";
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);

        assertScopeAndClaims(orgScope, orgA);
    }

    @Test
    public void testOrganizationScopeAnyAskUserToSelectOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.clientId("broker-app");
        oauth.scope("organization");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        assertTrue(selectOrganizationPage.isCurrent());
        assertFalse(driver.getPageSource().contains("kc-select-try-another-way-form"));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.login(memberPassword);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        // for now, return the organization scope in the response and access token even though no organization is mapped into the token
        // once we support the user to select an organization, the selected organization will be mapped
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        Map<String, Object> organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(organizations.containsKey(orgA.getAlias()), is(false));
        assertThat(organizations.containsKey(orgB.getAlias()), is(true));
    }

    @Test
    public void testRefreshTokenWithAllOrganizationsAskingForSpecificOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.clientId("broker-app");
        String orgScope = "organization:*";
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        Map<String, Object> organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        orgScope = "organization:orga";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(orgScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.containsKey(orgA.getAlias()), is(true));
    }

    @Test
    public void testRefreshTokenWithSingleOrganizationsAskingAllOrganizations() {
        OrganizationRepresentation orgA = createOrganization("orga", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.clientId("broker-app");
        String originalScope = "organization:orga";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        Map<String, Object> organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.containsKey(orgA.getAlias()), is(true));
        orgScope = "organization:*";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(originalScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(originalScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.containsKey(orgA.getAlias()), is(true));
    }

    @Test
    public void testRefreshTokenWithSingleOrganizationsAskingDifferentOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.clientId("broker-app");
        String originalScope = "organization:orga";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        loginPage.login(memberPassword);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        Map<String, Object> organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.containsKey(orgA.getAlias()), is(true));
        orgScope = "organization:orgb";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), not(containsString(originalScope)));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), not(containsString(orgScope)));
        assertThat(accessToken.getScope(), not(containsString(originalScope)));
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    @Test
    public void testRefreshTokenScopeAnyAskingAllOrganizations() {
        OrganizationRepresentation orgA = createOrganization("orga", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.clientId("broker-app");
        String originalScope = "organization";
        oauth.scope(originalScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        assertTrue(selectOrganizationPage.isCurrent());
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.login(memberPassword);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        // for now, return the organization scope in the response and access token even though no organization is mapped into the token
        // once we support the user to select an organization, the selected organization will be mapped
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        Map<String, Object> organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(organizations.containsKey(orgB.getAlias()), is(true));
        String orgScope = "organization:*";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(originalScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(originalScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.containsKey(orgB.getAlias()), is(true));
    }

    @Test
    public void testRefreshTokenScopeAnyAskingSingleOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        MemberRepresentation member = addMember(testRealm().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", Map.of(OrganizationModel.BROKER_PUBLIC, Boolean.TRUE.toString()));
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.clientId("broker-app");
        String originalScope = "organization";
        oauth.scope(originalScope);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(member.getEmail());
        assertTrue(selectOrganizationPage.isCurrent());
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.login(memberPassword);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        // for now, return the organization scope in the response and access token even though no organization is mapped into the token
        // once we support the user to select an organization, the selected organization will be mapped
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        Map<String, Object> organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(organizations.containsKey(orgB.getAlias()), is(true));
        String orgScope = "organization:orgb";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), not(containsString(orgScope)));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), not(containsString(orgScope)));
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    @Test
    public void testInvalidOrganizationScope() throws MalformedURLException {
        oauth.clientId("broker-app");
        oauth.scope("organization:unknown");
        oauth.realm(TEST_REALM_NAME);
        oauth.openLoginForm();
        MultivaluedHashMap<String, String> queryParams = UriUtils.decodeQueryString(new URL(driver.getCurrentUrl()).getQuery());
        assertEquals("invalid_scope", queryParams.getFirst("error"));
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

    private void assertScopeAndClaims(String orgScope, OrganizationRepresentation orgA) {
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        Map<String, Object> organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.containsKey(orgA.getAlias()), is(true));
        assertThat(response.getRefreshToken(), notNullValue());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString(orgScope));
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(orgScope));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.containsKey(orgA.getAlias()), is(true));
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString(orgScope));
    }
}
