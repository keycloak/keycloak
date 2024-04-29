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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.util.List;
import java.util.function.Function;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.OrganizationModel;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.KcOidcBrokerConfiguration;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.util.TestCleanup;

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
    protected IdpConfirmLinkPage idpConfirmLinkPage;

    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @Page
    protected AppPage appPage;

    protected BrokerConfiguration bc = brokerConfigFunction.apply(organizationName);

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

    protected OrganizationRepresentation createOrganization(String name, String... orgDomain) {
        return createOrganization(testRealm(), getCleanup(), name, brokerConfigFunction.apply(name).setUpIdentityProvider(), orgDomain);
    }

    protected static OrganizationRepresentation createOrganization(RealmResource testRealm, TestCleanup testCleanup, String name, IdentityProviderRepresentation broker, String... orgDomain) {
        OrganizationRepresentation org = createRepresentation(name, orgDomain);
        String id;

        try (Response response = testRealm.organizations().create(org)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            id = ApiUtil.getCreatedId(response);
        }
        broker.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, org.getDomains().iterator().next().getName());
        testRealm.identityProviders().create(broker).close();
        testCleanup.addCleanup(testRealm.identityProviders().get(broker.getAlias())::remove);
        testRealm.organizations().get(id).identityProviders().addIdentityProvider(broker.getAlias()).close();
        org = testRealm.organizations().get(id).toRepresentation();
        testCleanup.addCleanup(() -> testRealm.organizations().get(id).delete().close());

        return org;
    }

    protected static OrganizationRepresentation createRepresentation(String name, String... orgDomains) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(name);

        for (String orgDomain : orgDomains) {
            OrganizationDomainRepresentation domainRep = new OrganizationDomainRepresentation();
            domainRep.setName(orgDomain);
            org.addDomain(domainRep);
        }

        return org;
    }

    protected UserRepresentation addMember(OrganizationResource organization) {
        return addMember(organization, memberEmail);
    }

    protected UserRepresentation addMember(OrganizationResource organization, String email) {
        return addMember(organization, email, null, null);
    }

    protected UserRepresentation addMember(OrganizationResource organization, String email, String firstName, String lastName) {
        UserRepresentation expected = new UserRepresentation();

        expected.setEmail(email);
        expected.setUsername(expected.getEmail());
        expected.setEnabled(true);
        expected.setFirstName(firstName);
        expected.setLastName(lastName);
        Users.setPasswordFor(expected, memberPassword);

        try (Response response = testRealm().users().create(expected)) {
            expected.setId(ApiUtil.getCreatedId(response));
        }

        getCleanup().addCleanup(() -> testRealm().users().get(expected.getId()).remove());

        String userId = expected.getId();

        try (Response response = organization.members().addMember(userId)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            UserRepresentation actual = organization.members().member(userId).toRepresentation();

            assertNotNull(expected);
            assertEquals(userId, actual.getId());
            assertEquals(expected.getUsername(), actual.getUsername());
            assertEquals(expected.getEmail(), actual.getEmail());

            return actual;
        }
    }

    protected void assertBrokerRegistration(OrganizationResource organization, String email) {
        // login with email only
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
        loginPage.loginUsername(email);

        // user automatically redirected to the organization identity provider
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        // login to the organization identity provider and run the configured first broker login flow
        loginPage.login(email, bc.getUserPassword());
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
        log.debug("Updating info on updateAccount page");
        assertFalse(driver.getPageSource().contains("kc.org"));
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), email, "Firstname", "Lastname");
        assertThat(appPage.getRequestType(),is(AppPage.RequestType.AUTH_RESPONSE));
        
        assertIsMember(email, organization);
    }

    protected void assertIsMember(String userEmail, OrganizationResource organization) {
        UserRepresentation account = getUserRepresentation(userEmail);
        UserRepresentation member = organization.members().member(account.getId()).toRepresentation();
        Assert.assertEquals(account.getId(), member.getId());
    }

    protected UserRepresentation getUserRepresentation(String userEmail) {
        UsersResource users = adminClient.realm(bc.consumerRealmName()).users();
        List<UserRepresentation> reps = users.searchByEmail(userEmail, true);
        Assert.assertFalse(reps.isEmpty());
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
        return new KcOidcBrokerConfiguration();
    };
}
