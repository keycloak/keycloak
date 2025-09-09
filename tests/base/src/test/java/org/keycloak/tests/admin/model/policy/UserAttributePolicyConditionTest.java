package org.keycloak.tests.admin.model.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResourcePolicies;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.EventBasedResourcePolicyProviderFactory;
import org.keycloak.models.policy.ResourceOperationType;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.SetUserAttributeActionProviderFactory;
import org.keycloak.models.policy.conditions.UserAttributePolicyConditionFactory;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyConditionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

@KeycloakIntegrationTest(config = RLMServerConfig.class)
public class UserAttributePolicyConditionTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @BeforeEach
    public void onBefore() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);
    }

    @Test
    public void testConditionForSingleValuedAttribute() {
        String expected = "valid";
        createPolicy(expected);
        assertUserAttribute("user-1", false);
        assertUserAttribute("user-2", false, "not-valid");
        assertUserAttribute("user-3", true, expected);
    }

    @Test
    public void testConditionForMultiValuedAttribute() {
        List<String> expected = List.of("v1", "v2", "v3");
        createPolicy(expected);
        assertUserAttribute("user-1", false, "v1");
        assertUserAttribute("user-2", true, expected);
        assertUserAttribute("user-3", false, "v1", "v2", "v3", "v4");
    }

    @Test
    public void testConditionForMultipleAttributes() {
        Map<String, List<String>> expected = Map.of("a", List.of("a1"), "b", List.of("b1"), "c", List.of("c11", "c2"));
        createPolicy(expected);
        assertUserAttribute("user-1", false, Map.of("a", List.of("a3"), "b", List.of("b1"), "c", List.of("c1", "c2")));
        assertUserAttribute("user-2", true, expected);
        assertUserAttribute("user-3", false, Map.of("a", List.of("a1"), "b", List.of("b1")));
        HashMap<String, List<String>> values = new HashMap<>(expected);
        values.put("d", List.of("d1"));
        assertUserAttribute("user-4", true, values);
    }

    private void assertUserAttribute(String username, boolean shouldExist, String... values) {
        assertUserAttribute(username, shouldExist, Map.of("attribute", List.of(values)));
    }

    private void assertUserAttribute(String username, boolean shouldExist, List<String> values) {
        assertUserAttribute(username, shouldExist, Map.of("attribute", values));
    }

    private void assertUserAttribute(String username, boolean shouldExist, Map<String, List<String>> attributes) {
        managedRealm.admin().users().create(UserConfigBuilder.create()
                .username(username)
                .email(username + "@example.com")
                .attributes(attributes)
                .build()).close();
        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);

            try {
                // set offset to 7 days - notify action should run now
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                new ResourcePolicyManager(session).runScheduledActions();
            } finally {
                Time.setOffset(0);
            }

            UserModel user = session.users().getUserByUsername(realm, username);
            assertNotNull(user);

            if (shouldExist) {
                assertTrue(user.getAttributes().containsKey("notified"));
            } else {
                assertFalse(user.getAttributes().containsKey("notified"));
            }
        }));
    }

    private void createPolicy(String... expectedValues) {
        createPolicy(Map.of("attribute", List.of(expectedValues)));
    }

    private void createPolicy(List<String> expectedValues) {
        createPolicy(Map.of("attribute", expectedValues));
    }

    private void createPolicy(Map<String, List<String>> attributes) {
        List<ResourcePolicyRepresentation> expectedPolicies = ResourcePolicyRepresentation.create()
                .of(EventBasedResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.CREATE.name())
                .recurring()
                .onConditions(ResourcePolicyConditionRepresentation.create()
                        .of(UserAttributePolicyConditionFactory.ID)
                        .withConfig(attributes)
                        .build())
                .withActions(
                        ResourcePolicyActionRepresentation.create()
                                .of(SetUserAttributeActionProviderFactory.ID)
                                .withConfig("notified", "true")
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        RealmResourcePolicies policies = managedRealm.admin().resources().policies();

        try (Response response = policies.create(expectedPolicies)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }
    }

    private static RealmModel configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        session.getContext().setRealm(realm);
        return realm;
    }
}
