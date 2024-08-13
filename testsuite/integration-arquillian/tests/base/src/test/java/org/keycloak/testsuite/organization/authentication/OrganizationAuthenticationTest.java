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

package org.keycloak.testsuite.organization.authentication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.OrganizationModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.broker.KcOidcBrokerConfiguration;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.OAuthClient;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationAuthenticationTest extends AbstractOrganizationTest {

    @Test
    public void testAuthenticateUnmanagedMember() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org");

        // first try to log in using only the email
        openIdentityFirstLoginPage(member.getEmail(), false, null, false, false);

        Assert.assertTrue(loginPage.isPasswordInputPresent());
        // no idp should be shown because there is only a single idp that is bound to an organization
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        // the member should be able to log in using the credentials
        loginPage.login(memberPassword);
        appPage.assertCurrent();
    }

    @Test
    public void testTryLoginWithUsernameNotAnEmail() {
        testRealm().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user", false, null, false, false);

        // check if the login page is shown
        Assert.assertTrue(loginPage.isUsernameInputPresent());
        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testDefaultAuthenticationMechanismIfNotOrganizationMember() {
        testRealm().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user@noorg.org", false, null, false, false);

        // check if the login page is shown
        Assert.assertTrue(loginPage.isUsernameInputPresent());
        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testAuthenticateUnmanagedMemberWhenProviderDisabled() throws IOException {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org");

        // first try to access login page
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        // disable the organization provider
        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm())
                .setOrganizationsEnabled(Boolean.FALSE)
                .update()) {

            // access the page again, now it should be present username and password fields
            loginPage.open(bc.consumerRealmName());

            waitForPage(driver, "sign in to", true);
            assertThat("Driver should be on the consumer realm page right now",
                    driver.getCurrentUrl(), Matchers.containsString("/auth/realms/" + bc.consumerRealmName() + "/"));
            Assert.assertTrue(loginPage.isPasswordInputPresent());
            // no idp should be shown because there is only a single idp that is bound to an organization
            Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

            // the member should be able to log in using the credentials
            loginPage.login(member.getEmail(), memberPassword);
            appPage.assertCurrent();
        }
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
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertTrue(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        Assert.assertFalse(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));

        // identity-first login will respect the organization provided in the scope even though the user email maps to a different organization
        oauth.clientId("broker-app");
        String orgScope = "organization:" + orgB.getAlias();
        oauth.scope(orgScope);
        loginPage.open(bc.consumerRealmName());
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertTrue(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));
        Assert.assertFalse(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        loginPage.loginUsername(member.getEmail());
        Assert.assertTrue(loginPage.isPasswordInputPresent());
        Assert.assertTrue(loginPage.isSocialButtonPresent(orgB.getAlias() + "-identity-provider"));
        Assert.assertFalse(loginPage.isSocialButtonPresent(orgA.getAlias() + "-identity-provider"));
        loginPage.login(memberPassword);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        assertThat(response.getScope(), containsString(orgScope));
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertThat(accessToken.getScope(), containsString(orgScope));
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));
        Map<String, Object> organizations = (Map<String, Object>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations.size(), is(1));
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
        assertThat(organizations.size(), is(1));
        assertThat(organizations.containsKey(orgB.getAlias()), is(true));
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertThat(refreshToken.getScope(), containsString(orgScope));
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
}
