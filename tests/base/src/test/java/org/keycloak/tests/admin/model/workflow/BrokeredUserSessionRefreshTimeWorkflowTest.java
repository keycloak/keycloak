/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.model.workflow;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DeleteUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.Workflow;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.models.workflow.WorkflowStateProvider;
import org.keycloak.models.workflow.conditions.IdentityProviderWorkflowConditionFactory;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStateRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ConsentPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.ApiUtil;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import static org.keycloak.models.workflow.ResourceOperationType.USER_ADDED;
import static org.keycloak.models.workflow.ResourceOperationType.USER_LOGGED_IN;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class BrokeredUserSessionRefreshTimeWorkflowTest extends AbstractWorkflowTest {

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests", realmRef = "consumer")
    RunOnServerClient runOnServer;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectUser(ref = "alice", realmRef = "provider", config = ProviderRealmUserConf.class)
    ManagedUser aliceFromProviderRealm;

    @InjectUser(ref = "bob", realmRef = "consumer", config = ConsumerRealmUserConf.class)
    ManagedUser bobFromConsumerRealm;

    @InjectOAuthClient(ref = "consumer", realmRef = "consumer")
    OAuthClient consumerRealmOAuth;

    @InjectClient(realmRef = "provider", config = ProviderRealmClientConf.class)
    ManagedClient providerRealmClient;

    @InjectWebDriver
    WebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ConsentPage consentPage;

    private static final String REALM_PROV_NAME = "provider";
    private static final String REALM_CONS_NAME = "consumer";

    private static final String IDP_OIDC_ALIAS = "kc-oidc-idp";
    private static final String IDP_OIDC_PROVIDER_ID = "keycloak-oidc";
    private static final String IDP_CONDITION = IdentityProviderWorkflowConditionFactory.ID + "(" + IDP_OIDC_ALIAS + ")";

    private static final String CLIENT_ID = "brokerapp";
    private static final String CLIENT_SECRET = "secret";

    @Test
    public void testInvalidateWorkflowOnIdentityProviderRemoval() {
        String workflowId;
        try (Response response = consumerRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_ADDED.toString(), USER_LOGGED_IN.toString())
                .onCondition(IDP_CONDITION)
                .withSteps(
                        WorkflowStepRepresentation.create().of(DeleteUserStepProviderFactory.ID)
                            .after(Duration.ofDays(1))
                            .build())
                .build())) {
            workflowId = ApiUtil.getCreatedId(response);
        }

        List<WorkflowRepresentation> workflows = consumerRealm.admin().workflows().list();
        assertThat(workflows, hasSize(1));

        WorkflowRepresentation workflowRep = consumerRealm.admin().workflows().workflow(workflowId).toRepresentation();
        assertThat(workflowRep.getConfig().getFirst("enabled"), nullValue());

        // remove IDP
        consumerRealm.admin().identityProviders().get(IDP_OIDC_ALIAS).remove();

        // create new user - it will trigger an activation event and therefore should disable the workflow
        consumerRealm.admin().users().create(UserConfigBuilder.create().username("test").build()).close();

        Awaitility.await()
                .timeout(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    var rep = consumerRealm.admin().workflows().workflow(workflowId).toRepresentation();
                    assertThat(rep.getEnabled(), allOf(notNullValue(), is(false)));
                    WorkflowStateRepresentation status = rep.getState();
                    assertThat(status, notNullValue());
                    assertThat(status.getErrors(), hasSize(1));
                    assertThat(status.getErrors().get(0), containsString("Identity provider %s does not exist.".formatted(IDP_OIDC_ALIAS)));
                });
    }

    @Test
    public void tesRunStepOnFederatedUser() {
        consumerRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_ADDED.toString(), USER_LOGGED_IN.toString())
                .onCondition(IDP_CONDITION)
                .withSteps(
                        WorkflowStepRepresentation.create().of(DeleteUserStepProviderFactory.ID)
                                .after(Duration.ofDays(1))
                                .build()
                ).build()).close();

        loginBrokeredUser();

        UsersResource users = consumerRealm.admin().users();
        String username = aliceFromProviderRealm.getUsername();
        UserRepresentation federatedUser = users.search(username).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = users.get(federatedUser.getId()).getFederatedIdentity();
        assertFalse(federatedIdentities.isEmpty());

        runScheduledSteps(Duration.ZERO);

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertNotNull(user);
            assertTrue(user.isEnabled());
        }));

        runScheduledSteps(Duration.ofDays(2));

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertNull(user);
        }));

        // now authenticate with bob directly in the consumer realm - he is not associated with the IDP and thus not influenced
        // by the idp-exclusive lifecycle workflow.
        consumerRealmOAuth.openLoginForm();
        loginPage.fillLogin(bobFromConsumerRealm.getUsername(), bobFromConsumerRealm.getPassword());
        loginPage.submit();
        assertTrue(driver.getPageSource().contains("Happy days"), "Test user should be successfully logged in.");

        // run the scheduled tasks - bob should not be affected.
        runScheduledSteps(Duration.ZERO);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "bob");
            assertNotNull(user);
            assertTrue(user.isEnabled());
        });

        // run with a time offset - bob should still not be affected.
        runScheduledSteps(Duration.ofDays(2));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "bob");
            assertNotNull(user);
        });
    }

    @Disabled("For now deactivation events is not enabled to any event")
    @Test
    public void testAddRemoveFedIdentityAffectsWorkflowAssociation() {
        consumerRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_FEDERATED_IDENTITY_ADDED.toString())
                .onCondition(IDP_CONDITION)
                .withSteps(
                        WorkflowStepRepresentation.create().of(DeleteUserStepProviderFactory.ID)
                                .after(Duration.ofDays(1))
                                .build()
                ).build()).close();

        loginBrokeredUser();

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);
            Workflow workflow = provider.getWorkflows().toList().get(0);
            UserModel alice = session.users().getUserByUsername(realm, "alice");
            assertNotNull(alice);

            // alice should be associated with the workflow
            WorkflowStateProvider stateProvider = session.getProvider(WorkflowStateProvider.class);
            WorkflowStateProvider.ScheduledStep scheduledStep = stateProvider.getScheduledStep(workflow.getId(), alice.getId());
            assertNotNull(scheduledStep, "A step should have been scheduled for the user " + alice.getUsername());
        });

        // remove the federated identity - alice should be disassociated from the workflow and thus not deleted
        UserRepresentation aliceInConsumerRealm = consumerRealm.admin().users().search(aliceFromProviderRealm.getUsername()).get(0);
        assertNotNull(aliceInConsumerRealm);
        consumerRealm.admin().users().get(aliceInConsumerRealm.getId()).removeFederatedIdentity(IDP_OIDC_ALIAS);

        // run with a time offset - alice should not be deleted as she is no longer associated with the IDP and thus the workflow
        runScheduledSteps(Duration.ofDays(2));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "alice");
            assertNotNull(user, "User alice should not be deleted as she is no longer associated with the IDP and thus the workflow.");
        });

        // add a federated identity for user bob - bob should now be associated with the workflow and thus deleted when the scheduled tasks run
        FederatedIdentityRepresentation federatedIdentityRepresentation = new FederatedIdentityRepresentation();
        federatedIdentityRepresentation.setIdentityProvider(IDP_OIDC_ALIAS);
        federatedIdentityRepresentation.setUserId("bob-federated-id");
        federatedIdentityRepresentation.setUserName("bob-federated-usewrname");
        consumerRealm.admin().users().get(bobFromConsumerRealm.getId()).addFederatedIdentity(IDP_OIDC_ALIAS, federatedIdentityRepresentation).close();

        // run with a time offset - bob should be deleted as he is now associated with the IDP and thus with the workflow
        runScheduledSteps(Duration.ofDays(2));

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "bob");
            assertNull(user);
        });

    }

    private void loginBrokeredUser() {
        consumerRealmOAuth.openLoginForm();
        loginPage.clickSocial(IDP_OIDC_ALIAS);

        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"), "Driver should be on the provider realm page right now");
        loginPage.fillLogin(aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getPassword());
        loginPage.submit();
        consentPage.waitForPage();
        consentPage.assertCurrent();
        consentPage.confirm();
        assertTrue(driver.getPageSource().contains("Happy days"), "Test user should be successfully logged in.");
    }

    private static IdentityProviderRepresentation setUpIdentityProvider() {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

        Map<String, String> config = idp.getConfig();

        config.put("clientId", CLIENT_ID);
        config.put("clientSecret", CLIENT_SECRET);
        config.put("prompt", "login");
        config.put("authorizationUrl", "http://localhost:8080/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/auth");
        config.put("tokenUrl", "http://localhost:8080/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/token");
        config.put("logoutUrl", "http://localhost:8080/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", "http://localhost:8080/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/userinfo");
        config.put("defaultScope", "email profile");
        config.put("backchannelSupported", "true");

        return idp;
    }

    private static IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(alias);
        identityProviderRepresentation.setDisplayName(providerId);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

    private static class ProviderRealmUserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder builder) {
            builder.username("alice");
            builder.password("password");
            builder.email("alice@wonderland.org");
            builder.emailVerified(true);
            builder.name("Alice", "Wonderland");
            return builder;
        }
    }

    private static class ConsumerRealmUserConf implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder builder) {
            builder.username("bob");
            builder.password("password");
            builder.email("bob@wonderland.org");
            builder.emailVerified(true);
            builder.name("Bob", "Madhatter");
            return builder;
        }
    }

    private static class ProviderRealmClientConf implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder builder) {
            builder.clientId(CLIENT_ID);
            builder.name(CLIENT_ID);
            builder.secret(CLIENT_SECRET);
            builder.consentRequired(true);
            builder.redirectUris( "http://localhost:8080/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*");
            builder.adminUrl("http://localhost:8080/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint");

            return builder;
        }
    }

    private static class ConsumerRealmConf implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder builder) {
            builder.identityProvider(setUpIdentityProvider());
            return builder;
        }
    }
}
