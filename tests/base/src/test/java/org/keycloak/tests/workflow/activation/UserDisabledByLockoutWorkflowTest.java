package org.keycloak.tests.workflow.activation;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.SetUserAttributeStepProviderFactory;
import org.keycloak.models.workflow.events.UserDisabledByPermanentLockoutWorkflowEventFactory;
import org.keycloak.models.workflow.events.UserDisabledByTemporaryLockoutWorkflowEventFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.representations.workflows.WorkflowStepRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm.RealmUpdate;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.tests.workflow.AbstractWorkflowTest;
import org.keycloak.tests.workflow.config.WorkflowsBlockingServerConfig;

import org.awaitility.Awaitility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests that a workflow is triggered when a user is locked out by brute force protection, both for temporary and
 * permanent lockout.
 */
@KeycloakIntegrationTest(config = WorkflowsBlockingServerConfig.class)
public class UserDisabledByLockoutWorkflowTest extends AbstractWorkflowTest {

    private static final String LOCKOUT_USER = "lockoutuser";
    private static final String LOCKOUT_ATTRIBUTE = "lockout_triggered";
    private static final int MAX_LOGIN_FAILURES = 2;

    @InjectUser(ref = LOCKOUT_USER, config = UserDisabledByLockoutWorkflowTest.DefaultUserConfig.class, lifecycle = LifeCycle.METHOD, realmRef = DEFAULT_REALM_NAME)
    private ManagedUser user;

    static Stream<Arguments> lockoutProvider() {
        RealmUpdate temporaryLockout = realm -> realm
                // Brute Force Mode: Lockout temporarily
                .bruteForceProtected(true)
                .permanentLockout(false)
                .maxTemporaryLockouts(0)
                .failureFactor(MAX_LOGIN_FAILURES)
                .maxDeltaTimeSeconds((int) TimeUnit.HOURS.toSeconds(12))
                .maxSecondaryAuthFailures(0)
                .bruteForceStrategy(RealmRepresentation.BruteForceStrategy.MULTIPLE)
                .waitIncrementSeconds((int) TimeUnit.MINUTES.toSeconds(1))
                .maxFailureWaitSeconds((int) TimeUnit.MINUTES.toSeconds(15))
                .quickLoginCheckMilliSeconds(500L)
                .minimumQuickLoginWaitSeconds((int) TimeUnit.MINUTES.toSeconds(1));

        RealmUpdate permanentLockout = realm -> realm
                // Brute Force Mode: Lockout permanently
                .bruteForceProtected(true)
                .permanentLockout(true)
                .failureFactor(MAX_LOGIN_FAILURES);

        return Stream.of(
                Arguments.of("temporary lockout", temporaryLockout, UserDisabledByTemporaryLockoutWorkflowEventFactory.ID),
                Arguments.of("permanent lockout", permanentLockout, UserDisabledByPermanentLockoutWorkflowEventFactory.ID)
        );
    }

    @ParameterizedTest(name = "{0} triggers workflow")
    @MethodSource("lockoutProvider")
    void testUserDisabledByLockoutTriggersWorkflow(String name, RealmUpdate bruteForceConfig, String eventId) {
        // configure brute force protection
        managedRealm.updateWithCleanup(bruteForceConfig);

        // create workflow
        managedRealm.admin().workflows().create(WorkflowRepresentation.withName("lockout-workflow")
                .onEvent(eventId)
                .withSteps(
                        WorkflowStepRepresentation.create().of(SetUserAttributeStepProviderFactory.ID)
                                .withConfig(LOCKOUT_ATTRIBUTE, "true")
                                .build()
                ).build()).close();

        for (int i = 0; i <= MAX_LOGIN_FAILURES; i++) {
            oauth.realm(managedRealm.getName());
            oauth.openLoginForm();
            oauth.fillLoginForm(LOCKOUT_USER, "wrong-password");
        }

        Awaitility.await()
            .pollInterval(1, TimeUnit.SECONDS)
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> runOnServer.run(session -> {
                UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), LOCKOUT_USER);
                assertThat("User should exist", user, notNullValue());
                assertThat("Lockout attribute should be set by workflow",
                    user.getAttributes().get(LOCKOUT_ATTRIBUTE), notNullValue());
                assertThat(user.getAttributes().get(LOCKOUT_ATTRIBUTE).get(0), is("true"));
            }));
    }

    private static class DefaultUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            user.username(LOCKOUT_USER);
            user.password("password");
            user.name("Lockout", "User");
            user.email(String.format("%s@email.com", LOCKOUT_USER));
            return user;
        }
    }
}
