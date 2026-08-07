package org.keycloak.tests.workflow.condition;

import java.time.Duration;
import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.workflow.client.DeleteClientStepProviderFactory;
import org.keycloak.models.workflow.conditions.CimdOrphanClientWorkflowConditionFactory;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeCondition;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeConditionFactory;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutorFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowScheduleRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.CimdProvider;
import org.keycloak.testframework.oauth.OIDCClientRepresentationBuilder;
import org.keycloak.testframework.oauth.annotations.InjectCimdProvider;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.tests.workflow.AbstractWorkflowTest;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the CIMD orphan client workflow condition using actual CIMD authorization flows
 * to register clients, then verifying the workflow correctly identifies and deletes
 * orphaned clients based on cache expiry threshold.
 */
@KeycloakIntegrationTest(config = CimdOrphanClientWorkflowConditionTest.CimdWorkflowServerConfig.class)
public class CimdOrphanClientWorkflowConditionTest extends AbstractWorkflowTest {

    private static final String CLIENT_ID = "http://localhost:8500/cimd/metadata";
    private static final String REDIRECT_URI = "http://localhost:8500/";
    private static final int ONE_DAY_IN_SECONDS = 86400;
    // The CIMD executor enforces a minimum cache time of 300 seconds
    private static final int CIMD_MIN_CACHE_TIME_SEC = 300;

    @InjectUser(config = CimdTestUserConfig.class, realmRef = DEFAULT_REALM_NAME, lifecycle = LifeCycle.METHOD)
    protected ManagedUser user;

    @InjectCimdProvider(config = CimdPublicClientConfig.class, lifecycle = LifeCycle.METHOD)
    CimdProvider cimd;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectPage
    OAuthGrantPage grantPage;

    @BeforeEach
    public void setupCimdPolicy() {
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));

        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        executorConfig.setRestrictSameDomain(true);
        executorConfig.setAllowHttpScheme(true);

        managedRealm.updateWithCleanup(r -> {
            r.resetClientProfiles()
                    .clientProfile(ClientProfileBuilder.create()
                            .name("cimd-executor")
                            .description("CIMD executor profile")
                            .executor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, executorConfig)
                            .build());
            r.resetClientPolicies()
                    .clientPolicy(ClientPolicyBuilder.create()
                            .name("cimd-policy")
                            .description("CIMD policy")
                            .condition(ClientIdUriSchemeConditionFactory.PROVIDER_ID, conditionConfig)
                            .profile("cimd-executor")
                            .build());
            return r;
        });
    }

    @Test
    public void testOrphanedCimdClientIsDeleted() {
        // Register a CIMD client through actual authorization flow
        String registeredClientId = performCimdAuthorizationFlow(true);
        assertNotNull(registeredClientId, "CIMD client should have been registered");

        // Create a workflow that deletes orphaned CIMD clients (threshold: 1 day after cache expiry)
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("cimd-orphan-cleanup")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").build())
                .onCondition(CimdOrphanClientWorkflowConditionFactory.ID + "(" + ONE_DAY_IN_SECONDS + ")")
                .withSteps(
                        WorkflowStepRepresentation.create().of(DeleteClientStepProviderFactory.ID)
                                .build()
                ).build()).close();

        // Move time forward past cache expiry + threshold (min cache time + 1 day + buffer)
        timeOffSet.set(CIMD_MIN_CACHE_TIME_SEC + ONE_DAY_IN_SECONDS + 60);

        // Wait for the scheduled workflow to pick up and delete the orphaned client
        Awaitility.await()
                .timeout(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    runOnServer.run((RunOnServer) session -> {
                        RealmModel realm = session.getContext().getRealm();
                        ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
                        assertNull(client, "Orphaned CIMD client should have been deleted");
                    });
                });
    }

    @Test
    public void testNonOrphanedCimdClientIsNotDeleted() {
        // Register a CIMD client through actual authorization flow
        String registeredClientId = performCimdAuthorizationFlow(true);
        assertNotNull(registeredClientId, "CIMD client should have been registered");

        // Create a workflow that deletes orphaned CIMD clients (threshold: 1 day after cache expiry)
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("cimd-orphan-cleanup")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").build())
                .onCondition(CimdOrphanClientWorkflowConditionFactory.ID + "(" + ONE_DAY_IN_SECONDS + ")")
                .withSteps(
                        WorkflowStepRepresentation.create().of(DeleteClientStepProviderFactory.ID)
                                .build()
                ).build()).close();

        // Do NOT move time forward — the cache is still valid, so the client is not orphaned.
        // We expect a timeout because the client should never be scheduled for deletion.
        assertThrows(ConditionTimeoutException.class, () -> Awaitility.await()
                .timeout(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    runOnServer.run((RunOnServer) session -> {
                        RealmModel realm = session.getContext().getRealm();
                        ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
                        // Assert the opposite: if client is deleted, condition incorrectly matched
                        assertNull(client, "Client should have been deleted (but it shouldn't be)");
                    });
                }));

        // Verify client still exists
        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            assertNotNull(client, "Active CIMD client should NOT have been deleted");
        });
    }

    @Test
    public void testOrphanedClientNotDeletedBeforeThreshold() {
        // Register a CIMD client through actual authorization flow
        String registeredClientId = performCimdAuthorizationFlow(true);
        assertNotNull(registeredClientId, "CIMD client should have been registered");

        // Create a workflow with threshold of 1 day
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("cimd-orphan-cleanup")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").build())
                .onCondition(CimdOrphanClientWorkflowConditionFactory.ID + "(" + ONE_DAY_IN_SECONDS + ")")
                .withSteps(
                        WorkflowStepRepresentation.create().of(DeleteClientStepProviderFactory.ID)
                                .build()
                ).build()).close();

        // Move time past cache expiry but NOT past threshold (cache expired but less than 1 day ago)
        timeOffSet.set(CIMD_MIN_CACHE_TIME_SEC + (ONE_DAY_IN_SECONDS / 2));

        // We expect a timeout because the client hasn't exceeded the threshold yet
        assertThrows(ConditionTimeoutException.class, () -> Awaitility.await()
                .timeout(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    runOnServer.run((RunOnServer) session -> {
                        RealmModel realm = session.getContext().getRealm();
                        ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
                        // Assert the opposite: if client is deleted, condition incorrectly matched
                        assertNull(client, "Client should have been deleted (but it shouldn't be)");
                    });
                }));

        // Verify client still exists
        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientByClientId(realm, CLIENT_ID);
            assertNotNull(client, "Recently expired CIMD client should NOT have been deleted yet");
        });
    }

    @Test
    public void testNonCimdClientIsNotAffected() {
        // Register a CIMD client through actual authorization flow
        performCimdAuthorizationFlow(true);

        // Also create a regular client without CIMD attributes
        ClientRepresentation regularClient = new ClientRepresentation();
        regularClient.setName("regular-client");
        regularClient.setClientId("regular-client");
        regularClient.setProtocol("openid-connect");
        regularClient.setPublicClient(true);
        regularClient.setEnabled(true);
        String regularClientId;
        try (var response = managedRealm.admin().clients().create(regularClient)) {
            regularClientId = org.keycloak.testframework.util.ApiUtil.getCreatedId(response);
        }

        // Create a workflow that deletes orphaned CIMD clients (threshold: 1 day)
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("cimd-orphan-cleanup")
                .schedule(WorkflowScheduleRepresentation.create().after("1s").build())
                .onCondition(CimdOrphanClientWorkflowConditionFactory.ID + "(" + ONE_DAY_IN_SECONDS + ")")
                .withSteps(
                        WorkflowStepRepresentation.create().of(DeleteClientStepProviderFactory.ID)
                                .build()
                ).build()).close();

        // Move time forward past cache expiry + threshold so the CIMD client becomes orphaned
        timeOffSet.set(CIMD_MIN_CACHE_TIME_SEC + ONE_DAY_IN_SECONDS + 60);

        // Wait for the CIMD client to be deleted (proving the workflow ran)
        Awaitility.await()
                .timeout(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    runOnServer.run((RunOnServer) session -> {
                        RealmModel realm = session.getContext().getRealm();
                        ClientModel cimdClient = session.clients().getClientByClientId(realm, CLIENT_ID);
                        assertNull(cimdClient, "Orphaned CIMD client should have been deleted");
                    });
                });

        // Verify the regular client was NOT affected
        final String finalRegularClientId = regularClientId;
        runOnServer.run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            ClientModel client = session.clients().getClientById(realm, finalRegularClientId);
            assertNotNull(client, "Regular client should NOT have been deleted by CIMD orphan cleanup");
        });
    }

    /**
     * Performs a CIMD authorization code flow which triggers client registration via CIMD.
     *
     * @param isFirstLogin true if this is the first login (consent screen will appear)
     * @return the internal ID of the registered CIMD client
     */
    private String performCimdAuthorizationFlow(boolean isFirstLogin) {
        oauth.client(CLIENT_ID);
        oauth.redirectUri(REDIRECT_URI);
        oauth.loginForm().codeChallenge(null).open();
        oauth.fillLoginForm(user.getUsername(), user.getPassword());

        if (isFirstLogin) {
            grantPage.assertCurrent();
            grantPage.accept();
        }

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull(code, "Authorization code should have been returned");

        // Exchange code for token (public client, no client_secret needed)
        oauth.client(CLIENT_ID).accessTokenRequest(code).send();

        // Return the internal ID of the registered client
        List<ClientRepresentation> clients = managedRealm.admin().clients().findByClientId(CLIENT_ID);
        if (clients.isEmpty()) {
            return null;
        }
        return clients.get(0).getId();
    }

    /**
     * Server configuration enabling both WORKFLOWS (blocking mode) and CIMD features.
     */
    public static class CimdWorkflowServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                    .features(Profile.Feature.CIMD)
                    .option("spi-workflow--default--executor-blocking", Boolean.TRUE.toString());
        }
    }

    /**
     * CIMD public client metadata configuration served by the CimdProvider.
     */
    public static class CimdPublicClientConfig implements OIDCClientRepresentationBuilder {
        @Override
        public OIDCClientRepresentation build() {
            OIDCClientRepresentation client = new OIDCClientRepresentation();
            client.setClientId(CLIENT_ID);
            client.setRedirectUris(List.of(REDIRECT_URI));
            client.setClientName("cimd-test-client");
            client.setClientUri("http://localhost:8500");
            client.setTokenEndpointAuthMethod(null); // public client
            client.setGrantTypes(List.of("authorization_code", "refresh_token"));
            return client;
        }
    }

    /**
     * Simple test user for CIMD login.
     */
    public static class CimdTestUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user
                    .username("cimd-test-user")
                    .password("password")
                    .email("cimd-test@localhost")
                    .name("CIMD", "User");
        }
    }
}
