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

package org.keycloak.testsuite.organization.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.io.IOException;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationMemberAuthenticationTest extends AbstractOrganizationTest {

    @Test
    public void testAuthenticateUnmanagedMember() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org");

        // first try to log in using only the email
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
        loginPage.loginUsername(member.getEmail());

        // the email does not match an organization so redirect to the realm's default authentication mechanism
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the consumer realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
        Assert.assertTrue(loginPage.isPasswordInputPresent());
        Assert.assertEquals(member.getEmail(), loginPage.getUsername());
        // no idp should be shown because there is only a single idp that is bound to an organization
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        // the member should be able to log in using the credentials
        loginPage.login(member.getEmail(), memberPassword);
        appPage.assertCurrent();
    }

    @Test
    public void testTryLoginWithUsernameNotAnEmail() {
        testRealm().organizations().get(createOrganization().getId());
        oauth.clientId("broker-app");

        // login with email only
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        loginPage.loginUsername("user");

        // check if the login page is shown
        Assert.assertTrue(loginPage.isUsernameInputPresent());
        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testDefaultAuthenticationMechanismIfNotOrganizationMember() {
        testRealm().organizations().get(createOrganization().getId());
        oauth.clientId("broker-app");

        // login with email only
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        loginPage.loginUsername("user@noorg.org");

        // check if the login page is shown
        Assert.assertTrue(loginPage.isUsernameInputPresent());
        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testAuthenticateUnmanagedMemberWehnProviderDisabled() throws IOException {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        UserRepresentation member = addMember(organization, "contractor@contractor.org");

        // first try to access login page
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        // disable the organization provider
        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm())
                .setOrganizationEnabled(Boolean.FALSE)
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
}
