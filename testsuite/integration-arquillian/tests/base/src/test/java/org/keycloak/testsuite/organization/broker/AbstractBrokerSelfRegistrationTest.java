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
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.pages.AppPage;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.models.OrganizationDomainModel.ANY_DOMAIN;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractBrokerSelfRegistrationTest extends AbstractOrganizationTest {

    @Test
    public void testRegistrationRedirectWhenSingleBroker() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());
    }

    @Test
    public void testLoginHintSentToBrokerWhenEnabled() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(IdentityProviderModel.LOGIN_HINT, "true");
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);

        openIdentityFirstLoginPage(bc.getUserEmail(), true, null, false,false);
        // check if the username is automatically filled
        Assertions.assertEquals(bc.getUserEmail(), loginPage.getUsername());
    }

    @Test
    public void testLoginHintSentToBrokerIfUserAlreadyAMember() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(IdentityProviderModel.LOGIN_HINT, "true");
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);
        String userId = ApiUtil.getCreatedId(managedRealm.admin().users().create(UserBuilder.create()
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
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);

        openIdentityFirstLoginPage("user@neworg.org", false, null, false, false);

        Assertions.assertTrue(loginPage.isUsernameInputPresent());
        // registration link shown
        Assertions.assertTrue(loginPage.isRegisterLinkPresent());
        // no need for password because the user does not exist
        Assertions.assertFalse(loginPage.isPasswordInputPresent());
        Assertions.assertFalse(loginPage.isSocialButtonPresent(idpRep.getAlias()));
    }

    @Test
    public void testIdentityFirstUserNotExistEmailMatchBrokerDomainAndBrokerIsPublic() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.setHideOnLogin(false);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);

        openIdentityFirstLoginPage("user@neworg.org", false, null, false, false);

        Assertions.assertEquals("Your email domain matches an organization but you don't have an account yet.", loginPage.getError());
        Assertions.assertTrue(loginPage.isUsernameInputPresent());
        Assertions.assertFalse(loginPage.isPasswordInputPresent());
        Assertions.assertTrue(loginPage.isSocialButtonPresent(idpRep.getAlias()));

        // no self-registration link because the user should register through the broker
        Assertions.assertFalse(loginPage.isRegisterLinkPresent());
    }

    @Test
    public void testIdentityFirstUserNotExistEmailMatchBrokerDomainNoPublicBroker() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);

        openIdentityFirstLoginPage("user@neworg.org", false, null, false, false);

        Assertions.assertTrue(driver.getPageSource().contains("Your email domain matches an organization but you don't have an account yet."));
        Assertions.assertTrue(loginPage.isUsernameInputPresent());
        Assertions.assertFalse(loginPage.isPasswordInputPresent());
        // self-registration link shown because there is no public broker and user can choose to register
        Assertions.assertTrue(loginPage.isRegisterLinkPresent());
    }

    @Test
    public void testDefaultAuthenticationShowsPublicOrganizationBrokers() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        OrganizationRepresentation representation = organization.toRepresentation();
        representation.addDomain(new OrganizationDomainRepresentation("other.org"));
        organization.update(representation).close();
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        // set a domain to the existing broker
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp = bc.setUpIdentityProvider();
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.setHideOnLogin(false);
        idp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        // create a second broker without a domain set
        managedRealm.admin().identityProviders().create(idp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();
        idp = organization.identityProviders().get(idp.getAlias()).toRepresentation();

        openIdentityFirstLoginPage("external@user.org", false, idp.getAlias(), false, false);

        Assertions.assertTrue(loginPage.isPasswordInputPresent());
        Assertions.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
        Assertions.assertTrue(loginPage.isSocialButtonPresent(idp.getAlias()));

        idp.setHideOnLogin(true);
        managedRealm.admin().identityProviders().get(idp.getAlias()).update(idp);
        driver.navigate().refresh();
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
        Assertions.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
        Assertions.assertFalse(loginPage.isSocialButtonPresent(idp.getAlias()));
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
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);

        openIdentityFirstLoginPage("user@neworg.org", false, null, false, false);

        Assertions.assertTrue(loginPage.isPasswordInputPresent());
    }

    @Test
    public void testRealmLevelBrokersAvailableIfEmailDoesNotMatchOrganization() {
        managedRealm.admin().organizations().get(createOrganization().getId());

        openIdentityFirstLoginPage("user", false, null, false, false);

        // check if the login page is shown
        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
        Assertions.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        IdentityProviderRepresentation idp = bc.setUpIdentityProvider();
        idp.setAlias("realm-level-idp");
        idp.setHideOnLogin(false);
        managedRealm.admin().identityProviders().create(idp).close();

        driver.navigate().refresh();

        loginPage.assertAttemptedUsernameAvailability(true);
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
        Assertions.assertTrue(loginPage.isSocialButtonPresent(idp.getAlias()));
        Assertions.assertFalse(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
    }

    @Test
    public void testLinkExistingAccount() {
        createUserInConsumerRealm();

        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        OrganizationIdentityProviderResource broker = organization.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation brokerRep = broker.toRepresentation();
        brokerRep.setHideOnLogin(false);
        brokerRep.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        managedRealm.admin().identityProviders().get(brokerRep.getAlias()).update(brokerRep);

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

        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        OrganizationIdentityProviderResource broker = organization.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation brokerRep = broker.toRepresentation();
        brokerRep.setHideOnLogin(false);
        brokerRep.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        managedRealm.admin().identityProviders().get(brokerRep.getAlias()).update(brokerRep);

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
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());

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
        OrganizationResource orgA = managedRealm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = managedRealm.admin().organizations().get(createOrganization("org-b").getId());
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
        OrganizationResource orgA = managedRealm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = managedRealm.admin().organizations().get(createOrganization("org-b").getId());
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
        OrganizationResource orgA = managedRealm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = managedRealm.admin().organizations().get(createOrganization("org-b").getId());
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
        Assertions.assertTrue(driver.getPageSource().contains("Email domain does not match any domain from the organization"));

        updateAccountInformationPage.updateAccountInformation(email, orgBEmail, "f", "l");
        // user is asked to link accounts
        idpConfirmLinkPage.assertCurrent();
    }

    @Test
    public void testRedirectUnManagedMemberAllOrganizationsScope() {
        OrganizationResource orgA = managedRealm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = managedRealm.admin().organizations().get(createOrganization("org-b").getId());
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
        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(orgAEmail);
        selectOrganizationPage.assertCurrent();
    }

    @Test
    public void testRedirectBrokerManagedMemberUsingUsernameAllOrganizationsScope() {
        OrganizationResource orgA = managedRealm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = managedRealm.admin().organizations().get(createOrganization("org-b").getId());
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
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        addMember(organization, bc.getUserLogin(), bc.getUserEmail(), "f", "l", false);
        openIdentityFirstLoginPage(bc.getUserLogin(), true, null, false, false);
    }

    @Test
    public void testShowOnlyBrokersLinkedUserInPasswordPage() {
        assertOrganizationBrokerVisibilityWhenUserIsLinkedElsewhere(false);
    }

    @Test
    public void testShowOrganizationBrokerLinkedElsewhereInPasswordPage() {
        assertOrganizationBrokerVisibilityWhenUserIsLinkedElsewhere(true);
    }

    private void assertOrganizationBrokerVisibilityWhenUserIsLinkedElsewhere(boolean showWhenLinkedElsewhere) {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        OrganizationIdentityProviderResource broker = organization.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation brokerRep = broker.toRepresentation();
        brokerRep.setHideOnLogin(false);
        brokerRep.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.FALSE.toString());
        managedRealm.admin().identityProviders().get(brokerRep.getAlias()).update(brokerRep);
        IdentityProviderRepresentation secondIdp = bc.setUpIdentityProvider();
        secondIdp.setAlias("second-idp");
        secondIdp.setInternalId(null);
        secondIdp.setHideOnLogin(false);
        secondIdp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        secondIdp.getConfig().put(OrganizationModel.SHOW_IDP_ON_LOGIN_WHEN_LINKED_ELSEWHERE, Boolean.toString(showWhenLinkedElsewhere));
        managedRealm.admin().identityProviders().create(secondIdp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(secondIdp.getAlias()).close();

        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        String email = bc.getUserEmail();
        loginPage.loginUsername(email);
        // second-idp shown because user is not linked yet to any broker
        Assertions.assertTrue(loginPage.isSocialButtonPresent(secondIdp.getAlias()));
        brokerRep.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString());
        managedRealm.admin().identityProviders().get(brokerRep.getAlias()).update(brokerRep);

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

        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        log.debug("Logging in");
        loginPage.loginUsername(email);

        Assertions.assertFalse(loginPage.isUsernameInputPresent());
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
        Assertions.assertTrue(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
        Assertions.assertEquals(showWhenLinkedElsewhere, loginPage.isSocialButtonPresent(secondIdp.getAlias()));
    }

    @Test
    public void testShowWhenLinkedElsewhereEdgeCases() {
        OrganizationResource orgA = managedRealm.admin().organizations().get(createOrganization("org-a").getId());
        OrganizationResource orgB = managedRealm.admin().organizations().get(createOrganization("org-b").getId());
        IdentityProviderRepresentation orgABroker = orgA.identityProviders().getIdentityProviders().get(0);
        orgABroker.setHideOnLogin(false);
        orgABroker.getConfig().put(OrganizationModel.SHOW_IDP_ON_LOGIN_WHEN_LINKED_ELSEWHERE, Boolean.TRUE.toString());
        orgABroker.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.FALSE.toString());
        managedRealm.admin().identityProviders().get(orgABroker.getAlias()).update(orgABroker);
        IdentityProviderRepresentation orgBBroker = orgB.identityProviders().getIdentityProviders().get(0);
        orgBBroker.setHideOnLogin(false);
        orgBBroker.getConfig().put(OrganizationModel.SHOW_IDP_ON_LOGIN_WHEN_LINKED_ELSEWHERE, Boolean.TRUE.toString());
        orgBBroker.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.FALSE.toString());
        managedRealm.admin().identityProviders().get(orgBBroker.getAlias()).update(orgBBroker);
        String hideUnknownAlias = "hide-unknown-idp-" + KeycloakModelUtils.generateId();
        IdentityProviderRepresentation hideUnknownIdp = bc.setUpIdentityProvider();
        hideUnknownIdp.setAlias(hideUnknownAlias);
        hideUnknownIdp.setInternalId(null);
        hideUnknownIdp.setHideOnLogin(false);
        hideUnknownIdp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        hideUnknownIdp.getConfig().put(OrganizationModel.SHOW_IDP_ON_LOGIN_WHEN_LINKED_ELSEWHERE, Boolean.TRUE.toString());
        hideUnknownIdp.getConfig().put(OrganizationModel.HIDE_IDP_ON_LOGIN_WHEN_ORGANIZATION_UNKNOWN, Boolean.TRUE.toString());
        managedRealm.admin().identityProviders().create(hideUnknownIdp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get(hideUnknownAlias)::remove);
        orgA.identityProviders().addIdentityProvider(hideUnknownAlias).close();
        String username = "user-" + KeycloakModelUtils.generateId();
        String unresolvedEmail = username + "@user.org";
        UserRepresentation account = UserBuilder.create().username(username).email(unresolvedEmail).password("updated-password").enabled(true).build();
        try (Response response = managedRealm.admin().users().create(account)) {
            account.setId(ApiUtil.getCreatedId(response));
        }
        UserRepresentation finalAccount = account;
        getCleanup().addCleanup(() -> managedRealm.admin().users().get(finalAccount.getId()).remove());
        FederatedIdentityRepresentation identity = new FederatedIdentityRepresentation();
        identity.setIdentityProvider(orgABroker.getAlias());
        identity.setUserId(KeycloakModelUtils.generateId());
        identity.setUserName(username);
        try (Response response = managedRealm.admin().users().get(account.getId()).addFederatedIdentity(orgABroker.getAlias(), identity)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        orgA.members().addMember(account.getId()).close();
        orgB.members().addMember(account.getId()).close();
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();
        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(username);
        Assertions.assertTrue(loginPage.isSocialButtonPresent(orgABroker.getAlias()));
        Assertions.assertTrue(loginPage.isSocialButtonPresent(orgBBroker.getAlias()));
        Assertions.assertFalse(loginPage.isSocialButtonPresent(hideUnknownAlias));
        String resolvedEmail = username + "@org-a.org";
        UserRepresentation updatedAccount = managedRealm.admin().users().get(account.getId()).toRepresentation();
        updatedAccount.setEmail(resolvedEmail);
        managedRealm.admin().users().get(account.getId()).update(updatedAccount);
        String disabledAlias = "disabled-idp-" + KeycloakModelUtils.generateId();
        IdentityProviderRepresentation disabledIdp = bc.setUpIdentityProvider();
        disabledIdp.setAlias(disabledAlias);
        disabledIdp.setInternalId(null);
        disabledIdp.setEnabled(false);
        disabledIdp.setHideOnLogin(false);
        disabledIdp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        disabledIdp.getConfig().put(OrganizationModel.SHOW_IDP_ON_LOGIN_WHEN_LINKED_ELSEWHERE, Boolean.TRUE.toString());
        managedRealm.admin().identityProviders().create(disabledIdp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get(disabledAlias)::remove);
        orgA.identityProviders().addIdentityProvider(disabledAlias).close();
        String linkOnlyAlias = "link-only-idp-" + KeycloakModelUtils.generateId();
        IdentityProviderRepresentation linkOnlyIdp = bc.setUpIdentityProvider();
        linkOnlyIdp.setAlias(linkOnlyAlias);
        linkOnlyIdp.setInternalId(null);
        linkOnlyIdp.setLinkOnly(true);
        linkOnlyIdp.setHideOnLogin(false);
        linkOnlyIdp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        linkOnlyIdp.getConfig().put(OrganizationModel.SHOW_IDP_ON_LOGIN_WHEN_LINKED_ELSEWHERE, Boolean.TRUE.toString());
        managedRealm.admin().identityProviders().create(linkOnlyIdp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get(linkOnlyAlias)::remove);
        orgA.identityProviders().addIdentityProvider(linkOnlyAlias).close();
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(resolvedEmail);
        Assertions.assertTrue(loginPage.isSocialButtonPresent(hideUnknownAlias));
        Assertions.assertFalse(loginPage.isSocialButtonPresent(disabledAlias));
        Assertions.assertFalse(loginPage.isSocialButtonPresent(linkOnlyAlias));
    }

    @Test
    public void testNoIDPRedirectWhenUserHasCredentialsSet() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.setHideOnLogin(false);
        managedRealm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);

        assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());

        // set the user's credentials
        UserRepresentation user = managedRealm.admin().users().searchByEmail(bc.getUserEmail(), true).get(0);
        AdminApiUtil.resetUserPassword(realmsResouce().realm(bc.consumerRealmName()).users().get(user.getId()), "updated-password", false);

        // logout to force the user to authenticate again
        UserRepresentation account = getUserRepresentation(bc.getUserEmail());
        realmsResouce().realm(bc.consumerRealmName()).users().get(account.getId()).logout();
        realmsResouce().realm(bc.providerRealmName()).logoutAll();

        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername(bc.getUserEmail());

        // after providing username/email, user can pick to authenticate by password or by org IDP
        Assertions.assertFalse(loginPage.isUsernameInputPresent());
        Assertions.assertTrue(loginPage.isPasswordInputPresent());
        Assertions.assertTrue(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

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
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        OrganizationIdentityProviderResource idp = organization.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation idpRep = idp.toRepresentation();
        idpRep.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        managedRealm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);

        // add the member for the first time
        assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());
        UserRepresentation member = getUserRepresentation(bc.getUserEmail());

        member.setEmail(KeycloakModelUtils.generateId() + "@user.org");

        try {
            // member has a hard link with the organization, and the email must match the domains set to the organization
            managedRealm.admin().users().get(member.getId()).update(member);
            fail("Should fail because email domain does not match any from organization");
        } catch (BadRequestException expected) {
            ErrorRepresentation error = expected.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals(UserModel.EMAIL, error.getField());
            assertEquals("Email domain does not match any domain from the organization", error.getErrorMessage());
        }

        member.setEmail(member.getEmail().replace("@user.org", "@" + organizationName + ".org"));
        managedRealm.admin().users().get(member.getId()).update(member);
    }

    @Test
    public void testDeleteManagedMember() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());

        // add the member for the first time
        assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());
        UserRepresentation member = getUserRepresentation(bc.getUserEmail());
        OrganizationMemberResource organizationMember = organization.members().member(member.getId());

        organizationMember.delete().close();

        try {
            managedRealm.admin().users().get(member.getId()).toRepresentation();
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
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().create(idp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        openIdentityFirstLoginPage(bc.getUserEmail(), true, idp.getAlias(), false, false);

        loginOrgIdp(bc.getUserEmail(), bc.getUserEmail(),true, true);

        assertIsMember(bc.getUserEmail(), organization);
        UserRepresentation user = managedRealm.admin().users().search(bc.getUserEmail()).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = managedRealm.admin().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(bc.getIDPAlias(), federatedIdentities.get(0).getIdentityProvider());
    }

    @Test
    public void testRedirectToIdentityProviderAssociatedWithOrganizationDomainCaseInsensitive() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().create(idp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        openIdentityFirstLoginPage(bc.getUserEmail().toUpperCase(), true, idp.getAlias(), false, false);

        loginOrgIdp(bc.getUserEmail().toUpperCase(), bc.getUserEmail().toUpperCase(),true, true);

        assertIsMember(bc.getUserEmail().toUpperCase(), organization);
        UserRepresentation user = managedRealm.admin().users().search(bc.getUserEmail().toUpperCase()).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = managedRealm.admin().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(bc.getIDPAlias(), federatedIdentities.get(0).getIdentityProvider());
    }

    @Test
    public void testRedirectToIdentityProviderAssociatedWithOrganizationDomainUsingAnyMatch() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, ANY_DOMAIN);
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp.setAlias("second-idp");
        idp.setInternalId(null);
        managedRealm.admin().identityProviders().create(idp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        openIdentityFirstLoginPage(bc.getUserEmail(), true, idp.getAlias(), false, false);

        loginOrgIdp(bc.getUserEmail(), bc.getUserEmail(),true, true);

        assertIsMember(bc.getUserEmail(), organization);
        UserRepresentation user = managedRealm.admin().users().search(bc.getUserEmail()).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = managedRealm.admin().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(bc.getIDPAlias(), federatedIdentities.get(0).getIdentityProvider());
    }

    @Test
    public void testRedirectToIdentityProviderAssociatedWithOrganizationDomainUsingAnyMatchCaseInsensitive() {
        String userEmail = bc.getUserEmail().toUpperCase();
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, ANY_DOMAIN);
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp.setAlias("second-idp");
        idp.setInternalId(null);
        managedRealm.admin().identityProviders().create(idp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        openIdentityFirstLoginPage(userEmail, true, idp.getAlias(), false, false);

        loginOrgIdp(userEmail, userEmail, true, true);

        assertIsMember(userEmail, organization);
        UserRepresentation user = managedRealm.admin().users().search(userEmail).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = managedRealm.admin().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(bc.getIDPAlias(), federatedIdentities.get(0).getIdentityProvider());
    }

    @Test
    public void testDoNotRedirectToIdentityProviderAssociatedWithOrganizationDomain() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.setHideOnLogin(false);
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        idp.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.FALSE.toString());
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);

        openIdentityFirstLoginPage(bc.getUserEmail(), false, idp.getAlias(), false, false);

        Assertions.assertFalse(loginPage.isPasswordInputPresent());
        Assertions.assertTrue(driver.getPageSource().contains("Your email domain matches an organization but you don't have an account yet."));
        Assertions.assertTrue(loginPage.isSocialButtonPresent(bc.getIDPAlias()));

        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, ANY_DOMAIN);
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);

        openIdentityFirstLoginPage(bc.getUserEmail(), false, idp.getAlias(), false, false);

        Assertions.assertFalse(loginPage.isPasswordInputPresent());
        Assertions.assertTrue(driver.getPageSource().contains("Your email domain matches an organization but you don't have an account yet."));
        Assertions.assertTrue(loginPage.isSocialButtonPresent(bc.getIDPAlias()));
    }

    @Test
    public void testOnlyShowBrokersAssociatedWithResolvedOrganization() {
        String org0Name = "org-0";
        OrganizationResource org0 = managedRealm.admin().organizations().get(createOrganization(org0Name).getId());
        IdentityProviderRepresentation org0Broker = org0.identityProviders().getIdentityProviders().get(0);
        org0Broker.setHideOnLogin(false);
        org0Broker.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(org0Broker.getAlias()).update(org0Broker);
        String org1Name = "org-1";
        OrganizationResource org1 = managedRealm.admin().organizations().get(createOrganization(org1Name).getId());
        IdentityProviderRepresentation org1Broker = org1.identityProviders().getIdentityProviders().get(0);
        org1Broker.setHideOnLogin(false);
        org1Broker.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        org1Broker.getConfig().remove(IdentityProviderRedirectMode.EMAIL_MATCH.getKey());
        managedRealm.admin().identityProviders().get(org1Broker.getAlias()).update(org1Broker);

        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("user@org-0.org");
        Assertions.assertTrue(driver.getPageSource().contains("Your email domain matches an organization but you don't have an account yet."));
        Assertions.assertTrue(loginPage.isSocialButtonPresent(org0Broker.getAlias()));
        Assertions.assertFalse(loginPage.isSocialButtonPresent(org1Broker.getAlias()));

        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("user@org-1.org");
        Assertions.assertTrue(driver.getPageSource().contains("Your email domain matches an organization but you don't have an account yet."));
        Assertions.assertTrue(loginPage.isSocialButtonPresent(org1Broker.getAlias()));
        Assertions.assertFalse(loginPage.isSocialButtonPresent(org0Broker.getAlias()));
    }

    @Test
    public void testDoNotShowBrokersIfOrganizationNotResolved() {
        String org0Name = "org-0";
        OrganizationResource org0 = managedRealm.admin().organizations().get(createOrganization(org0Name).getId());
        IdentityProviderRepresentation org0Broker = org0.identityProviders().getIdentityProviders().get(0);
        org0Broker.setHideOnLogin(false);
        org0Broker.getConfig().put(OrganizationModel.HIDE_IDP_ON_LOGIN_WHEN_ORGANIZATION_UNKNOWN, Boolean.TRUE.toString());
        org0Broker.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(org0Broker.getAlias()).update(org0Broker);

        // do not show if organization cannot be resolved
        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("user@unknowndomain.org");
        Assertions.assertFalse(loginPage.isSocialButtonPresent(org0Broker.getAlias()));

        // show if organization can be resolved
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("user@org-0.org");
        Assertions.assertTrue(loginPage.isSocialButtonPresent(org0Broker.getAlias()));

        // show if the config is set to false
        org0Broker.getConfig().put(OrganizationModel.HIDE_IDP_ON_LOGIN_WHEN_ORGANIZATION_UNKNOWN, Boolean.FALSE.toString());
        managedRealm.admin().identityProviders().get(org0Broker.getAlias()).update(org0Broker);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("user@unknowndomain.org");
        Assertions.assertTrue(loginPage.isSocialButtonPresent(org0Broker.getAlias()));
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("user@org-0.org");
        Assertions.assertTrue(loginPage.isSocialButtonPresent(org0Broker.getAlias()));

        // hide if hide on login is set to true
        org0Broker.setHideOnLogin(true);
        managedRealm.admin().identityProviders().get(org0Broker.getAlias()).update(org0Broker);
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("user@unknowndomain.org");
        Assertions.assertFalse(loginPage.isSocialButtonPresent(org0Broker.getAlias()));
        loginPage.open(bc.consumerRealmName());
        loginPage.loginUsername("user@org-0.org");
        Assertions.assertFalse(loginPage.isSocialButtonPresent(org0Broker.getAlias()));
    }

    @Test
    public void testLoginUsingBrokerWithoutDomain() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        // set a domain to the existing broker
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp = bc.setUpIdentityProvider();
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.setHideOnLogin(false);
        idp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        // create a second broker without a domain set
        managedRealm.admin().identityProviders().create(idp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        String email = "external@user.org";
        openIdentityFirstLoginPage(email, true, idp.getAlias(), false, true);

        loginOrgIdp("external", email, true, true);

        assertIsMember(email, organization);

        // make sure the federated identity matches the expected broker
        UserRepresentation user = managedRealm.admin().users().searchByEmail(email, true).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = managedRealm.admin().users().get(user.getId()).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        assertEquals(idp.getAlias(), federatedIdentities.get(0).getIdentityProvider());
        managedRealm.admin().users().get(user.getId()).remove();
    }

    @Test
    public void testEmailDomainDoesNotMatchBrokerDomain() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        OrganizationRepresentation representation = organization.toRepresentation();
        representation.addDomain(new OrganizationDomainRepresentation("other.org"));
        organization.update(representation).close();
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        // set a domain to the existing broker
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp = bc.setUpIdentityProvider();
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.setHideOnLogin(false);
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "other.org");
        // create a second broker without a domain set
        managedRealm.admin().identityProviders().create(idp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        String email = "external@user.org";
        openIdentityFirstLoginPage(email, true, idp.getAlias(), false, true);

        loginOrgIdp(email, email, true, false);

        Assertions.assertTrue(driver.getPageSource().contains("Email domain does not match any domain from the organization"));
        assertIsNotMember(email, organization);

        updateAccountInformationPage.updateAccountInformation("external@other.org", "external@other.org", "Firstname", "Lastname");
        appPage.assertCurrent();
        assertIsMember("external@other.org", organization);
    }

    @Test
    public void testAnyEmailFromBrokerWithoutDomainSet() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        OrganizationRepresentation representation = organization.toRepresentation();
        representation.addDomain(new OrganizationDomainRepresentation("other.org"));
        organization.update(representation).close();
        IdentityProviderRepresentation idp = organization.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, "neworg.org");
        // set a domain to the existing broker
        managedRealm.admin().identityProviders().get(bc.getIDPAlias()).update(idp);

        idp = bc.setUpIdentityProvider();
        idp.setAlias("second-idp");
        idp.setInternalId(null);
        idp.setHideOnLogin(false);
        // create a second broker without a domain set
        managedRealm.admin().identityProviders().create(idp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get("second-idp")::remove);
        organization.identityProviders().addIdentityProvider(idp.getAlias()).close();

        String email = "external@user.org";
        openIdentityFirstLoginPage(email, true, idp.getAlias(), false, true);

        loginOrgIdp(email, "external@unknown.org", true, true);
        assertIsMember("external@unknown.org", organization);
    }

    @Test
    public void testRealmLevelBrokerNotImpactedByOrganizationFlow() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idp = bc.setUpIdentityProvider();
        idp.setAlias("realm-idp");
        idp.setInternalId(null);
        idp.setHideOnLogin(false);
        // create a second broker without a domain set
        managedRealm.admin().identityProviders().create(idp).close();

        openIdentityFirstLoginPage("some@user.org", true, idp.getAlias(), true, true);

        loginOrgIdp("external", bc.getUserEmail(), true, true);

        assertThat(organization.members().list(-1, -1), Matchers.empty());

        UserRepresentation user = managedRealm.admin().users().searchByEmail(bc.getUserEmail(), true).get(0);
        managedRealm.admin().users().get(user.getId()).remove();
    }

    @Test
    public void testMemberRegistrationUsingDifferentDomainThanOrganization() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);

        // make sure the user can select this idp from the organization when authenticating
        idpRep.setHideOnLogin(false);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);

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
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);

        // make sure the user can select this idp from the organization when authenticating
        idpRep.setHideOnLogin(false);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);

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
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        String email = bc.getUserEmail();
        assertBrokerRegistration(organization, bc.getUserLogin(), email);

        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        idpRep.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(idpRep.getAlias()).update(idpRep);
        UserRepresentation user = getUserRepresentation(email);
        user.setEmail("user@someother.com");
        managedRealm.admin().users().get(user.getId()).update(user);
    }

    @Test
    public void testFailUpdateEmailWithDifferentDomainThanOrgIfBrokerHasDomainSet() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        String email = bc.getUserEmail();
        assertBrokerRegistration(organization, bc.getUserLogin(), email);
        IdentityProviderRepresentation idpRep = organization.identityProviders().getIdentityProviders().get(0);
        assertEquals(email.substring(email.indexOf('@') + 1), idpRep.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE));
        UserRepresentation user = getUserRepresentation(email);
        user.setEmail("user@someother.com");
        try {
            managedRealm.admin().users().get(user.getId()).update(user);
            fail("invalid email domain");
        } catch (BadRequestException expected) {

        }
    }

    @Test
    public void testRememberOrganizationWhenReloadingLoginPage() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        OrganizationRepresentation org1 = organization.toRepresentation();
        IdentityProviderRepresentation orgIdp = organization.identityProviders().getIdentityProviders().get(0);
        orgIdp.setHideOnLogin(false);
        orgIdp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(orgIdp.getAlias()).update(orgIdp);

        IdentityProviderRepresentation realmIdp = bc.setUpIdentityProvider();
        realmIdp.setAlias("second-idp");
        realmIdp.setInternalId(null);
        realmIdp.setHideOnLogin(false);
        managedRealm.admin().identityProviders().create(realmIdp).close();
        getCleanup().addCleanup(managedRealm.admin().identityProviders().get("second-idp")::remove);

        oauth.client("broker-app");
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
        OrganizationResource org2 = managedRealm.admin().organizations().get(createOrganization(org2Name).getId());
        IdentityProviderRepresentation org2Idp = org2.identityProviders().getIdentityProviders().get(0);
        org2Idp.setHideOnLogin(false);
        org2Idp.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        managedRealm.admin().identityProviders().get(org2Idp.getAlias()).update(org2Idp);
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
