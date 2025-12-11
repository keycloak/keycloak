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

package org.keycloak.testsuite.organization.broker;

import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationIdentityProviderResource;
import org.keycloak.admin.client.resource.OrganizationMemberResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.UserBuilder;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.keycloak.models.OrganizationDomainModel.ANY_DOMAIN;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractBrokerSelfRegistrationTest extends AbstractOrganizationTest {

    @Test
    public void testRegistrationRedirectWhenSingleBroker() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());
    }

    @Test
    public void testLoginHintSentToBrokerWhenEnabled() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(IdentityProviderModel.LOGIN_HINT, "true");
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);

        openIdentityFirstLoginPage(bc.getUserEmail(), true, null, false,false);
        // check if the username is automatically filled
        Assert.assertEquals(bc.getUserEmail(), loginPage.getUsername());
    }

    @Test
    public void testLoginHintSentToBrokerIfUserAlreadyAMember() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(IdentityProviderModel.LOGIN_HINT, "true");
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);
        String userId = ApiUtil.getCreatedId(testRealm().users().create(UserBuilder.create()
                .username("test")
                .email("test@neworg.org")
                .enabled(true)
                .firstName("f")
                .lastName("l")
                .build()));
        organization.members().addMember(userId).close();

        // login hint will automatically redirect user to broker
        oauth.realm(bc.consumerRealmName());
        oauth.client("broker-app");
        oauth.loginForm().loginHint("test@neworg.org").open();

        MatcherAssert.assertThat("Driver should be on the provider realm page right now",
                driver.getCurrentUrl(), Matchers.containsString("/auth/realms/" + bc.providerRealmName() + "/"));
    }

    @Test
    public void testIdentityFirstIfUserNotExistsAndEmailMatchOrgDomain() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);

        openIdentityFirstLoginPage("user@neworg.org", false, null, false, false);

        Assert.assertTrue(loginPage.isUsernameInputPresent());
        // registration link shown
        Assert.assertTrue(loginPage.isRegisterLinkPresent());
        // no need for password because the user does not exist
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertFalse(loginPage.isSocialButtonPresent(idpRep.getAlias()));
    }

    @Test
    public void testIdentityFirstUserNotExistEmailMatchBrokerDomainAndBrokerIsPublic() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.setHideOnLogin(false);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);

        openIdentityFirstLoginPage("user@neworg.org", false, null, false, false);

        Assert.assertEquals("Your email domain matches the neworg organization but you don't have an account yet.", loginPage.getError());
        Assert.assertTrue(loginPage.isUsernameInputPresent());
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertTrue(loginPage.isSocialButtonPresent(idpRep.getAlias()));

        // no self-registration link because the user should register through the broker
        Assert.assertFalse(loginPage.isRegisterLinkPresent());
    }

    @Test
    public void testIdentityFirstUserNotExistEmailMatchBrokerDomainNoPublicBroker() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);

        openIdentityFirstLoginPage("user@neworg.org", false, null, false, false);

        Assert.assertTrue(driver.getPageSource().contains("Your email domain matches the neworg organization but you don't have an account yet."));
        Assert.assertTrue(loginPage.isUsernameInputPresent());
        Assert.assertFalse(loginPage.isPasswordInputPresent());
        // self-registration link shown because there is no public broker and user can choose to register
        Assert.assertTrue(loginPage.isRegisterLinkPresent());
    }

    @Test
    public void testDefaultAuthenticationShowsPublicOrganizationBrokers() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        OrganizationRepresentation representation = organization.toRepresentation();
        representation.addDomain(new OrganizationDomainRepresentation("other.org"));
        organization.update(representation).close();
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        // set a domain to the existing broker
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp = bc.setUpIdentityProvider();
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.setHideOnLogin(false);
        idp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        // create a second broker without a domain set
        testRealm().identityProviders().create(idp).close();
        getCleanup().addCleanup(testRealm().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();
        idp = organization.identityProviders().get(idp.getAlias()).toRepresentation();

        openIdentityFirstLoginPage("external@user.org", false, idp.getAlias(), false, false);

        Assert.assertTrue(loginPage.isPasswordInputPresent());
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
        Assert.assertTrue(loginPage.isSocialButtonPresent(idp.getAlias()));

        idp.setHideOnLogin(true);
        testRealm().identityProviders().get(idp.getAlias()).update(idp);
        driver.navigate().refresh();
        Assert.assertTrue(loginPage.isPasswordInputPresent());
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
        Assert.assertFalse(loginPage.isSocialButtonPresent(idp.getAlias()));
    }

    @Test
    public void testDefaultAuthenticationWhenUserExistEmailMatchOrgDomain() {
        realmsResouce().realm(bc.consumerRealmName()).users()
                .create(UserBuilder.create()
                        .username("user@neworg.org")
                        .email("user@neworg.org")
                        .password(bc.getUserPassword())
                        .enabled(true).build()
                ).close();
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);

        openIdentityFirstLoginPage("user@neworg.org", false, null, false, false);

        Assert.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testRealmLevelBrokersAvailableIfEmailDoesNotMatchOrganization() {
        testRealm().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user", false, null, false, false);

        // check if the login page is shown
        loginPage.assertAttemptedUsernameAvailability(true);
        Assert.assertTrue(loginPage.isPasswordInputPresent());
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        IdentityProviderRepresentation idp = bc.setUpIdentityProvider();
        idp.setAlias("realm-level-idp");
        idp.setHideOnLogin(false);
        testRealm().identityProviders().create(idp).close();

        driver.navigate().refresh();

        loginPage.assertAttemptedUsernameAvailability(true);
        Assert.assertTrue(loginPage.isPasswordInputPresent());
        Assert.assertTrue(loginPage.isSocialButtonPresent(idp.getAlias()));
        Assert.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
    }

    @Test
    public void testLinkExistingAccount() {
        createUserInConsumerRealm();

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        OrganizationIdentityProviderResource broker = organization.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation brokerRep = broker.toRepresentation();
        brokerRep.setHideOnLogin(false);
        brokerRep.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        testRealm().identityProviders().get(brokerRep.getAlias()).update(brokerRep);

        openIdentityFirstLoginPage(bc.getUserEmail(), true, brokerRep.getAlias(), false, true);

        loginOrgIdp(bc.getUserEmail(), bc.getUserEmail(),true, false);

        // account with the same email exists in the realm, execute account linking
        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();
        // confirm the link by authenticating
        loginPage.login(bc.getUserEmail(), bc.getUserPassword());
        assertIsMember(bc.getUserEmail(), organization);
    }

    @Test
    public void testExistingUserUsingOrgDomain() {
        createUserInConsumerRealm();

        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        OrganizationIdentityProviderResource broker = organization.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation brokerRep = broker.toRepresentation();
        brokerRep.setHideOnLogin(false);
        brokerRep.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        testRealm().identityProviders().get(brokerRep.getAlias()).update(brokerRep);

        openIdentityFirstLoginPage(bc.getUserEmail(), true, brokerRep.getAlias(), false, true);

        // login to the organization identity provider and run the configured first broker login flow
        loginOrgIdp(bc.getUserEmail(), bc.getUserEmail(), true, false);

        // account with the same email exists in the realm, execute account linking
        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();
        // confirm the link by authenticating
        loginPage.login(bc.getUserEmail(), bc.getUserPassword());
        assertIsMember(bc.getUserEmail(), organization);
    }

    @Test
    public void testRedirectBrokerWhenManagedMember() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        // add the member for the first time
        assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());

        // logout to force the user to authenticate again
        UserRepresentation account = getUserRepresentation(bc.getUserEmail());
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        openIdentityFirstLoginPage(bc.getUserLogin(), true, null, false, false);

        // login to the organization identity provider by username and automatically redirects to the app as the account already exists
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        appPage.assertCurrent();
        assertIsMember(bc.getUserEmail(), organization);

        // logout to force the user to authenticate again
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        openIdentityFirstLoginPage(bc.getUserEmail(), true, null, false, false);

        // login to the organization identity provider by email and automatically redirects to the app as the account already exists
        loginPage.login(bc.getUserEmail(), bc.getUserPassword());
        appPage.assertCurrent();
        assertIsMember(bc.getUserEmail(), organization);
    }

    @Test
    public void testRedirectManagedMemberOfMultipleOrganizations() {
        OrganizationResource orgA = testRealm().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = testRealm().organizations().get(createOrganization("org-b").getId());
        String email = bc.getUserLogin() + "@" + orgA.toRepresentation().getDomains().iterator().next().getName();
        UserRepresentation account = UserBuilder.create()
                .username(email)
                .email(email)
                .password(bc.getUserPassword())
                .enabled(true)
                .build();
        UsersResource users = realmsResouce().realm(bc.providerRealmName()).users();
        try (Response response = users.create(account)) {
            account.setId(ApiUtil.getCreatedId(response));
        }
        UserRepresentation finalAccount = account;
        getCleanup().addCleanup(() -> users.get(finalAccount.getId()).remove());
        // add the member for the first time
        assertBrokerRegistration(orgA, email, email);
        account = getUserRepresentation(account.getEmail());
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        orgB.members().addMember(account.getId()).close();
        openIdentityFirstLoginPage(email, true, null, false, false);
        // login to the organization identity provider using e-mail and automatically redirects to the app as the account already exists
        loginPage.login(email, bc.getUserPassword());
        appPage.assertCurrent();
        UserRepresentation finalAccount1 = account;
        getCleanup().addCleanup(() -> realmsResouce().realm(bc.consumerRealmName()).users().get(finalAccount1.getId()).remove());

        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();
        openIdentityFirstLoginPage(email, true, null, false, false);
        // login to the organization identity provider user username and automatically redirects to the app as the account already exists
        loginPage.login(account.getUsername(), bc.getUserPassword());
        appPage.assertCurrent();
    }

    @Test
    public void testRedirectManagedMemberOfMultipleOrganizationsAllOrganizationsScope() {
        OrganizationResource orgA = testRealm().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = testRealm().organizations().get(createOrganization("org-b").getId());
        String email = bc.getUserLogin() + "@" + orgA.toRepresentation().getDomains().iterator().next().getName();
        UserRepresentation account = UserBuilder.create()
                .username(email)
                .email(email)
                .password(bc.getUserPassword())
                .enabled(true)
                .build();
        UsersResource users = realmsResouce().realm(bc.providerRealmName()).users();
        try (Response response = users.create(account)) {
            account.setId(ApiUtil.getCreatedId(response));
        }
        UserRepresentation finalAccount = account;
        getCleanup().addCleanup(() -> users.get(finalAccount.getId()).remove());
        // add the member for the first time
        assertBrokerRegistration(orgA, email, email);
        account = getUserRepresentation(account.getEmail());
        UserRepresentation finalAccount1 = account;
        getCleanup().addCleanup(() -> realmsResouce().realm(bc.consumerRealmName()).users().get(finalAccount1.getId()).remove());
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        orgB.members().addMember(account.getId()).close();
        oauth.scope("organization:*");
        openIdentityFirstLoginPage(email, true, null, false, false);
        // login to the organization identity provider by username and automatically redirects to the app as the account already exists
        loginPage.login(email, bc.getUserPassword());
        appPage.assertCurrent();
    }

    @Test
    public void testRedirectManagedMemberUsingUnManagedMemberAllOrganizationsScope() {
        OrganizationResource orgA = testRealm().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = testRealm().organizations().get(createOrganization("org-b").getId());
        String email = bc.getUserLogin() + "@" + orgA.toRepresentation().getDomains().iterator().next().getName();
        UserRepresentation account = UserBuilder.create()
                .username(email)
                .email(email)
                .password(bc.getUserPassword())
                .enabled(true)
                .build();
        UsersResource users = realmsResouce().realm(bc.providerRealmName()).users();
        try (Response response = users.create(account)) {
            account.setId(ApiUtil.getCreatedId(response));
        }
        UserRepresentation finalAccount = account;
        getCleanup().addCleanup(() -> users.get(finalAccount.getId()).remove());
        // add the member for the first time
        assertBrokerRegistration(orgA, email, email);
        account = getUserRepresentation(account.getEmail());
        UserRepresentation finalAccount1 = account;
        getCleanup().addCleanup(() -> realmsResouce().realm(bc.consumerRealmName()).users().get(finalAccount1.getId()).remove());
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        orgB.members().addMember(account.getId()).close();
        oauth.scope("organization:*");
        String orgBEmail = bc.getUserLogin() + "@" + orgB.toRepresentation().getDomains().iterator().next().getName();
        openIdentityFirstLoginPage(orgBEmail, true, null, false, false);
        // login to the organization identity provider by username and as to review profile because the user is not yet linked with the idp
        loginPage.login(email, bc.getUserPassword());
        updateAccountInformationPage.assertCurrent();
        // should enforce a email with the same domain as the organization
        updateAccountInformationPage.updateAccountInformation(email, email, "f", "l");
        Assert.assertTrue(driver.getPageSource().contains("Email domain does not match any domain from the organization"));

        updateAccountInformationPage.updateAccountInformation(email, orgBEmail, "f", "l");
        // user is asked to link accounts
        idpConfirmLinkPage.assertCurrent();
    }

    @Test
    public void testRedirectUnManagedMemberAllOrganizationsScope() {
        OrganizationResource orgA = testRealm().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = testRealm().organizations().get(createOrganization("org-b").getId());
        String orgAEmail = bc.getUserLogin() + "@" + orgA.toRepresentation().getDomains().iterator().next().getName();
        // create user without credential to force the redirect to the IdP
        UserRepresentation account = UserBuilder.create()
                .username(orgAEmail)
                .email(orgAEmail)
                .enabled(true)
                .build();
        UsersResource users = realmsResouce().realm(bc.consumerRealmName()).users();
        try (Response response = users.create(account)) {
            String id = ApiUtil.getCreatedId(response);
            account.setId(id);
            getCleanup().addCleanup(() -> users.get(id).remove());
        }

        // add the unmanaged member to both organizations
        orgA.members().addMember(account.getId()).close();
        orgB.members().addMember(account.getId()).close();

        oauth.scope("organization:*");
        // resolve both organizations and redirect the user automatically
        openIdentityFirstLoginPage(orgAEmail, true, null, false, false);
        assertTrue(driver.getPageSource().contains("Sign in to provider"));
        openIdentityFirstLoginPage(bc.getUserLogin() + "@" + orgB.toRepresentation().getDomains().iterator().next().getName(), true, null, false, false);
        assertTrue(driver.getPageSource().contains("Sign in to provider"));

        oauth.scope("organization");
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(orgAEmail);
        selectOrganizationPage.assertCurrent();
    }

    @Test
    public void testRedirectBrokerManagedMemberUsingUsernameAllOrganizationsScope() {
        OrganizationResource orgA = testRealm().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = testRealm().organizations().get(createOrganization("org-b").getId());
        String email = bc.getUserLogin() + "@" + orgA.toRepresentation().getDomains().iterator().next().getName();
        UserRepresentation account = UserBuilder.create()
                .username(email)
                .email(email)
                .password(bc.getUserPassword())
                .enabled(true)
                .build();
        UsersResource users = realmsResouce().realm(bc.providerRealmName()).users();
        try (Response response = users.create(account)) {
            account.setId(ApiUtil.getCreatedId(response));
        }
        UserRepresentation finalAccount = account;
        getCleanup().addCleanup(() -> users.get(finalAccount.getId()).remove());
        // add the member for the first time
        assertBrokerRegistration(orgA, bc.getUserLogin(), email);
        account = getUserRepresentation(account.getEmail());
        UserRepresentation finalAccount1 = account;
        getCleanup().addCleanup(() -> realmsResouce().realm(bc.consumerRealmName()).users().get(finalAccount1.getId()).remove());
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        orgB.members().addMember(account.getId()).close();
        oauth.scope("organization:*");
        // provide the username, the user is automatically redirected to home broker because he is a managed member
        openIdentityFirstLoginPage(bc.getUserLogin(), true, null, false, false);
        // login to the organization identity provider by username
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        appPage.assertCurrent();
    }

    @Test
    public void testRedirectBrokerWhenUnmanagedMemberProfileEmailMatchesOrganization() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        addMember(organization, bc.getUserLogin(), bc.getUserEmail(), "f", "l", false);
        openIdentityFirstLoginPage(bc.getUserLogin(), true, null, false, false);
    }

    @Test
    public void testShowOnlyBrokersLinkedUserInPasswordPage() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        OrganizationIdentityProviderResource broker = organization.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation brokerRep = broker.toRepresentation();
        brokerRep.setHideOnLogin(false);
        brokerRep.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.FALSE.toString());
        testRealm().identityProviders().get(brokerRep.getAlias()).update(brokerRep);
        IdentityProviderRepresentation secondIdp = bc.setUpIdentityProvider();
        secondIdp.setAlias("second-idp");
        secondIdp.setInternalId(null);
        secondIdp.setHideOnLogin(false);
        secondIdp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().create(secondIdp).close();
        getCleanup().addCleanup(testRealm().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(secondIdp.getAlias()).close();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        String email = bc.getUserEmail();
        loginPage.loginUsername(email);
        // second-idp shown because user is not linked yet to any broker
        Assert.assertTrue(loginPage.isSocialButtonPresent(secondIdp.getAlias()));
        brokerRep.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString());
        testRealm().identityProviders().get(brokerRep.getAlias()).update(brokerRep);

        assertBrokerRegistration(organization, bc.getUserLogin(), email);

        // logout to force the user to authenticate again
        UserRepresentation account = getUserRepresentation(email);
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        broker = organization.identityProviders().get(bc.getIDPAlias());
        brokerRep = broker.toRepresentation();
        organization.identityProviders().get(brokerRep.getAlias()).delete().close();
        IdentityProviderRepresentation finalBrokerRep = brokerRep;
        getCleanup().addCleanup(() -> organization.identityProviders().addIdentityProvider(finalBrokerRep.getInternalId()).close());

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        loginPage.loginUsername(email);
        Assert.assertFalse(loginPage.isUsernameInputPresent());
        Assert.assertTrue(loginPage.isPasswordInputPresent());
        Assert.assertTrue(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
        // second-idp not shown because user is linked to another broker
        Assert.assertFalse(loginPage.isSocialButtonPresent(secondIdp.getAlias()));
    }

    @Test
    public void testNoIDPRedirectWhenUserHasCredentialsSet() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.setHideOnLogin(false);
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);

        assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());

        // set the user's credentials
        UserRepresentation user = testRealm().users().searchByEmail(bc.getUserEmail(), true).get(0);
        ApiUtil.resetUserPassword(realmsResouce().realm(bc.consumerRealmName()).users().get(user.getId()), "updated-password", false);

        // logout to force the user to authenticate again
        UserRepresentation account = getUserRepresentation(bc.getUserEmail());
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(bc.getUserEmail());

        // after providing username/email, user can pick to authenticate by password or by org IDP
        Assert.assertFalse(loginPage.isUsernameInputPresent());
        Assert.assertTrue(loginPage.isPasswordInputPresent());
        Assert.assertTrue(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        // log in by password
        loginPage.login("updated-password");
        appPage.assertCurrent();
        MatcherAssert.assertThat(appPage.getRequestType(), is(AppPage.RequestType.AUTH_RESPONSE));

        // logout again
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        // log in via IDP
        openIdentityFirstLoginPage(bc.getUserEmail(), true, bc.getIDPAlias(), false, true);
        loginOrgIdp(bc.getUserLogin(), bc.getUserEmail(), false, true);
    }

    @Test
    public void testFailUpdateEmailNotAssociatedOrganizationUsingAdminAPI() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        OrganizationIdentityProviderResource idp = organization.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation idpRep = idp.toRepresentation();
        idpRep.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);

        // add the member for the first time
        assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());
        UserRepresentation member = getUserRepresentation(bc.getUserEmail());

        member.setEmail(KeycloakModelUtils.generateId() + "@user.org");

        try {
            // member has a hard link with the organization, and the email must match the domains set to the organization
            testRealm().users().get(member.getId()).update(member);
            fail("Should fail because email domain does not match any from organization");
        } catch (BadRequestException expected) {
            ErrorRepresentation error = expected.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals(UserModel.EMAIL, error.getField());
            assertEquals("Email domain does not match any domain from the organization", error.getErrorMessage());
        }

        member.setEmail(member.getEmail().replace("@user.org", "@" + organizationName + ".org"));
        testRealm().users().get(member.getId()).update(member);
    }

    @Test
    public void testDeleteManagedMember() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());

        // add the member for the first time
        assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());
        UserRepresentation member = getUserRepresentation(bc.getUserEmail());
        OrganizationMemberResource organizationMember = organization.members().member(member.getId());

        organizationMember.delete().close();

        try {
            testRealm().users().get(member.getId()).toRepresentation();
            fail("it is managed member should be removed from the realm");
        } catch (NotFoundException expected) {
        }

        try {
            organizationMember.toRepresentation();
            fail("it is managed member should be removed from the realm");
        } catch (NotFoundException expected) {
        }
    }

    @Test
    public void testRedirectToIdentityProviderAssociatedWithOrganizationDomain() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().create(idp).close();
        getCleanup().addCleanup(testRealm().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        openIdentityFirstLoginPage(bc.getUserEmail(), true, idp.getAlias(), false, false);

        loginOrgIdp(bc.getUserEmail(), bc.getUserEmail(),true, true);

        assertIsMember(bc.getUserEmail(), organization);
        UserRepresentation user = testRealm().users().search(bc.getUserEmail()).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = testRealm().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(bc.getIDPAlias(), federatedIdentities.get(0).getIdentityProvider());
    }

    @Test
    public void testRedirectToIdentityProviderAssociatedWithOrganizationDomainCaseInsensitive() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().create(idp).close();
        getCleanup().addCleanup(testRealm().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        openIdentityFirstLoginPage(bc.getUserEmail().toUpperCase(), true, idp.getAlias(), false, false);

        loginOrgIdp(bc.getUserEmail().toUpperCase(), bc.getUserEmail().toUpperCase(),true, true);

        assertIsMember(bc.getUserEmail().toUpperCase(), organization);
        UserRepresentation user = testRealm().users().search(bc.getUserEmail().toUpperCase()).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = testRealm().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(bc.getIDPAlias(), federatedIdentities.get(0).getIdentityProvider());
    }

    @Test
    public void testRedirectToIdentityProviderAssociatedWithOrganizationDomainUsingAnyMatch() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, ANY_DOMAIN);
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp.setAlias("second-idp");
        idp.setInternalId(null);
        testRealm().identityProviders().create(idp).close();
        getCleanup().addCleanup(testRealm().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        openIdentityFirstLoginPage(bc.getUserEmail(), true, idp.getAlias(), false, false);

        loginOrgIdp(bc.getUserEmail(), bc.getUserEmail(),true, true);

        assertIsMember(bc.getUserEmail(), organization);
        UserRepresentation user = testRealm().users().search(bc.getUserEmail()).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = testRealm().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(bc.getIDPAlias(), federatedIdentities.get(0).getIdentityProvider());
    }

    @Test
    public void testDoNotRedirectToIdentityProviderAssociatedWithOrganizationDomain() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.setHideOnLogin(false);
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        idp.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.FALSE.toString());
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);

        openIdentityFirstLoginPage(bc.getUserEmail(), false, idp.getAlias(), false, false);

        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertTrue(driver.getPageSource().contains("Your email domain matches the " + organizationName + " organization but you don't have an account yet."));
        Assert.assertTrue(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, ANY_DOMAIN);
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);

        openIdentityFirstLoginPage(bc.getUserEmail(), false, idp.getAlias(), false, false);

        Assert.assertFalse(loginPage.isPasswordInputPresent());
        Assert.assertTrue(driver.getPageSource().contains("Your email domain matches the " + organizationName + " organization but you don't have an account yet."));
        Assert.assertTrue(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
    }

    @Test
    public void testOnlyShowBrokersAssociatedWithResolvedOrganization() {
        String org0Name = "org-0";
        OrganizationResource org0 = testRealm().organizations().get(createOrganization(org0Name).getId());
        IdentityProviderRepresentation org0Broker = org0.identityProviders().getIdentityProviders().get(0);
        org0Broker.setHideOnLogin(false);
        org0Broker.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().get(org0Broker.getAlias()).update(org0Broker);
        String org1Name = "org-1";
        OrganizationResource org1 = testRealm().organizations().get(createOrganization(org1Name).getId());
        IdentityProviderRepresentation org1Broker = org1.identityProviders().getIdentityProviders().get(0);
        org1Broker.setHideOnLogin(false);
        org1Broker.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        org1Broker.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        testRealm().identityProviders().get(org1Broker.getAlias()).update(org1Broker);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("user@org-0.org");
        Assert.assertTrue(driver.getPageSource().contains("Your email domain matches the " + org0Name + " organization but you don't have an account yet."));
        Assert.assertTrue(loginPage.isSocialButtonPresent(org0Broker.getAlias()));
        Assert.assertFalse(loginPage.isSocialButtonPresent(org1Broker.getAlias()));

        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("user@org-1.org");
        Assert.assertTrue(driver.getPageSource().contains("Your email domain matches the " + org1Name + " organization but you don't have an account yet."));
        Assert.assertTrue(loginPage.isSocialButtonPresent(org1Broker.getAlias()));
        Assert.assertFalse(loginPage.isSocialButtonPresent(org0Broker.getAlias()));
    }

    @Test
    public void testLoginUsingBrokerWithoutDomain() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        // set a domain to the existing broker
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp = bc.setUpIdentityProvider();
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.setHideOnLogin(false);
        idp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        // create a second broker without a domain set
        testRealm().identityProviders().create(idp).close();
        getCleanup().addCleanup(testRealm().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        String email = "external@user.org";
        openIdentityFirstLoginPage(email, true, idp.getAlias(), false, true);

        loginOrgIdp("external", email, true, true);

        assertIsMember(email, organization);

        // make sure the federated identity matches the expected broker
        UserRepresentation user = testRealm().users().searchByEmail(email, true).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = testRealm().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(idp.getAlias(), federatedIdentities.get(0).getIdentityProvider());
        testRealm().users().get(user.getId()).remove();
    }

    @Test
    public void testEmailDomainDoesNotMatchBrokerDomain() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        OrganizationRepresentation representation = organization.toRepresentation();
        representation.addDomain(new OrganizationDomainRepresentation("other.org"));
        organization.update(representation).close();
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        // set a domain to the existing broker
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp = bc.setUpIdentityProvider();
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.setHideOnLogin(false);
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "other.org");
        // create a second broker without a domain set
        testRealm().identityProviders().create(idp).close();
        getCleanup().addCleanup(testRealm().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        String email = "external@user.org";
        openIdentityFirstLoginPage(email, true, idp.getAlias(), false, true);

        loginOrgIdp(email, email, true, false);

        Assert.assertTrue(driver.getPageSource().contains("Email domain does not match any domain from the organization"));
        assertIsNotMember(email, organization);

        updateAccountInformationPage.updateAccountInformation("external@other.org", "external@other.org", "Firstname", "Lastname");
        appPage.assertCurrent();
        assertIsMember("external@other.org", organization);
    }

    @Test
    public void testAnyEmailFromBrokerWithoutDomainSet() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        OrganizationRepresentation representation = organization.toRepresentation();
        representation.addDomain(new OrganizationDomainRepresentation("other.org"));
        organization.update(representation).close();
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        // set a domain to the existing broker
        testRealm().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp = bc.setUpIdentityProvider();
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.setHideOnLogin(false);
        // create a second broker without a domain set
        testRealm().identityProviders().create(idp).close();
        getCleanup().addCleanup(testRealm().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        String email = "external@user.org";
        openIdentityFirstLoginPage(email, true, idp.getAlias(), false, true);

        loginOrgIdp(email, "external@unknown.org", true, true);
        assertIsMember("external@unknown.org", organization);
    }

    @Test
    public void testRealmLevelBrokerNotImpactedByOrganizationFlow() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = bc.setUpIdentityProvider();
        idp.setAlias("realm-idp");
        idp.setInternalId(null);
        idp.setHideOnLogin(false);
        // create a second broker without a domain set
        testRealm().identityProviders().create(idp).close();

        openIdentityFirstLoginPage("some@user.org", true, idp.getAlias(), true, true);

        loginOrgIdp("external", bc.getUserEmail(), true, true);

        assertThat(organization.members().list(-1, -1), Matchers.empty());

        UserRepresentation user = testRealm().users().searchByEmail(bc.getUserEmail(), true).get(0);
        testRealm().users().get(user.getId()).remove();
    }

    @Test
    public void testMemberRegistrationUsingDifferentDomainThanOrganization() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);

        // make sure the user can select this idp from the organization when authenticating
        idpRep.setHideOnLogin(false);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);

        // create a user to the provider realm using an email that does not share the same domain as the org
        UserRepresentation user = UserBuilder.create()
                .username("user")
                .email("user@different.org")
                .password("password")
                .enabled(true)
                .build();
        realmsResouce().realm(bc.providerRealmName()).users().create(user).close();

        // select the organization broker to authenticate
        openIdentityFirstLoginPage("user@different.org", true, idpRep.getAlias(), false, true);

        loginOrgIdp(user.getEmail(), user.getEmail(), true, true);
    }

    @Test
    public void testMemberFromBrokerRedirectedToOriginBroker() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);

        // make sure the user can select this idp from the organization when authenticating
        idpRep.setHideOnLogin(false);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);

        // create a user to the provider realm using an email that does not share the same domain as the org
        UserRepresentation user = UserBuilder.create()
                .username("user")
                .email("user@different.org")
                .password("password")
                .enabled(true)
                .build();
        realmsResouce().realm(bc.providerRealmName()).users().create(user).close();

        openIdentityFirstLoginPage(user.getEmail(), true, idpRep.getAlias(), false, true);

        loginOrgIdp(user.getEmail(), user.getEmail(),true, true);

        UserRepresentation account = getUserRepresentation(user.getEmail());
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        // the flow now changed and the user should be automatically redirected to the origin broker
        openIdentityFirstLoginPage(user.getEmail(), true, null, false, false);
        loginOrgIdp(user.getEmail(), user.getEmail(),false, true);
    }

    @Test
    public void testAllowUpdateEmailWithDifferentDomainThanOrgIfBrokerHasNoDomainSet() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        String email = bc.getUserEmail();
        assertBrokerRegistration(organization, bc.getUserLogin(), email);

        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);
        UserRepresentation user = getUserRepresentation(email);
        user.setEmail("user@someother.com");
        testRealm().users().get(user.getId()).update(user);
    }

    @Test
    public void testFailUpdateEmailWithDifferentDomainThanOrgIfBrokerHasDomainSet() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        String email = bc.getUserEmail();
        assertBrokerRegistration(organization, bc.getUserLogin(), email);
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        assertEquals(email.substring(email.indexOf('@') + 1), idpRep.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE));
        UserRepresentation user = getUserRepresentation(email);
        user.setEmail("user@someother.com");
        try {
            testRealm().users().get(user.getId()).update(user);
            fail("invalid email domain");
        } catch (BadRequestException expected) {

        }
    }

    @Test
    public void testRememberOrganizationWhenReloadingLoginPage() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        OrganizationRepresentation org1 = organization.toRepresentation();
        IdentityProviderRepresentation orgIdp = organization.identityProviders().getIdentityProviders().get(0);
        orgIdp.setHideOnLogin(false);
        orgIdp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().get(orgIdp.getAlias()).update(orgIdp);

        IdentityProviderRepresentation realmIdp = bc.setUpIdentityProvider();
        realmIdp.setAlias("second-idp");
        realmIdp.setInternalId(null);
        realmIdp.setHideOnLogin(false);
        testRealm().identityProviders().create(realmIdp).close();
        getCleanup().addCleanup(testRealm().identityProviders().get("second-idp")::remove);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("test@" + org1.getDomains().iterator().next().getName());
        // only org idp
        assertTrue(loginPage.isSocialButtonPresent(orgIdp.getAlias()));
        assertFalse(loginPage.isSocialButtonPresent(realmIdp.getAlias()));

        driver.navigate().refresh();
        // still only org idp
        assertTrue(loginPage.isSocialButtonPresent(orgIdp.getAlias()));
        assertFalse(loginPage.isSocialButtonPresent(realmIdp.getAlias()));

        driver.navigate().back();
        // chrome requires refresh, otherwise Sign in button is not active
        driver.navigate().refresh();

        loginPage.loginUsername("test");
        // both realm and org idps because the user does not map to any organization
        assertTrue(loginPage.isSocialButtonPresent(orgIdp.getAlias()));
        assertTrue(loginPage.isSocialButtonPresent(realmIdp.getAlias()));

        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("test@" + org1.getDomains().iterator().next().getName());
        // only org idp
        assertTrue(loginPage.isSocialButtonPresent(orgIdp.getAlias()));
        assertFalse(loginPage.isSocialButtonPresent(realmIdp.getAlias()));

        String org2Name = "org-2";
        OrganizationResource org2 = testRealm().organizations().get(createOrganization(org2Name).getId());
        IdentityProviderRepresentation org2Idp = org2.identityProviders().getIdentityProviders().get(0);
        org2Idp.setHideOnLogin(false);
        org2Idp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        testRealm().identityProviders().get(org2Idp.getAlias()).update(org2Idp);
        driver.navigate().back();
        loginPage.loginUsername("test@" + org2.toRepresentation().getDomains().iterator().next().getName());
        // resolves to brokers from another organization
        assertFalse(loginPage.isSocialButtonPresent(orgIdp.getAlias()));
        assertFalse(loginPage.isSocialButtonPresent(realmIdp.getAlias()));
        assertTrue(loginPage.isSocialButtonPresent(org2Idp.getAlias()));
    }

    private void assertIsNotMember(String userEmail, OrganizationResource organization) {
        UsersResource users = adminClient.realm(bc.consumerRealmName()).users();
        List<UserRepresentation> reps = users.searchByEmail(userEmail, true);

        if (reps.isEmpty()) {
            return;
        }

        assertEquals(1, reps.size());
        UserRepresentation account = reps.get(0);

        try {
            assertNull(organization.members().member(account.getId()).toRepresentation());
        } catch (NotFoundException ignore) {
        }
    }

    private void createUserInConsumerRealm() {
        // create a realm user in the consumer realm
        try (Response response = realmsResouce().realm(bc.consumerRealmName()).users()
                .create(UserBuilder.create()
                        .username(bc.getUserLogin())
                        .email(bc.getUserEmail())
                        .password(bc.getUserPassword())
                        .enabled(true).build())) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            getCleanup(bc.consumerRealmName()).addUserId(id);
        }
    }
}
