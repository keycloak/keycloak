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

package org.keycloak.tests.organization.mapper;

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
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationMembershipMapper;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.CredentialBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.SelectOrganizationPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationOIDCProtocolMapperTest extends AbstractOrganizationTest {

    @InjectRealm(ref = "provider", config = ProviderRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectUser(ref = "alice", realmRef = "provider", config = AliceUserConf.class)
    ManagedUser aliceFromProviderRealm;

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

    @InjectPage
    SelectOrganizationPage selectOrganizationPage;

    @BeforeEach
    public void onBefore() {
        for (OrganizationRepresentation org : realm.admin().organizations().list(null, null)) {
            realm.admin().organizations().get(org.getId()).delete().close();
        }
        realm.admin().identityProviders().findAll().forEach(idp -> realm.admin().identityProviders().get(idp.getAlias()).remove());
        realm.admin().users().list().stream()
                .filter(u -> !"admin".equals(u.getUsername()))
                .forEach(u -> realm.admin().users().get(u.getId()).remove());
        createTestClients();
        setMapperConfig(ProtocolMapperUtils.MULTIVALUED, null);
        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, null);
        setMapperConfig(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, null);
    }

    @Test
    public void testPasswordGrantType() throws Exception {
        OrganizationResource orga = realm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = realm.admin().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        Assertions.assertTrue(orga.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        Assertions.assertTrue(orgb.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));

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
    @SuppressWarnings("unchecked")
    public void testMultipleOrganizationScopes() throws Exception {
        OrganizationResource orga = realm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = realm.admin().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        Assertions.assertTrue(orga.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        Assertions.assertTrue(orgb.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));

        oauth.client("test-app", "test-secret");

        // Test multiple specific organization scopes - should return both organizations
        oauth.scope("openid organization:org-a organization:org-b");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        Assertions.assertNotNull(organizations);
        Assertions.assertTrue(organizations.contains("org-a"));
        Assertions.assertTrue(organizations.contains("org-b"));

        // Test organization + specific organization scope - should still fail (mixing ANY with SPECIFIC)
        oauth.scope("openid organization organization:org-a");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());

        // Test organization + wildcard scope - should still fail (mixing ANY with ALL)
        oauth.scope("openid organization organization:*");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());

        // Test specific organization + wildcard scope - should still fail (mixing SPECIFIC with ALL)
        oauth.scope("openid organization:org-a organization:*");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());

        // Test nonexistent org alias - should fail (nonexistent alias is not a valid scope)
        oauth.scope("openid organization:org-a organization:nonexistent");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void testOrganizationNotAddedByGroupMapper() throws Exception {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        addMember(organization);
        ClientRepresentation client = realm.admin().clients().findByClientId("direct-grant").get(0);
        ClientResource clientResource = realm.admin().clients().get(client.getId());
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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        // resolve organization based on the organization scope value
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization:" + orgA.getAlias());
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        assertFalse(loginPage.isPasswordInputPresent());
        assertTrue(loginUsernamePage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        assertFalse(loginUsernamePage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));
        assertFalse(driver.driver().getPageSource().contains("Your email domain matches"));

        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization:" + orgB.getAlias();
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        assertFalse(loginPage.isPasswordInputPresent());
        assertTrue(loginUsernamePage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));
        assertFalse(loginUsernamePage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        assertTrue(loginPage.isPasswordInputPresent());
        assertTrue(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));
        assertFalse(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        assertScopeAndClaims(orgScope, orgB);
    }

    @Test
    public void testOrganizationScopeMapsAllOrganizations() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        // resolve organization based on the organization scope value
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization:" + orgA.getAlias());
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        assertFalse(loginPage.isPasswordInputPresent());
        assertTrue(loginUsernamePage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        assertFalse(loginUsernamePage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));

        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization:*";
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());

        // resolve organization based on the organization scope value
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization";
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();

        assertScopeAndClaims(orgScope, orgA);
    }

    @Test
    public void testOrganizationScopeAnyAskUserToSelectOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization");
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.assertCurrent();
        assertFalse(driver.driver().getPageSource().contains("kc-select-try-another-way-form"));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(organizations.contains(orgA.getAlias()), is(false));
        assertThat(organizations.contains(orgB.getAlias()), is(true));

        realm.admin().users().get(member.getId()).logout();
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.assertCurrent();
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        orgB.setEnabled(false);
        realm.admin().organizations().get(orgB.getId()).update(orgB).close();
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.assertCurrent();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString("organization"));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        assertThat(organizations.contains(orgB.getAlias()), is(false));
    }

    @Test
    public void testOrganizationScopeSelectDisabledOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization");
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.assertCurrent();
        assertFalse(driver.driver().getPageSource().contains("kc-select-try-another-way-form"));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(organizations.contains(orgA.getAlias()), is(false));
        assertThat(organizations.contains(orgB.getAlias()), is(true));

        realm.admin().users().get(member.getId()).logout();
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.assertCurrent();
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        orgB.setEnabled(false);
        realm.admin().organizations().get(orgB.getId()).update(orgB).close();
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.assertCurrent();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString("organization"));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        assertThat(organizations.contains(orgB.getAlias()), is(false));
    }

    @Test
    public void testOrganizationScopeSpecifyDisabledOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization");
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.assertCurrent();
        assertFalse(driver.driver().getPageSource().contains("kc-select-try-another-way-form"));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(organizations.contains(orgA.getAlias()), is(false));
        assertThat(organizations.contains(orgB.getAlias()), is(true));

        realm.admin().users().get(member.getId()).logout();
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.assertCurrent();
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        orgB.setEnabled(false);
        realm.admin().organizations().get(orgB.getId()).update(orgB).close();
        oauth.scope("organization:" + orgB.getAlias());
        oauth.openLoginForm();
        assertTrue(driver.getCurrentUrl().contains("Invalid+scopes%3A+openid+organization"));

        oauth.scope("organization:" + orgA.getAlias());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString("organization"));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        assertThat(organizations.contains(orgB.getAlias()), is(false));
        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        orgA.setEnabled(false);
        realm.admin().organizations().get(orgA.getId()).update(orgA).close();
        oauth.openLoginForm();
        assertTrue(driver.getCurrentUrl().contains("Invalid+scopes%3A+openid+organization"));

        oauth.scope("");
        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), not(containsString("organization")));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    @Test
    public void testMultipleTabsTrackingDifferentOrganizationSelectionHoldAcrossTokenRefresh() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization");

        var tabUtil = driver.tabs();
        //first tab - select orgA
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.selectOrganization(orgA.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant(oauth);
        assertThat(response.getScope(), containsString("organization"));
        String tab1RefreshToken = response.getRefreshToken();

        //second tab - select orgB
        tabUtil.newTab(oauth.loginForm().build());
        assertThat(tabUtil.getCountOfTabs(), is(2));
        selectOrganizationPage.assertCurrent();
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        response = assertSuccessfulCodeGrant(oauth);
        assertThat(response.getScope(), containsString("organization"));
        String tab2RefreshToken = response.getRefreshToken();

        //refresh first tab - ensure still orgA
        tabUtil.switchToTab(0);
        response = oauth.doRefreshTokenRequest(tab1RefreshToken);
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));

        //refresh second tab - ensure still orgB
        tabUtil.switchToTab(1);
        response = oauth.doRefreshTokenRequest(tab2RefreshToken);
        assertThat(response.getScope(), containsString("organization"));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgB.getAlias()), is(true));
    }

    @Test
    public void testUserInfoEndpoint() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization");

        var tabUtil = driver.tabs();
        //first tab - select orgA
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.selectOrganization(orgA.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant(oauth);
        assertThat(response.getScope(), containsString("organization"));
        String tab1AccessToken = response.getAccessToken();

        //second tab - select orgB
        tabUtil.newTab(oauth.loginForm().build());
        assertThat(tabUtil.getCountOfTabs(), is(2));
        selectOrganizationPage.assertCurrent();
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        response = assertSuccessfulCodeGrant(oauth);
        assertThat(response.getScope(), containsString("organization"));
        String tab2AccessToken = response.getAccessToken();

        UserInfoResponse userInfoResponse = oauth.userInfoRequest(tab1AccessToken).send();
        UserInfo userInfo = userInfoResponse.getUserInfo();
        List<String> organizations = (List<String>) userInfo.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));

        userInfoResponse = oauth.userInfoRequest(tab2AccessToken).send();
        userInfo = userInfoResponse.getUserInfo();
        organizations = (List<String>) userInfo.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgB.getAlias()), is(true));
    }

    @Test
    public void testIntrospectionEndpoint() throws Exception {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization");

        var tabUtil = driver.tabs();
        //first tab - select orgA
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.selectOrganization(orgA.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant(oauth);
        assertThat(response.getScope(), containsString("organization"));
        String tab1AccessToken = response.getAccessToken();

        //second tab - select orgB
        tabUtil.newTab(oauth.loginForm().build());
        assertThat(tabUtil.getCountOfTabs(), is(2));
        selectOrganizationPage.assertCurrent();
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        response = assertSuccessfulCodeGrant(oauth);
        assertThat(response.getScope(), containsString("organization"));
        String tab2AccessToken = response.getAccessToken();

        IntrospectionResponse introspectionResponse = oauth.introspectionRequest(tab1AccessToken).send();
        TokenMetadataRepresentation metadata = introspectionResponse.asTokenMetadata();
        List<String> organizations = (List<String>) metadata.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));

        introspectionResponse = oauth.introspectionRequest(tab2AccessToken).send();
        metadata = introspectionResponse.asTokenMetadata();
        organizations = (List<String>) metadata.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgB.getAlias()), is(true));
    }

    @Test
    public void testRefreshTokenWithAllOrganizationsAskingForSpecificOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization:*";
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));

        //previous:(ALL) -> current:(SINGLE:orga) == SINGLE:orga
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
    public void testRefreshTokenWithAllOrganizationsAskingForAny() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization:*";
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));

        //previous:(ALL) -> current:(ANY) == not allowed
        orgScope = "organization";
        oauth.scope(orgScope).openid(false);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals("ANY organization scope is not allowed in this context", response.getError());
    }

    @Test
    public void testRefreshTokenWithSingleOrganizationsAskingAllOrganizations() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization:orga";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));

        //previous:(SINGLE:orga) -> current:(ALL) == SINGLE:orga
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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization:orga";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgA.getAlias()), is(true));

        //previous:(SINGLE:orga) -> current:(SINGLE:orgb) == not allowed
        orgScope = "organization:orgb";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertResponseMissingOrganizationScopeAndClaims(response);
    }

    @Test
    public void testRefreshTokenScopeWithOrganizationSelectionAskingForSameOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization";
        oauth.scope(originalScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.assertCurrent();
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString("organization"));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertEquals( 1, organizations.toArray().length);
        assertThat(organizations.contains(orgB.getAlias()), is(true));
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString("organization"));

        //previous:(ANY -> SINGLE:orgb) -> current:(SINGLE:orgb) == SINGLE:orgb -> cannot change user selection
        oauth.scope("organization:orgb");
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), not(containsString("organization")));

        //previous:(ANY -> ALL) -> return the selected org
        oauth.scope("organization:*");
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), containsString("organization"));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString("organization"));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertEquals( 1, organizations.toArray().length);
        assertThat(organizations.contains(orgB.getAlias()), is(true));
        assertThat(organizations.contains(orgA.getAlias()), is(false));
    }

    @Test
    public void testRefreshTokenScopeWithOrganizationSelectionAskingForDifferentOrganization() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization";
        oauth.scope(originalScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.assertCurrent();
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString("organization"));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertEquals( 1, organizations.toArray().length);
        assertThat(organizations.contains(orgB.getAlias()), is(true));
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString("organization"));

        //previous:(ANY -> SINGLE:orgb) -> current:(SINGLE:orga) == not allowed
        String orgScope = "organization:orga";
        oauth.scope(orgScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertResponseMissingOrganizationScopeAndClaims(response);
    }

    @Test
    public void testRefreshTokenScopeWithOrganizationSelectionAskingForAll() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization";
        oauth.scope(originalScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.assertCurrent();
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgA.getAlias()));
        assertTrue(selectOrganizationPage.isOrganizationButtonPresent(orgB.getAlias()));
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString("organization"));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertEquals( 1, organizations.toArray().length);
        assertThat(organizations.contains(orgB.getAlias()), is(true));
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString("organization"));

        //previous:(ANY -> SINGLE:orgb) -> current:(ALL) == SINGLE:orgb
        String allOrgsScope = "organization:*";
        oauth.scope(allOrgsScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), not(containsString(allOrgsScope)));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString("organization"));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertEquals( 1, organizations.toArray().length);
        assertThat(organizations.contains(orgB.getAlias()), is(true));
    }

    @Test
    public void testRefreshTokenScopeWithSingleOrganizationAskingForAll() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization:" + orgA.getAlias();
        oauth.scope(originalScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(originalScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(originalScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertEquals( 1, organizations.toArray().length);
        assertThat(organizations.contains(orgA.getAlias()), is(true));
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString(originalScope));

        //previous:(SINGLE:orga) -> current:(ALL) == SINGLE:orga
        String allOrgsScope = "organization:*";
        oauth.scope(allOrgsScope);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), not(containsString(allOrgsScope)));
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(originalScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertEquals( 1, organizations.toArray().length);
        assertThat(organizations.contains(orgA.getAlias()), is(true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPasswordGrantWithAllOrganizationsAndRefresh() throws Exception {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(member.getEmail(), memberPassword);
        assertThat(response.getScope(), containsString("organization"));

        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations, containsInAnyOrder("orga", "orgb"));

        // refresh token and verify same organizations are resolved
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), containsString("organization"));
        accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations, containsInAnyOrder("orga", "orgb"));

        // refresh again to verify subsequent refreshes also work
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getScope(), containsString("organization"));
        accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations, containsInAnyOrder("orga", "orgb"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMultipleTabsWithMixedScopeFormats() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");

        var tabUtil = driver.tabs();
        // first tab - organization:* (all orgs)
        oauth.scope("organization:*");
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant(oauth);
        assertThat(response.getScope(), containsString("organization:*"));
        String tab1RefreshToken = response.getRefreshToken();
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        assertThat(organizations, containsInAnyOrder(orgA.getAlias(), orgB.getAlias()));

        // second tab - organization (ANY, select orgB)
        oauth.scope("organization");
        tabUtil.newTab(oauth.loginForm().build());
        assertThat(tabUtil.getCountOfTabs(), is(2));
        selectOrganizationPage.assertCurrent();
        selectOrganizationPage.selectOrganization(orgB.getAlias());
        response = assertSuccessfulCodeGrant(oauth);
        assertThat(response.getScope(), containsString("organization"));
        String tab2RefreshToken = response.getRefreshToken();
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgB.getAlias()), is(true));

        // refresh first tab - should still have all organizations (not contaminated by tab 2's org selection)
        tabUtil.switchToTab(0);
        oauth.scope(null);
        response = oauth.doRefreshTokenRequest(tab1RefreshToken);
        assertThat(response.getScope(), containsString("organization:*"));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        assertThat(organizations, containsInAnyOrder(orgA.getAlias(), orgB.getAlias()));

        // refresh second tab - should still have only orgB
        tabUtil.switchToTab(1);
        oauth.scope(null);
        response = oauth.doRefreshTokenRequest(tab2RefreshToken);
        assertThat(response.getScope(), containsString("organization"));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.contains(orgB.getAlias()), is(true));

        // refresh second tab changing scopes to ask for orgA, should not be allowed since the user selection in
        // that tab was orgB
        tabUtil.switchToTab(1);
        oauth.scope("organization:" + orgA.getAlias());
        response = oauth.doRefreshTokenRequest(tab2RefreshToken);
        assertThat(response.getScope(), not(containsString("organization")));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations, is(nullValue()));

        // refresh first tab changing scopes ALL -> SINGLE
        tabUtil.switchToTab(0);
        oauth.scope("organization:" + orgA.getAlias());
        response = oauth.doRefreshTokenRequest(tab1RefreshToken);
        assertThat(response.getScope(), containsString("organization:" + orgA.getAlias()));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
        assertThat(organizations, containsInAnyOrder(orgA.getAlias()));

        // try to refresh first tab changing scopes SINGLE -> ANY, not allowed
        tabUtil.switchToTab(0);
        oauth.scope("organization");
        response = oauth.doRefreshTokenRequest(tab1RefreshToken);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals("ANY organization scope is not allowed in this context", response.getError());

        // try to refresh first tab changing scopes SINGLE -> ALL
        tabUtil.switchToTab(0);
        oauth.scope("organization:*");
        response = oauth.doRefreshTokenRequest(tab1RefreshToken);
        assertThat(response.getScope(), containsString("organization:*"));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        assertThat(organizations, containsInAnyOrder(orgA.getAlias(), orgB.getAlias()));

        // try to refresh second tab changing scopes ANY -> ALL
        tabUtil.switchToTab(1);
        oauth.scope("organization:*");
        response = oauth.doRefreshTokenRequest(tab1RefreshToken);
        assertThat(response.getScope(), containsString("organization:*"));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        assertThat(organizations, containsInAnyOrder(orgA.getAlias(), orgB.getAlias()));

        // try to refresh second tab changing scopes ALL -> ANY
        tabUtil.switchToTab(1);
        oauth.scope("organization");
        response = oauth.doRefreshTokenRequest(tab1RefreshToken);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals("ANY organization scope is not allowed in this context", response.getError());

        // refresh second tab resetting scopes so that the scopes from the last successful refresh are respected
        tabUtil.switchToTab(1);
        oauth.scope(null);
        response = oauth.doRefreshTokenRequest(tab1RefreshToken);
        assertThat(response.getScope(), containsString("organization:*"));
        accessToken = oauth.verifyToken(response.getAccessToken());
        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(2));
        assertThat(organizations, containsInAnyOrder(orgA.getAlias(), orgB.getAlias()));
    }

    @Test
    public void testIncludeOrganizationAttributes() throws Exception {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource organization = realm.admin().organizations().get(orgRep.getId());
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
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION), is(orgRep.getAlias()));

        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ATTRIBUTES, Boolean.FALSE.toString());
        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, "JSON");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Map<String, List<String>>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(organizationName));
        assertThat(organizations.get(organizationName).keySet().isEmpty(), is(true));
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION), is(orgRep.getAlias()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIncludeOrganizationId() throws Exception {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource organization = realm.admin().organizations().get(orgRep.getId());
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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

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
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        MultivaluedHashMap<String, String> queryParams = UriUtils.decodeQueryString(new URL(driver.getCurrentUrl()).getQuery());
        assertEquals("invalid_scope", queryParams.getFirst("error"));
    }

    @Test
    public void testAuthenticatingUsingBroker() {
        String idpAlias = organizationName + "-identity-provider";
        OrganizationRepresentation orgRep = createOrganization(realm, organizationName,
                createRealOrgBroker(idpAlias, providerRealm), organizationName + ".org");
        OrganizationResource organization = realm.admin().organizations().get(orgRep.getId());

        oauth.scope(OAuth2Constants.ORGANIZATION);
        assertBrokerRegistration(organization, aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getEmail(),
                oauth, loginUsernamePage, loginPage, loginUpdateProfilePage, providerRealm);

        UserRepresentation user = getUserRepresentation(aliceFromProviderRealm.getEmail());
        List<FederatedIdentityRepresentation> federatedIdentities = realm.admin().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(idpAlias, federatedIdentities.get(0).getIdentityProvider());
    }

    @Test
    public void testMapDifferentOrganizationWhenReAuthenticating() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization:orga";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
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
        oauth.realm(realm.getName());
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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.selectOrganization(orgA.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
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
        oauth.realm(realm.getName());
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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        ClientRepresentation client = realm.admin().clients().findByClientId("broker-app").get(0);
        client.setId(null);
        client.setClientId("broker-app2");
        realm.admin().clients().create(client).close();
        oauth.client("broker-app", "broker-app-secret");
        String originalScope = "organization:orga";
        String orgScope = originalScope;
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
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
        oauth.realm(realm.getName());
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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        ClientRepresentation client = realm.admin().clients().findByClientId("broker-app").get(0);
        client.setId(null);
        client.setClientId("broker-app2");
        realm.admin().clients().create(client).close();
        client.setProtocolMappers(null);
        realm.admin().clients().create(client).close();
        oauth.client("broker-app", "broker-app-secret");
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        realm.admin().organizations().get(orgA.getId()).members().member(member.getId()).delete().close();
        ClientRepresentation client = realm.admin().clients().findByClientId("broker-app").get(0);
        client.setId(null);
        client.setClientId("broker-app2");
        realm.admin().clients().create(client).close();
        oauth.client("broker-app", "broker-app-secret");
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization";
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.selectOrganization(orgA.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        AccessTokenResponse response = assertSuccessfulCodeGrant();
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        oauth.scope("openid");
        oauth.openLoginForm();
        response = assertSuccessfulCodeGrant();
        assertResponseMissingOrganizationScopeAndClaims(response);
    }

    @Test
    public void testSelectDifferentOrganizationWhenReAuthenticating() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();
        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization";
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.selectOrganization(orgA.getAlias());
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        ClientScopeRepresentation orgScope = realm.admin().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();
        ClientScopeResource orgScopeResource = realm.admin().clientScopes().get(orgScope.getId());
        ProtocolMapperRepresentation orgMapper = orgScopeResource.getProtocolMappers().getMappers().stream()
                .filter(m -> OIDCLoginProtocolFactory.ORGANIZATION.equals(m.getName()))
                .findAny()
                .orElseThrow();
        orgMapper.setId(null);
        orgScope.setProtocolMappers(List.of(orgMapper));
        orgScope.setId(null);
        orgScope.setName("org");
        String createdId = ApiUtil.getCreatedId(realm.admin().clientScopes().create(orgScope));
        realm.admin().addDefaultDefaultClientScope(createdId);
        ClientRepresentation client = realm.admin().clients().findByClientId("broker-app").get(0);
        realm.admin().clients().get(client.getId()).addDefaultClientScope(createdId);
        realm.cleanup().add(r -> r.clientScopes().get(createdId).remove());

        oauth.client("broker-app", "broker-app-secret");
        String scopeName = "org:" + orgA.getAlias();
        oauth.scope(scopeName);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();

        assertScopeAndClaims(scopeName, orgA);
    }

    @Test
    public void testCustomOrganizationScopeNameAllOrganizations() {
        OrganizationResource orga = realm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = realm.admin().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        Assertions.assertTrue(orga.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        Assertions.assertTrue(orgb.members().list(-1, -1).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));

        ClientScopeRepresentation orgScope = realm.admin().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();
        ClientScopeResource orgScopeResource = realm.admin().clientScopes().get(orgScope.getId());
        ProtocolMapperRepresentation orgMapper = orgScopeResource.getProtocolMappers().getMappers().stream()
                .filter(m -> OIDCLoginProtocolFactory.ORGANIZATION.equals(m.getName()))
                .findAny()
                .orElseThrow();
        orgMapper.setId(null);
        orgScope.setProtocolMappers(List.of(orgMapper));
        orgScope.setId(null);
        orgScope.setName("org");
        String createdId = ApiUtil.getCreatedId(realm.admin().clientScopes().create(orgScope));
        realm.admin().addDefaultDefaultClientScope(createdId);
        ClientRepresentation client = realm.admin().clients().findByClientId("broker-app").get(0);
        realm.admin().clients().get(client.getId()).addDefaultClientScope(createdId);
        realm.cleanup().add(r -> r.clientScopes().get(createdId).remove());

        oauth.client("broker-app", "broker-app-secret");
        String scopeName = "org:*";
        oauth.scope(scopeName);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();

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
    @SuppressWarnings("unchecked")
    public void testCustomOrganizationScopeNameMultipleSpecific() throws Exception {
        OrganizationResource orga = realm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgb = realm.admin().organizations().get(createOrganization("org-b").getId());

        addMember(orga);

        UserRepresentation member = getUserRepresentation(memberEmail);

        orgb.members().addMember(member.getId()).close();

        assertTrue(orga.members().list(null, null).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));
        assertTrue(orgb.members().list(null, null).stream().map(UserRepresentation::getId).anyMatch(member.getId()::equals));

        // Create custom "org" scope with OrganizationMembershipMapper
        ClientScopeRepresentation orgScope = realm.admin().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();
        ClientScopeResource orgScopeResource = realm.admin().clientScopes().get(orgScope.getId());
        ProtocolMapperRepresentation orgMapper = orgScopeResource.getProtocolMappers().getMappers().stream()
                .filter(m -> OIDCLoginProtocolFactory.ORGANIZATION.equals(m.getName()))
                .findAny()
                .orElseThrow();
        orgMapper.setId(null);
        orgScope.setProtocolMappers(List.of(orgMapper));
        orgScope.setId(null);
        orgScope.setName("org");
        String createdId = ApiUtil.getCreatedId(realm.admin().clientScopes().create(orgScope));
        realm.admin().addDefaultDefaultClientScope(createdId);
        ClientRepresentation testApp = realm.admin().clients().findByClientId("test-app").get(0);
        realm.admin().clients().get(testApp.getId()).addDefaultClientScope(createdId);
        realm.cleanup().add(r -> r.clientScopes().get(createdId).remove());

        oauth.client("test-app", "test-secret");

        // Test multiple specific organization scopes with custom scope name - should return both organizations
        oauth.scope("openid org:org-a org:org-b");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getStatusCode(), is(Response.Status.OK.getStatusCode()));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations, hasSize(2));
        assertThat(organizations, hasItem("org-a"));
        assertThat(organizations, hasItem("org-b"));

        // Test mixing custom ANY + SPECIFIC scope - should fail
        oauth.scope("openid org org:org-a");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        // Test mixing custom SPECIFIC + ALL scope - should fail
        oauth.scope("openid org:org-a org:*");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testClaimNotMappedIfUserNotMemberWhenDefaultClientScope() {
        OrganizationRepresentation orgARep = createOrganization("orga", true);
        OrganizationResource orgA = realm.admin().organizations().get(orgARep.getId());
        MemberRepresentation member = addMember(orgA, "member@" + orgARep.getDomains().iterator().next().getName());
        orgA.members().member(member.getId()).delete().close();

        ClientRepresentation clientRep = realm.admin().clients().findByClientId("broker-app").get(0);
        ClientResource client = realm.admin().clients().get(clientRep.getId());
        ClientScopeRepresentation orgScopeRep = client.getOptionalClientScopes().stream().filter(scope -> "organization".equals(scope.getName())).findAny().orElse(null);
        client.removeOptionalClientScope(orgScopeRep.getId());
        client.addDefaultClientScope(orgScopeRep.getId());
        realm.cleanup().add(r -> {
            ClientRepresentation cr = r.clients().findByClientId("broker-app").get(0);
            ClientResource crc = r.clients().get(cr.getId());
            crc.removeDefaultClientScope(orgScopeRep.getId());
            crc.addOptionalClientScope(orgScopeRep.getId());
        });
        // resolve organization based on the organization scope value
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope(null);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();

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
        MemberRepresentation member = addMember(realm.admin().organizations().get(orgA.getId()), "member@" + orgA.getDomains().iterator().next().getName());
        OrganizationRepresentation orgB = createOrganization("orgb", true);
        realm.admin().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        ClientRepresentation clientRep = realm.admin().clients().findByClientId("broker-app").get(0);
        ClientResource client = realm.admin().clients().get(clientRep.getId());
        ClientScopeRepresentation orgScopeRep = client.getOptionalClientScopes().stream().filter(scope -> "organization".equals(scope.getName())).findAny().orElse(null);
        orgScopeRep.setAttributes(Map.of(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "false"));
        realm.cleanup().add(r -> {
            orgScopeRep.setAttributes(Map.of(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "true"));
            r.clientScopes().get(orgScopeRep.getId()).update(orgScopeRep);
        });
        realm.admin().clientScopes().get(orgScopeRep.getId()).update(orgScopeRep);

        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(member.getEmail(), memberPassword);
        assertThat(response.getScope(), not(containsString("organization")));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        List<String> organization = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organization, containsInAnyOrder("orga", "orgb"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOrganizationAttributeNamedIdIsOverriddenByOrganizationId() throws Exception {
        // When an organization has a custom attribute called "id", the organization ID should override it in tokens
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource organization = realm.admin().organizations().get(orgRep.getId());
        addMember(organization);

        // Add a custom attribute named "id" to the organization
        orgRep.singleAttribute("id", "custom-id-value");

        try (Response response = organization.update(orgRep)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Verify that organization ID overrides custom "id" attribute in tokens
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ID, Boolean.TRUE.toString());
        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_ATTRIBUTES, Boolean.TRUE.toString());

        oauth.client("direct-grant", "password");
        oauth.scope("openid organization");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getScope(), containsString("organization"));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        Map<String, Map<String, String>> organizations = (Map<String, Map<String, String>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(organizationName));
        Map<String, String> orgClaims = organizations.get(organizationName);

        // The "id" attribute should contain the organization ID, not the custom value
        assertThat(orgClaims.get("id"), equalTo(orgRep.getId()));
        assertThat(orgClaims.get("id"), not(equalTo("custom-id-value")));
    }

    @Test
    public void testDomainClaim() {
        OrganizationRepresentation orgA = createOrganization("orga", true);
        OrganizationResource organization = realm.admin().organizations().get(orgA.getId());
        OrganizationDomainRepresentation domain = orgA.getDomains().iterator().next();
        MemberRepresentation member = addMember(organization, "member@" + domain.getName());

        domain.setName("*." + domain.getName());
        organization.update(orgA).close();

        setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_DOMAIN, Boolean.TRUE.toString());
        setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, "JSON");
        realm.cleanup().add(r -> {
            setMapperConfig(OrganizationMembershipMapper.ADD_ORGANIZATION_DOMAIN, Boolean.FALSE.toString());
            setMapperConfig(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        });
        oauth.client("broker-app", "broker-app-secret");
        String orgScope = "organization";
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        Map<String, Map<String, String>> organizations = (Map<String, Map<String, String>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(orgA.getAlias()));
        Map<String, String> orgClaims = organizations.get(orgA.getAlias());
        assertThat(orgClaims.get("domain"), is(domain.getName()));

        String memberEmailDomain = Organizations.getEmailDomain(this.memberEmail);
        memberEmailDomain = "sub." + memberEmailDomain;
        member.setEmail("test@" + memberEmailDomain);
        realm.admin().users().get(member.getId()).update(new UserRepresentation(member));
        orgA.addDomain(new OrganizationDomainRepresentation(memberEmailDomain));
        organization.update(orgA).close();

        realm.admin().users().get(member.getId()).logout();
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        code = oauth.parseLoginResponse().getCode();
        response = oauth.doAccessTokenRequest(code);
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Map<String, String>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(orgA.getAlias()));
        orgClaims = organizations.get(orgA.getAlias());
        assertThat(orgClaims.get("domain"), is(memberEmailDomain));

        memberEmailDomain = Organizations.getEmailDomain(member.getEmail());
        member.setEmail("test@deep." + memberEmailDomain);
        realm.admin().users().get(member.getId()).update(new UserRepresentation(member));
        orgA.addDomain(new OrganizationDomainRepresentation("*." + memberEmailDomain));
        organization.update(orgA).close();

        realm.admin().users().get(member.getId()).logout();
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
        code = oauth.parseLoginResponse().getCode();
        response = oauth.doAccessTokenRequest(code);
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Map<String, String>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(orgA.getAlias()));
        orgClaims = organizations.get(orgA.getAlias());
        assertThat(orgClaims.get("domain"), is("*." + memberEmailDomain));

        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        organizations = (Map<String, Map<String, String>>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.keySet(), hasItem(orgA.getAlias()));
        orgClaims = organizations.get(orgA.getAlias());
        assertThat(orgClaims.get("domain"), is("*." + memberEmailDomain));
    }

    private AccessTokenResponse assertSuccessfulCodeGrant() {
        return assertSuccessfulCodeGrant(oauth);
    }

    private AccessTokenResponse assertSuccessfulCodeGrant(OAuthClient oauth) {
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

    @Test
    @SuppressWarnings("unchecked")
    public void testCustomClaimName() throws Exception {
        OrganizationResource org = realm.admin().organizations().get(createOrganization("acme").getId());
        addMember(org);

        setMapperConfig(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "my_orgs");

        oauth.client("direct-grant", "password");
        oauth.scope("openid organization");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);

        AccessToken token = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();

        assertThat(token.getOtherClaims(), not(hasKey(OAuth2Constants.ORGANIZATION)));
        assertThat(token.getOtherClaims(), hasKey("my_orgs"));
        assertThat(token.getOtherClaims().get("my_orgs"), notNullValue());
    }

    @Test
    public void testCustomOrganizationClaimDoesNotTriggerOrganizationValidation() throws Exception {
        // Create a plain user – not a member of any organization.
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setEmail("custom-org-claim@example.com");
        user.setUsername("custom-org-claim@example.com");

        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }
        realm.admin().users().get(user.getId()).resetPassword(CredentialBuilder.password(memberPassword).build());
        realm.cleanup().add(r -> r.users().get(user.getId()).remove());

        realm.updateWithCleanup(r -> r.organizationsEnabled(false));

        // Add a hardcoded-claim mapper that emits an "organization" claim whose value is not a real org alias.
        // This simulates a customer using their own "organization" attribute unrelated to Keycloak Organizations.
        ClientRepresentation clientRep = realm.admin().clients().findByClientId("direct-grant").get(0);
        ClientResource clientResource = realm.admin().clients().get(clientRep.getId());

        createMapperAndAddCleanup(clientResource, createHardcodedClaim(
                "custom-org-mapper", OAuth2Constants.ORGANIZATION, "my-company", "String", true, true, true));
        createMapperAndAddCleanup(clientResource, ModelToRepresentation.toRepresentation(
                AudienceProtocolMapper.createClaimMapper(
                        "audience-mapper-test",
                        "direct-grant",
                        null,
                        true,
                        false,
                        false
                )
        ));

        oauth.client("direct-grant", "password");
        oauth.scope("openid");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getEmail(), memberPassword);
        assertThat("Token request must succeed", tokenResponse.getStatusCode(), is(200));

        // Confirm the custom "organization" claim is present in the access token.
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertThat(accessToken.getOtherClaims(), hasKey(OAuth2Constants.ORGANIZATION));
        assertThat(accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION), is("my-company"));

        // Userinfo endpoint must not fail with invalid_grant.
        UserInfoResponse userInfoResponse = oauth.userInfoRequest(tokenResponse.getAccessToken()).send();
        assertThat("Userinfo must return 200 for a token carrying a custom 'organization' claim",
                userInfoResponse.getStatusCode(), is(200));
        assertThat(userInfoResponse.getUserInfo().getOtherClaims(), hasKey(OAuth2Constants.ORGANIZATION));

        // Introspection endpoint must not fail with invalid_grant.
        IntrospectionResponse introspectionResponse = oauth.introspectionRequest(tokenResponse.getAccessToken()).send();
        assertThat("Introspection must return an active token carrying a custom 'organization' claim",
                introspectionResponse.asTokenMetadata().isActive(), is(true));
        assertThat(introspectionResponse.asTokenMetadata().getOtherClaims(), hasKey(OAuth2Constants.ORGANIZATION));

        // Refresh token endpoint must not fail with invalid_grant.
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertThat("Refresh must succeed for a session whose token carries a custom 'organization' claim",
                refreshResponse.getStatusCode(), is(200));
        accessToken = oauth.verifyToken(refreshResponse.getAccessToken());
        assertThat(accessToken.getOtherClaims(), hasKey(OAuth2Constants.ORGANIZATION));
    }

    private void assertResponseMissingOrganizationScopeAndClaims(AccessTokenResponse response) {
        assertThat(response.getScope(), not(containsString("organization")));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), not(containsString("organization")));
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    @Test
    public void testOrganizationClaimNotIssuedWhenOrganizationsDisabledCodeGrant() throws Exception {
        OrganizationResource orgA = realm.admin().organizations().get(createOrganization("orga").getId());
        addMember(orgA);

        // verify organization claim IS present when organizations are enabled (via password grant)
        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getStatusCode(), is(200));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        // now disable organizations on the realm and test the code grant flow
        // when organizations are disabled, the org authenticator skips (calls attempted()),
        // so the standard username/password login form is shown
        realm.updateWithCleanup(r -> r.organizationsEnabled(false));

        oauth.client("broker-app", "broker-app-secret");
        oauth.scope("organization:*");
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginPage.fillLogin(memberEmail, memberPassword);
        loginPage.submit();

        String code = oauth.parseLoginResponse().getCode();
        response = oauth.doAccessTokenRequest(code);
        assertThat(response.getStatusCode(), is(Status.OK.getStatusCode()));
        assertResponseMissingOrganizationScopeAndClaims(response);
    }

    @Test
    public void testOrganizationClaimNotIssuedWhenOrganizationsDisabledClientCredentials() throws Exception {
        OrganizationRepresentation orgA = createOrganization("orga");

        // create a client with service accounts enabled
        ClientRepresentation serviceClient = ClientBuilder.create()
                .clientId("service-account-org-test")
                .secret("secret")
                .serviceAccountsEnabled(true)
                .build();
        realm.admin().clients().create(serviceClient).close();
        ClientRepresentation createdClient = realm.admin().clients().findByClientId("service-account-org-test").get(0);
        realm.cleanup().add(r -> r.clients().get(createdClient.getId()).remove());

        // add the organization scope as optional client scope to the service account client
        ClientScopeRepresentation orgScope = realm.admin().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();
        realm.admin().clients().get(createdClient.getId()).addOptionalClientScope(orgScope.getId());

        // make the service account user a member of the organization
        String serviceAccountUserId = realm.admin().clients().get(createdClient.getId()).getServiceAccountUser().getId();
        realm.admin().organizations().get(orgA.getId()).members().addMember(serviceAccountUserId).close();

        // first, verify organization claim IS present when organizations are enabled
        oauth.client("service-account-org-test", "secret");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest();
        assertThat(response.getStatusCode(), is(200));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        // now disable organizations on the realm
        realm.updateWithCleanup(r -> r.organizationsEnabled(false));

        // attempt client credentials grant with organization scope — should not include organization claim
        oauth.client("service-account-org-test", "secret");
        oauth.scope("openid organization:*");
        response = oauth.doClientCredentialsGrantAccessTokenRequest();
        assertThat(response.getStatusCode(), is(200));
        assertResponseMissingOrganizationScopeAndClaims(response);
    }

    @Test
    public void testOrganizationClaimNotIssuedWhenOrganizationsDisabledPasswordGrant() throws Exception {
        OrganizationResource orgA = realm.admin().organizations().get(createOrganization("orga").getId());
        addMember(orgA);

        // verify organization claim IS present when organizations are enabled
        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getStatusCode(), is(200));
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        // now disable organizations on the realm
        realm.updateWithCleanup(r -> r.organizationsEnabled(false));

        // password grant with organization scope — should not include organization claim
        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getStatusCode(), is(200));
        assertResponseMissingOrganizationScopeAndClaims(response);
    }

    @Test
    public void testOrganizationClaimNotIssuedOnRefreshWhenOrganizationsDisabled() throws Exception {
        OrganizationResource orgA = realm.admin().organizations().get(createOrganization("orga").getId());
        addMember(orgA);

        // get a token with organization claims while orgs are enabled
        oauth.client("direct-grant", "password");
        oauth.scope("openid organization:*");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getRefreshToken(), notNullValue());
        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        String refreshToken = response.getRefreshToken();

        // disable organizations on the realm, then refresh the token
        realm.updateWithCleanup(r -> r.organizationsEnabled(false));

        // refresh should succeed but the new token should not contain organization claims
        response = oauth.doRefreshTokenRequest(refreshToken);
        assertThat(response.getStatusCode(), is(200));
        assertResponseMissingOrganizationScopeAndClaims(response);
    }

    @Test
    public void testOrganizationScopeFilteredButOtherScopesPreservedWhenOrganizationsDisabled() throws Exception {
        OrganizationResource orgA = realm.admin().organizations().get(createOrganization("orga").getId());
        addMember(orgA);

        // create a dedicated client with direct access grants and the "organization" optional scope
        ClientRepresentation testClient = ClientBuilder.create()
                .clientId("org-scope-test")
                .secret("secret")
                .directAccessGrantsEnabled()
                .build();
        realm.admin().clients().create(testClient).close();
        ClientRepresentation createdClient = realm.admin().clients().findByClientId("org-scope-test").get(0);
        realm.cleanup().add(r -> r.clients().get(createdClient.getId()).remove());

        ClientScopeRepresentation orgScope = realm.admin().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();
        realm.admin().clients().get(createdClient.getId()).addOptionalClientScope(orgScope.getId());

        // disable organizations on the realm
        realm.updateWithCleanup(r -> r.organizationsEnabled(false));

        // request the plain "organization" scope (ANY variant, resolved via allOptionalScopes)
        // alongside other standard scopes
        oauth.client("org-scope-test", "secret");
        oauth.scope("openid email profile organization");
        AccessTokenResponse response = oauth.doPasswordGrantRequest(memberEmail, memberPassword);
        assertThat(response.getStatusCode(), is(200));

        // organization scope should be stripped
        assertThat(response.getScope(), not(containsString("organization")));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));

        // but email and profile scopes should still be granted
        assertThat(response.getScope(), containsString("email"));
        assertThat(response.getScope(), containsString("profile"));
    }

    private void assertClaimNotMapped(String orgScope, OrganizationRepresentation orgARep, boolean grantScope) {
        OrganizationResource orgA = realm.admin().organizations().get(orgARep.getId());
        MemberRepresentation member = addMember(orgA, "member@" + orgARep.getDomains().iterator().next().getName());
        orgA.members().member(member.getId()).delete().close();
        oauth.client("broker-app", "broker-app-secret");
        oauth.scope(orgScope);
        oauth.realm(realm.getName());
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(member.getEmail());
        loginUsernamePage.submit();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertThat(response.getScope(), grantScope ? containsString(orgScope) : not(containsString(orgScope)));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), grantScope ? containsString(orgScope) : not(containsString(orgScope)));
        assertThat(accessToken.getOtherClaims().keySet(), not(hasItem(OAuth2Constants.ORGANIZATION)));
    }

    private void createMapperAndAddCleanup(ClientResource clientResource, ProtocolMapperRepresentation mapper) {
        String mapperId;

        try (Response response = clientResource.getProtocolMappers().createMapper(mapper)) {
            mapperId = ApiUtil.getCreatedId(response);
        }

        realm.cleanup().add(r -> {
            ClientRepresentation cr = r.clients().findByClientId(clientResource.toRepresentation().getClientId()).get(0);
            r.clients().get(cr.getId()).getProtocolMappers().delete(mapperId);
        });
    }
}
