/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.broker;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.access.AllowAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalClientScopeAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FlowUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.models.utils.TimeBasedOTP.DEFAULT_INTERVAL_SECONDS;
import static org.keycloak.testsuite.admin.ApiUtil.removeUserByUsername;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.configurePostBrokerLoginWithOTP;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KcOidcPostBrokerLoginTest extends AbstractInitializedBaseBrokerTest {

    private static final KcOidcBrokerConfiguration BROKER_CONFIG_INSTANCE = new KcOidcBrokerConfiguration();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return BROKER_CONFIG_INSTANCE;
    }

    @Before
    public void setUpTotp() {
        totp = new TimeBasedOTP();
    }


    @Test
    public void testReauthenticationSamlBrokerWithOTPRequired() throws Exception {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(samlBrokerConfig.getIDPAlias()));
            logInWithBroker(samlBrokerConfig);

            totpPage.assertCurrent();
            String totpSecret = totpPage.getTotpSecret();
            totpPage.configure(totp.generateTOTP(totpSecret));

            AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
            AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

            setOtpTimeOffset(DEFAULT_INTERVAL_SECONDS, totp);

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            idpConfirmLinkPage.assertCurrent();
            idpConfirmLinkPage.clickLinkAccount();

            loginPage.clickSocial(samlBrokerConfig.getIDPAlias());
            waitForPage(driver, "sign in to", true);
            log.debug("Logging in");
            loginTotpPage.login(totp.generateTOTP(totpSecret));

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 2);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }


    @Test
    public void testReauthenticationOIDCBrokerWithOTPRequired() throws Exception {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(samlBrokerConfig);
            AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
            AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

            testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(bc.getIDPAlias()));

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            idpConfirmLinkPage.assertCurrent();
            idpConfirmLinkPage.clickLinkAccount();
            loginPage.clickSocial(samlBrokerConfig.getIDPAlias());

            totpPage.assertCurrent();
            String totpSecret = totpPage.getTotpSecret();
            totpPage.configure(totp.generateTOTP(totpSecret));
            logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 2);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }


    @Test
    public void testReauthenticationBothBrokersWithOTPRequired() throws Exception {
        final RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        final RealmResource providerRealm = adminClient.realm(bc.providerRealmName());

        try (RealmAttributeUpdater rauConsumer = new RealmAttributeUpdater(consumerRealm).setOtpPolicyCodeReusable(true).update();
             RealmAttributeUpdater rauProvider = new RealmAttributeUpdater(providerRealm).setOtpPolicyCodeReusable(true).update()) {

            KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
            ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
            IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();

            try {
                updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
                providerRealm.clients().create(samlClient);
                consumerRealm.identityProviders().create(samlBroker);

                oauth.clientId("broker-app");
                loginPage.open(bc.consumerRealmName());

                testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(samlBrokerConfig.getIDPAlias()));
                logInWithBroker(samlBrokerConfig);
                totpPage.assertCurrent();
                String totpSecret = totpPage.getTotpSecret();
                totpPage.configure(totp.generateTOTP(totpSecret));
                AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
                AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

                testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(bc.getIDPAlias()));
                oauth.clientId("broker-app");
                loginPage.open(bc.consumerRealmName());

                logInWithBroker(bc);

                waitForPage(driver, "account already exists", false);
                idpConfirmLinkPage.assertCurrent();
                idpConfirmLinkPage.clickLinkAccount();
                loginPage.clickSocial(samlBrokerConfig.getIDPAlias());

                loginTotpPage.assertCurrent();
                loginTotpPage.login(totp.generateTOTP(totpSecret));
                AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
                AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

                oauth.clientId("broker-app");
                loginPage.open(bc.consumerRealmName());

                logInWithBroker(bc);

                loginTotpPage.assertCurrent();
                loginTotpPage.login(totp.generateTOTP(totpSecret));

                assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 2);
            } finally {
                updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
                removeUserByUsername(consumerRealm, "consumer");
            }
        }
    }


    @Test
    public void testPostBrokerLoginFlowWithOTP() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        // Setup with default client scope - OTP required
        configurePostBrokerLoginWithClientScopeConditionAndOTP(testingClient, bc.consumerRealmName(), bc.getIDPAlias(), OAuth2Constants.SCOPE_PROFILE, false);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        totpPage.assertCurrent();
        String totpSecret = totpPage.getTotpSecret();
        totpPage.configure(totp.generateTOTP(totpSecret));

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        assertNumFederatedIdentities(realm.users().search(bc.getUserLogin()).get(0).getId(), 1);

        appPage.assertCurrent();
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        // Setup with optional client scope - scope not present in scope parameter, OTP not required
        configurePostBrokerLoginWithClientScopeConditionAndOTP(testingClient, bc.consumerRealmName(), bc.getIDPAlias(), OAuth2Constants.SCOPE_PHONE, false);
        setOtpTimeOffset(DEFAULT_INTERVAL_SECONDS, totp);

        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        appPage.assertCurrent();
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        // Setup with optional client scope - scope parameter present, OTP required
        oauth.scope("openid phone");

        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        loginTotpPage.assertCurrent();
        loginTotpPage.login(totp.generateTOTP(totpSecret));

        appPage.assertCurrent();
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

        // Setup with optional client scope with negate - scope parameter present, OTP not required
        configurePostBrokerLoginWithClientScopeConditionAndOTP(testingClient, bc.consumerRealmName(), bc.getIDPAlias(), OAuth2Constants.SCOPE_PHONE, true);

        oauth.scope("openid phone");

        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        appPage.assertCurrent();
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());
    }

    static void configurePostBrokerLoginWithClientScopeConditionAndOTP(KeycloakTestingClient testingClient, String consumerRealmName, String idpAlias, String clientScopeName, boolean negate) {
        testingClient.server(consumerRealmName).run(session -> {
            AuthenticationFlowModel flowModel = session.getContext().getRealm().getFlowByAlias("post-broker");
            if (flowModel == null) {
                flowModel = FlowUtil.createFlowModel("post-broker", "basic-flow", "post-broker flow with client-scope condition and OTP", true, false);
                session.getContext().getRealm().addAuthenticationFlow(flowModel);
            }

            FlowUtil.inCurrentRealm(session)
                    // Select new flow
                    .selectFlow("post-broker")
                    .clear()
                    .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, AllowAccessAuthenticatorFactory.PROVIDER_ID)
                    .addSubFlowExecution("OTP requested when client scope", AuthenticationFlow.BASIC_FLOW, AuthenticationExecutionModel.Requirement.CONDITIONAL, (flowUtil) -> {
                        flowUtil.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ConditionalClientScopeAuthenticatorFactory.PROVIDER_ID, (configModel) -> {
                            Map<String, String> config = new HashMap<>();
                            config.put(ConditionalClientScopeAuthenticatorFactory.CLIENT_SCOPE, clientScopeName);
                            config.put(ConditionalClientScopeAuthenticatorFactory.CONF_NEGATE, String.valueOf(negate));
                            configModel.setConfig(config);
                            configModel.setAlias("condition - client scope");
                        })
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, OTPFormAuthenticatorFactory.PROVIDER_ID);
                    });
            IdentityProviderModel idp = session.identityProviders().getByAlias(idpAlias);
            idp.setPostBrokerLoginFlowId(flowModel.getId());
            session.identityProviders().update(idp);
        });
    }

}
