/*
 *  Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.organization.admin;

import java.util.List;
import java.util.Map.Entry;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.UserBuilder;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.MatcherAssert.assertThat;

public class OrganizationThemeTest extends AbstractOrganizationTest {

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginUpdateProfilePage updateProfilePage;

    @Page
    protected AppPage appPage;

    @Before
    public void onBefore() {
        RealmResource realm = realmsResouce().realm(bc.consumerRealmName());
        RealmRepresentation rep = realm.toRepresentation();
        rep.setLoginTheme("organization");
        realm.update(rep);
    }

    @Test
    public void testOrganizationOnRegularLogin() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization("myorg", "myorg.com").getId());
        IdentityProviderRepresentation broker = organization.identityProviders().getIdentityProviders().get(0);
        broker.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        testRealm().identityProviders().get(broker.getAlias()).update(broker);
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("tom")
                .email("tom@myorg.com")
                .password("password")
                .firstName("Tom")
                .lastName("Brady")
                .build();
        try (Response resp = realmsResouce().realm(bc.consumerRealmName()).users().create(user)) {
            String userId = ApiUtil.getCreatedId(resp);
            getCleanup(bc.consumerRealmName()).addUserId(userId);
        }

        // organization available to regular login page
        loginPage.open(bc.consumerRealmName());
        Assert.assertTrue(driver.getPageSource().contains("Sign-in to the realm"));
        loginPage.loginUsername("tom@myorg.com");
        Assert.assertTrue(driver.getPageSource().contains("Sign-in to myorg organization"));
        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testOrganizationOnIdentityFirstLogin() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization("myorg", "myorg.com").getId());
        IdentityProviderRepresentation broker = organization.identityProviders().getIdentityProviders().get(0);
        broker.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        testRealm().identityProviders().get(broker.getAlias()).update(broker);

        // organization available to identity-first login page
        loginPage.open(bc.consumerRealmName());
        Assert.assertTrue(driver.getPageSource().contains("Sign-in to the realm"));
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        loginPage.loginUsername("non-user@myorg.com");
        Assert.assertTrue(driver.getPageSource().contains("Sign-in to myorg organization"));
        Assert.assertFalse(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testOrganizationOnIdPReview() {
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("tom")
                .password("password")
                .firstName("Tom")
                .lastName("Brady")
                .build();
        try (Response resp = realmsResouce().realm(bc.providerRealmName()).users().create(user)) {
            String userId = ApiUtil.getCreatedId(resp);
            getCleanup(bc.providerRealmName()).addUserId(userId);
        }
        createOrganization("myorg", "myorg.com");

        // organization available to broker review profile
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("tom@myorg.com");
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        loginPage.login(user.getUsername(), "password");
        waitForPage(driver, "update account information", false);
        Assert.assertTrue("Driver should be on the consumer realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
        Assert.assertTrue(driver.getPageSource().contains("Sign-in to myorg organization"));
    }

    @Test
    public void testOrganizationOnUpdateProfile() {
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("tom")
                .email("tom@myorg.org")
                .password("password")
                .firstName("Tom")
                .lastName("Brady")
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.name())
                .build();
        try (Response resp = testRealm().users().create(user)) {
            String userId = ApiUtil.getCreatedId(resp);
            getCleanup(bc.consumerRealmName()).addUserId(userId);
        }
        createOrganization("myorg", "myorg.com", "myorg.org");
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("tom");
        Assert.assertTrue(driver.getPageSource().contains("Sign-in to myorg organization"));
        loginPage.login("password");
        waitForPage(driver, "update account information", false);
        Assert.assertTrue("Driver should be on the consumer realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
        Assert.assertTrue(driver.getPageSource().contains("Sign-in to myorg organization"));
    }

    @Test
    public void testOrganizationAttributes() {
        OrganizationRepresentation orgRep = createOrganization("myorg", "myorg.com");
        OrganizationResource organization = testRealm().organizations().get(orgRep.getId());
        IdentityProviderRepresentation broker = organization.identityProviders().getIdentityProviders().get(0);
        broker.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        testRealm().identityProviders().get(broker.getAlias()).update(broker);

        // organization available to identity-first login page
        loginPage.open(bc.consumerRealmName());
        Assert.assertTrue(driver.getPageSource().contains("Sign-in to the realm"));
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        loginPage.loginUsername("non-user@myorg.com");
        Assert.assertTrue(driver.getPageSource().contains("Sign-in to myorg organization"));
        for (Entry<String, List<String>> attribute : orgRep.getAttributes().entrySet()) {
            assertThat(driver.getPageSource(), Matchers.containsString("The " + attribute.getKey() + " is " + String.join(", ", attribute.getValue())));
        }
        Assert.assertFalse(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testUserIsMember() {
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("tom")
                .email("tom@myorg.com")
                .password("password")
                .firstName("Tom")
                .lastName("Brady")
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.name())
                .build();
        try (Response resp = testRealm().users().create(user)) {
            String userId = ApiUtil.getCreatedId(resp);
            user.setId(userId);
            getCleanup(bc.consumerRealmName()).addUserId(userId);
        }

        OrganizationRepresentation orgRep = createOrganization("myorg", "myorg.com");
        OrganizationResource organization = testRealm().organizations().get(orgRep.getId());
        IdentityProviderRepresentation broker = organization.identityProviders().getIdentityProviders().get(0);
        broker.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        testRealm().identityProviders().get(broker.getAlias()).update(broker);
        organization.members().addMember(user.getId()).close();

        // organization available to identity-first login page
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(user.getEmail());
        Assert.assertTrue(driver.getPageSource().contains("Sign-in to myorg organization"));
        Assert.assertTrue(driver.getPageSource().contains("User is member of " + orgRep.getName()));
        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }
}
