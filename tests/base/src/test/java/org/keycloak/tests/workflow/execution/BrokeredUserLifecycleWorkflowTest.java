package org.keycloak.tests.workflow.execution;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.DeleteUserStepProviderFactory;
import org.keycloak.models.workflow.DisableUserStepProviderFactory;
import org.keycloak.models.workflow.NotifyUserStepProviderFactory;
import org.keycloak.models.workflow.ResourceOperationType;
import org.keycloak.models.workflow.conditions.IdentityProviderWorkflowConditionFactory;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.workflows.StepExecutionStatus;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStateRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
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
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.workflow.ResourceOperationType.USER_AUTHENTICATED;
import static org.keycloak.models.workflow.ResourceOperationType.USER_CREATED;
import static org.keycloak.tests.workflow.util.EmailTestUtils.findEmailByRecipient;
import static org.keycloak.tests.workflow.util.EmailTestUtils.verifyEmailContent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the usage of workflows to manage the lifecycle of brokered users. The idea is to track the user's activity through
 * user-authenticated events, and deactivate or even delete the user after a certain period of inactivity.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class BrokeredUserLifecycleWorkflowTest extends AbstractWorkflowTest {

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
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ConsentPage consentPage;

    @InjectMailServer
    private MailServer mailServer;

    private static final String REALM_PROV_NAME = "provider";
    private static final String REALM_CONS_NAME = "consumer";

    private static final String IDP_OIDC_ALIAS = "kc-oidc-idp";
    private static final String IDP_OIDC_PROVIDER_ID = "keycloak-oidc";
    private static final String IDP_CONDITION = IdentityProviderWorkflowConditionFactory.ID + "(" + IDP_OIDC_ALIAS + ")";

    private static final String CLIENT_ID = "brokerapp";
    private static final String CLIENT_SECRET = "secret";

    @Test
    public void testWorkflowToManageBrokeredUserLifecycle() {

        // create a workflow that notifies inactive users after 7 days, disables them 30 days after that if the user doesn't
        // log back in, and finally deletes them also 30 days after being disabled.
        consumerRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_AUTHENTICATED.toString())
                .onCondition(IDP_CONDITION)
                .concurrency().restartInProgress("true")
                .withSteps(
                        WorkflowStepRepresentation.create().of(NotifyUserStepProviderFactory.ID)
                                .after(Duration.ofDays(3))
                                .withConfig("reason", "inactivity")
                                .build(),
                        WorkflowStepRepresentation.create().of(DisableUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build(),
                        WorkflowStepRepresentation.create().of(DeleteUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        // login as alice - this should trigger the workflow execution for her user
        loginBrokeredUser();

        UsersResource users = consumerRealm.admin().users();
        String username = aliceFromProviderRealm.getUsername();
        UserRepresentation federatedUser = users.search(username).get(0);
        List<FederatedIdentityRepresentation> federatedIdentities = users.get(federatedUser.getId()).getFederatedIdentity();
        assertFalse(federatedIdentities.isEmpty());

        // check that alice is associated with the workflow
        assertScheduledWorkflows(federatedUser.getId(), NotifyUserStepProviderFactory.ID, 3);

        // simulate 8 days passing - alice should be notified but still enabled
        runScheduledSteps(Duration.ofDays(4));

        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "alice@wonderland.org");
        assertNotNull(testUserMessage, "No email found for alice@wonderland.org");
        verifyEmailContent(testUserMessage, "alice@wonderland.org", "Disable", "Alice", "10", "inactivity");

        federatedUser = users.get(federatedUser.getId()).toRepresentation();
        assertThat(federatedUser.isEnabled(), is(true));
        assertScheduledWorkflows(federatedUser.getId(), DisableUserStepProviderFactory.ID, 2); // next scheduled step is disable

        // advance more time - alice should now be disabled
        runScheduledSteps(Duration.ofDays(15));
        federatedUser = users.get(federatedUser.getId()).toRepresentation();
        assertThat(federatedUser.isEnabled(), is(false));
        assertScheduledWorkflows(federatedUser.getId(), DeleteUserStepProviderFactory.ID, 1);

        // let's re-enable alice and then login again to simulate her coming back
        federatedUser.setEnabled(true);
        users.get(federatedUser.getId()).update(federatedUser);
        consumerRealmOAuth.openLoginForm();

        // workflow should have been restarted due to reauthentication
        assertScheduledWorkflows(federatedUser.getId(), NotifyUserStepProviderFactory.ID, 3);

        // now simulate the workflow to its end - alice should be deleted
        runScheduledSteps(Duration.ofDays(5)); // notify
        runScheduledSteps(Duration.ofDays(10)); // disable
        runScheduledSteps(Duration.ofDays(10)); // delete

        runOnServer.run((session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, username);
            assertNull(user);
        }));
    }

    @Test
    public void testNonBrokeredUserNotAffectedByWorkflow() {
        // create a workflow that deletes inactive users after 10 days.
        consumerRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_AUTHENTICATED.toString())
                .onCondition(IDP_CONDITION)
                .withSteps(
                        WorkflowStepRepresentation.create().of(DeleteUserStepProviderFactory.ID)
                                .after(Duration.ofDays(10))
                                .build()
                ).build()).close();

        // authenticate with bob directly in the consumer realm - he is not associated with the IDP and thus not influenced
        // by the idp-exclusive lifecycle workflow.
        consumerRealmOAuth.openLoginForm();
        loginPage.fillLogin(bobFromConsumerRealm.getUsername(), bobFromConsumerRealm.getPassword());
        loginPage.submit();
        assertTrue(driver.page().getPageSource().contains("Happy days"), "Test user should be successfully logged in.");

        List<WorkflowRepresentation> scheduledWorkflows = consumerRealm.admin().workflows().getScheduledWorkflows(bobFromConsumerRealm.getId());
        assertThat(scheduledWorkflows, hasSize(0));
    }

    @Test
    public void testInvalidateWorkflowOnIdentityProviderRemoval() {
        String workflowId;
        try (Response response = consumerRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(USER_CREATED.toString(), USER_AUTHENTICATED.toString())
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

    /**
     * Tests a somewhat different workflow that activates when a user links a federated identity from a specific IDP, and
     * that is cancelled if the identity is removed. The idea is to delete users that link that specific IDP after a certain period
     * of time unless they unlink the IDP before that.
     */
    @Test
    public void testWorkflowBasedOnFederatedIdentityMembership() {

        // create a workflow that deletes users 1 day after a federated identity is added, and that is cancelled if the identity is removed
        consumerRealm.admin().workflows().create(WorkflowRepresentation.withName("myworkflow")
                .onEvent(ResourceOperationType.USER_FEDERATED_IDENTITY_ADDED.name() + "(" + IDP_OIDC_ALIAS + ")")
                .concurrency().cancelInProgress(ResourceOperationType.USER_FEDERATED_IDENTITY_REMOVED.name() + "(" + IDP_OIDC_ALIAS + ")")
                .withSteps(
                        WorkflowStepRepresentation.create().of(DeleteUserStepProviderFactory.ID)
                                .after(Duration.ofDays(1))
                                .build()
                ).build()).close();

        loginBrokeredUser();

        UserRepresentation aliceInConsumerRealm = consumerRealm.admin().users().search(aliceFromProviderRealm.getUsername()).get(0);
        assertNotNull(aliceInConsumerRealm);
        assertScheduledWorkflows(aliceInConsumerRealm.getId(), DeleteUserStepProviderFactory.ID, 1);

        // remove the federated identity - alice should be disassociated from the workflow and thus not deleted
        consumerRealm.admin().users().get(aliceInConsumerRealm.getId()).removeFederatedIdentity(IDP_OIDC_ALIAS);

        // run with a time offset - alice should not be deleted as she is no longer associated with the IDP and thus the workflow
        List<WorkflowRepresentation> workflows = consumerRealm.admin().workflows().getScheduledWorkflows(aliceInConsumerRealm.getId());
        assertThat(workflows, empty());

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

    private void assertScheduledWorkflows(String resourceId, String nextScheduledStep, int expectedScheduledSteps) {
        List<WorkflowRepresentation> scheduledWorkflows = consumerRealm.admin().workflows().getScheduledWorkflows(resourceId);
        assertThat(scheduledWorkflows, hasSize(1));
        WorkflowRepresentation scheduledWorkflow = scheduledWorkflows.get(0);
        assertThat(scheduledWorkflow.getName(), is("myworkflow"));

        // get only the steps that are still pending - the expected scheduled steps
        List<WorkflowStepRepresentation> steps = scheduledWorkflow.getSteps().stream()
                .filter(step -> StepExecutionStatus.PENDING.equals(step.getExecutionStatus())).toList();
        assertThat(steps, hasSize(expectedScheduledSteps));
        if (nextScheduledStep != null) {
            assertThat(steps.get(0).getUses(), is(nextScheduledStep));
        }
    }

    private void loginBrokeredUser() {
        consumerRealmOAuth.openLoginForm();
        loginPage.clickSocial(IDP_OIDC_ALIAS);

        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"), "Driver should be on the provider realm page right now");
        loginPage.fillLogin(aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getPassword());
        loginPage.submit();
        consentPage.assertCurrent();
        consentPage.confirm();
        assertTrue(driver.page().getPageSource().contains("Happy days"), "Test user should be successfully logged in.");
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
