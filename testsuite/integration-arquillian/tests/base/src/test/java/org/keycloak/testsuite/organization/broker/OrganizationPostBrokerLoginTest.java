/*
 * Copyright 2025 Red Hat, Inc.
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

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.broker.AbstractBrokerTest;
import org.keycloak.testsuite.broker.AbstractInitializedBaseBrokerTest;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.util.AccountHelper;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests interaction between organization linked identity provider (redirect on email domain match + hide on login)
 * and post-broker login flow which requires OTP setup. Ensures that after OTP is configured the user WILL be
 * redirected to the identity provider on subsequent logins when organization settings instruct so.
 */
public class OrganizationPostBrokerLoginTest extends AbstractInitializedBaseBrokerTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private final String ORG_ALIAS = "org-post-broker";

    @Page
    private LoginConfigTotpPage totpConfigPage;

    @Page
    private LoginTotpPage loginTotpPage;

    @Override
    protected org.keycloak.testsuite.broker.BrokerConfiguration getBrokerConfiguration() {
        return new org.keycloak.testsuite.broker.KcOidcBrokerConfiguration();
    }

    @Test
    public void testAutoRedirectAfterPostBrokerOtpSetup() throws Exception {
        final RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        RealmRepresentation consumerRealmRep = consumerRealm.toRepresentation();
        if (!Boolean.TRUE.equals(consumerRealmRep.isOrganizationsEnabled())) {
            consumerRealmRep.setOrganizationsEnabled(Boolean.TRUE);
            consumerRealm.update(consumerRealmRep);
        }

        String orgDomain = Organizations.getEmailDomain(bc.getUserEmail());

        // 1) Create organization matching the brokered user's email domain
        OrganizationRepresentation organization = new OrganizationRepresentation();
        organization.setName(ORG_ALIAS);
        organization.setAlias(ORG_ALIAS);
        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName(orgDomain);
        organization.addDomain(domain);

        try (jakarta.ws.rs.core.Response resp = consumerRealm.organizations().create(organization)) {
            resp.close();
        }
        String orgId = consumerRealm.organizations().list(-1, -1).stream()
                .filter(r -> ORG_ALIAS.equals(r.getAlias())).findFirst().get().getId();

        // 2) Link existing identity provider to organization and set options: hide on login and redirect when email domain matches
        IdentityProviderRepresentation idp = consumerRealm.identityProviders().get(bc.getIDPAlias()).toRepresentation();
        idp.setHideOnLogin(false);
        consumerRealm.identityProviders().get(bc.getIDPAlias()).update(idp);

        // 3) Configure post-broker login flow for this idp to require OTP setup
        String idpAlias = bc.getIDPAlias();
        testingClient.server(bc.consumerRealmName()).run(new ConfigureOtpPostBrokerFlow(idpAlias));

        // Disable update profile prompts so we can reach the post-broker OTP flow directly
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        // 4) First login via broker, run post-broker OTP setup
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        // Post broker flow should require TOTPs
        totpConfigPage.assertCurrent();
        String totpSecret = totpConfigPage.getTotpSecret();
        totpConfigPage.configure(totp.generateTOTP(totpSecret));
        UsersResource usersApi = realmsResouce().realm(bc.consumerRealmName()).users();
        List<UserRepresentation> users = usersApi.search(bc.getUserLogin(), true);
        assertThat(1, equalTo(users.size()));
        UserRepresentation user = users.get(0);
        List<CredentialRepresentation> credentials = usersApi.get(user.getId()).credentials();
        assertThat("Expected exactly one credential after TOTP setup", credentials.size(), equalTo(1));
        assertThat("Expected TOTP credential type after setup", credentials.get(0).getType(), equalTo(OTPCredentialModel.TYPE));

        IdentityProviderRepresentation postTotpIdp = consumerRealm.identityProviders().get(idp.getAlias()).toRepresentation();
        postTotpIdp.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, orgDomain);
        postTotpIdp.getConfig().put(OrganizationModel.IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString());
        postTotpIdp.setHideOnLogin(true);
        consumerRealm.identityProviders().get(postTotpIdp.getAlias()).update(postTotpIdp);
        consumerRealm.organizations().get(orgId).identityProviders().addIdentityProvider(postTotpIdp.getAlias()).close();

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        // 5) Try re-login: user SHOULD be automatically redirected to the identity provider
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        // submit username/email to trigger organization resolution which should redirect to the provider
        loginPage.loginUsername(bc.getUserEmail());

        // wait for provider login page (title contains "sign in to <realm>")
        waitForPage(driver, "sign in to", true);
        assertThat("Driver should be on the provider realm page right now",
                driver.getCurrentUrl(), Matchers.containsString("/auth/realms/" + bc.providerRealmName() + "/"));
        String landingUrl = driver.getCurrentUrl();
        String landingTitle = driver.getTitle();
        log.infof("Organization broker re-login landed on page title='%s' url='%s' providerRealName='%s'", landingTitle, landingUrl, bc.providerRealmName());
        loginPage.login(bc.getUserPassword());

        // provide OTP code required on re-login for the configured credential
        assertThat("Driver should be on the consumer realm page right now",
                driver.getCurrentUrl(), Matchers.containsString("/auth/realms/" + bc.consumerRealmName() + "/"));
        loginTotpPage.assertCurrent();
    }

    private static class ConfigureOtpPostBrokerFlow implements RunOnServer {
        private final String idpAlias;

        private ConfigureOtpPostBrokerFlow(String idpAlias) {
            this.idpAlias = idpAlias;
        }

        @Override
        public void run(KeycloakSession session) {
            RealmModel realm = session.getContext().getRealm();

            // Build dedicated post-broker flow that enforces OTP setup before redirect
            AuthenticationFlowModel postBrokerFlow = new AuthenticationFlowModel();
            postBrokerFlow.setAlias("post-broker");
            postBrokerFlow.setDescription("post-broker flow with OTP");
            postBrokerFlow.setProviderId("basic-flow");
            postBrokerFlow.setTopLevel(true);
            postBrokerFlow.setBuiltIn(false);
            postBrokerFlow = realm.addAuthenticationFlow(postBrokerFlow);

            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(postBrokerFlow.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
            execution.setAuthenticator("auth-otp-form");
            execution.setPriority(20);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);

            IdentityProviderModel idpModel = session.identityProviders().getByAlias(idpAlias);
            idpModel.setPostBrokerLoginFlowId(postBrokerFlow.getId());
            session.identityProviders().update(idpModel);
        }
    }
}
