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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.KcOidcBrokerConfiguration;
import org.keycloak.testsuite.organization.broker.BrokerConfigurationWrapper;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.SelectOrganizationPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.util.TestCleanup;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractOrganizationTest extends AbstractAdminTest  {

    protected String organizationName = "neworg";
    protected String memberEmail = "jdoe@neworg.org";
    protected String memberPassword = "password";
    protected Function<String, BrokerConfiguration> brokerConfigFunction = name -> new BrokerConfigurationWrapper(name, createBrokerConfiguration());


    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected SelectOrganizationPage selectOrganizationPage;

    @Page
    protected IdpConfirmLinkPage idpConfirmLinkPage;

    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @Page
    protected AppPage appPage;

    protected BrokerConfiguration bc = brokerConfigFunction.apply(organizationName);

    @Override
    protected TestCleanup getCleanup() {
        return getCleanup(TEST_REALM_NAME);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.getClients().addAll(bc.createConsumerClients());
        testRealm.setSmtpServer(null);
        testRealm.setOrganizationsEnabled(Boolean.TRUE);
        super.configureTestRealm(testRealm);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(bc.createProviderRealm());
        super.addTestRealms(testRealms);
    }

    protected OrganizationRepresentation createOrganization() {
        return createOrganization(organizationName);
    }

    protected OrganizationRepresentation createOrganization(String name) {
        return createOrganization(name, name + ".org");
    }

    protected OrganizationRepresentation createOrganization(String name, String... orgDomains) {
        return createOrganization(testRealm(), name, orgDomains);
    }

    protected OrganizationRepresentation createOrganization(RealmResource realm, String name, String... orgDomains) {
        return createOrganization(realm, getCleanup(), name, brokerConfigFunction.apply(name).setUpIdentityProvider(), orgDomains);
    }

    protected OrganizationRepresentation createOrganization(String name, boolean isBrokerPublic) {
        IdentityProviderRepresentation broker = brokerConfigFunction.apply(name).setUpIdentityProvider();
        broker.setHideOnLogin(!isBrokerPublic);
        return createOrganization(testRealm(), getCleanup(), name, broker, name + ".org");
    }

    protected OrganizationRepresentation createOrganization(RealmResource testRealm, TestCleanup testCleanup, String name,
                                                                   IdentityProviderRepresentation broker, String... orgDomains) {
        OrganizationRepresentation org = createRepresentation(name, orgDomains);
        String id;

        try (Response response = testRealm.organizations().create(org)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            id = ApiUtil.getCreatedId(response);
        }

        if (orgDomains != null && orgDomains.length > 0) {
            // set the idp domain to the first domain used to create the org.
            broker.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, orgDomains[0]);
            broker.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString());
        }
        testRealm.identityProviders().create(broker).close();
        testCleanup.addCleanup(testRealm.identityProviders().get(broker.getAlias())::remove);
        testRealm.organizations().get(id).identityProviders().addIdentityProvider(broker.getAlias()).close();
        org = testRealm.organizations().get(id).toRepresentation();
        testCleanup.addCleanup(() -> testRealm.organizations().get(id).delete().close());

        return org;
    }

    protected OrganizationRepresentation createRepresentation(String name, String... orgDomains) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(name);
        org.setAlias(name);
        org.setDescription(name + " is a test organization!");

        if (orgDomains != null) {
            for (String orgDomain : orgDomains) {
                OrganizationDomainRepresentation domainRep = new OrganizationDomainRepresentation();
                domainRep.setName(orgDomain);
                org.addDomain(domainRep);
            }
        }

        org.setAttributes(Map.of("key", List.of("value1", "value2")));

        return org;
    }

    protected MemberRepresentation addMember(OrganizationResource organization) {
        return addMember(organization, memberEmail);
    }

    protected MemberRepresentation addMember(OrganizationResource organization, String email) {
        return addMember(organization, null, email, null, null, true);
    }

    protected MemberRepresentation addMember(OrganizationResource organization, String email, String firstName, String lastName) {
        return addMember(organization, null, email, firstName, lastName, true);
    }

    protected MemberRepresentation addMember(OrganizationResource organization, String username, String email, String firstName, String lastName, boolean isSetCredentials) {
        UserRepresentation expected = new UserRepresentation();

        expected.setEmail(email);
        expected.setUsername(username == null ? expected.getEmail() : username);
        expected.setEnabled(true);
        expected.setFirstName(firstName);
        expected.setLastName(lastName);
        if (isSetCredentials) {
            Users.setPasswordFor(expected, memberPassword);
        }

        try (Response response = testRealm().users().create(expected)) {
            expected.setId(ApiUtil.getCreatedId(response));
        }

        getCleanup().addCleanup(() -> testRealm().users().get(expected.getId()).remove());

        String userId = expected.getId();

        try (Response response = organization.members().addMember(userId)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            MemberRepresentation actual = organization.members().member(userId).toRepresentation();

            assertNotNull(expected);
            assertEquals(userId, actual.getId());
            assertEquals(expected.getUsername(), actual.getUsername());
            assertEquals(expected.getEmail(), actual.getEmail());

            return actual;
        }
    }

    protected void assertBrokerRegistration(OrganizationResource organization, String username, String email) {
        // login with email only
        openIdentityFirstLoginPage(email, true, null, false, false);

        loginOrgIdp(username, email, true, true);

        assertIsMember(email, organization);
    }

    protected void loginOrgIdp(String username, String email, boolean firstTimeLogin, boolean redirectToApp) {
        // login to the organization identity provider and run the configured first broker login flow
        loginPage.login(username, bc.getUserPassword());

        if (firstTimeLogin) {
            waitForPage(driver, "update account information", false);
            updateAccountInformationPage.assertCurrent();
            assertTrue("We must be on correct realm right now",
                    driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
            log.debug("Updating info on updateAccount page");
            assertFalse(driver.getPageSource().contains("kc.org"));
            updateAccountInformationPage.updateAccountInformation(username, email, "Firstname", "Lastname");
        }

        if (redirectToApp) {
            appPage.assertCurrent();
            assertThat(appPage.getRequestType(), is(AppPage.RequestType.AUTH_RESPONSE));
        }

        List<UserRepresentation> users = realmsResouce().realm(bc.consumerRealmName()).users().search(username, Boolean.TRUE);
        if (!users.isEmpty()) {
            assertThat(users, Matchers.hasSize(1));
            getCleanup(bc.consumerRealmName()).addUserId(users.get(0).getId());
        }
    }

    protected void assertIsMember(String userEmail, OrganizationResource organization) {
        UserRepresentation account = getUserRepresentation(userEmail);
        UserRepresentation member = organization.members().member(account.getId()).toRepresentation();
        Assert.assertEquals(account.getId(), member.getId());
    }

    protected UserRepresentation getUserRepresentation(String userEmail) {
        return getUserRepresentation(bc.consumerRealmName(), userEmail);
    }

    protected UserRepresentation getUserRepresentation(String realm, String userEmail) {
        UsersResource users = adminClient.realm(realm).users();
        List<UserRepresentation> reps = users.searchByEmail(userEmail, true);
        assertFalse(reps.isEmpty());
        Assert.assertEquals(1, reps.size());
        return reps.get(0);
    }

    protected GroupRepresentation createGroup(RealmResource realm, String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        try (Response response = realm.groups().add(group)) {
            String groupId = ApiUtil.getCreatedId(response);

            // Set ID to the original rep
            group.setId(groupId);
            return group;
        }
    }

    protected BrokerConfiguration createBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public RealmRepresentation createProviderRealm() {
                // enable organizations in the provider realm too just for testing purposes.
                RealmRepresentation realmRep = super.createProviderRealm();
                realmRep.setOrganizationsEnabled(true);
                return realmRep;
            }
        };
    }

    protected void openIdentityFirstLoginPage(String username, boolean autoIDPRedirect, String idpAlias, boolean isVisible, boolean clickIdp) {
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        assertTrue(loginPage.isUsernameInputPresent());
        assertNull(loginPage.getUsernameInputError());
        assertFalse(loginPage.isPasswordInputPresent());
        assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
        assertTrue(loginPage.isRegisterLinkPresent());
        if (idpAlias != null) {
            if (isVisible) {
                assertTrue(loginPage.isSocialButtonPresent(idpAlias));
            } else {
                assertFalse(loginPage.isSocialButtonPresent(idpAlias));
            }
        }
        loginPage.loginUsername(username);

        if (clickIdp) {
            assertTrue(loginPage.isPasswordInputPresent());
            assertTrue(loginPage.isSocialButtonPresent(idpAlias));
            loginPage.clickSocial(idpAlias);
        }

        waitForPage(driver, "sign in to", true);

        // user automatically redirected to the organization identity provider
        if (autoIDPRedirect) {
            assertThat("Driver should be on the provider realm page right now",
                    driver.getCurrentUrl(), Matchers.containsString("/auth/realms/" + bc.providerRealmName() + "/"));
        } else {
            assertThat("Driver should be on the consumer realm page right now",
                    driver.getCurrentUrl(), Matchers.containsString("/auth/realms/" + bc.consumerRealmName() + "/"));
        }
    }
}
