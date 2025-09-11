package org.keycloak.tests.admin.model.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.EventBasedResourcePolicyProviderFactory;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.ResourceType;
import org.keycloak.models.policy.SetUserAttributeActionProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;

@KeycloakIntegrationTest(config = RLMServerConfig.class)
public class AdhocPolicyTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @Test
    public void testCreate() {
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(EventBasedResourcePolicyProviderFactory.ID)
                .withActions(ResourcePolicyActionRepresentation.create()
                        .of(SetUserAttributeActionProviderFactory.ID)
                        .withConfig("message", "message")
                        .build())
                .build()).close();

        List<ResourcePolicyRepresentation> policies = managedRealm.admin().resources().policies().list();
        assertThat(policies, hasSize(1));
        ResourcePolicyRepresentation policy = policies.get(0);
        assertThat(policy.getActions(), hasSize(1));
        ResourcePolicyActionRepresentation aggregatedAction = policy.getActions().get(0);
        assertThat(aggregatedAction.getProviderId(), is(SetUserAttributeActionProviderFactory.ID));
    }

    @Test
    public void testRunAdHocScheduledPolicy() {
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(EventBasedResourcePolicyProviderFactory.ID)
                .withActions(ResourcePolicyActionRepresentation.create()
                        .of(SetUserAttributeActionProviderFactory.ID)
                        .after(Duration.ofDays(5))
                        .withConfig("message", "message")
                        .build())
                .build()).close();

        List<ResourcePolicyRepresentation> policies = managedRealm.admin().resources().policies().list();
        assertThat(policies, hasSize(1));
        ResourcePolicyRepresentation policy = policies.get(0);

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
            String id = ApiUtil.getCreatedId(response);
            managedRealm.admin().resources().policies().policy(policy.getId()).bind(ResourceType.USERS.name(), id);
        }

        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);
            UserModel user = session.users().getUserByUsername(realm, "alice");

            manager.runScheduledActions();
            assertNull(user.getAttributes().get("message"));

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledActions();
                user = session.users().getUserByUsername(realm, "alice");
                assertNotNull(user.getAttributes().get("message"));
            } finally {
                Time.setOffset(0);
            }
        }));
    }

    @Test
    public void testRunAdHocNonScheduledPolicy() {
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(EventBasedResourcePolicyProviderFactory.ID)
                .withActions(ResourcePolicyActionRepresentation.create()
                        .of(SetUserAttributeActionProviderFactory.ID)
                        .withConfig("message", "message")
                        .build())
                .build()).close();

        List<ResourcePolicyRepresentation> policies = managedRealm.admin().resources().policies().list();
        assertThat(policies, hasSize(1));
        ResourcePolicyRepresentation policy = policies.get(0);

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
            String id = ApiUtil.getCreatedId(response);
            managedRealm.admin().resources().policies().policy(policy.getId()).bind(ResourceType.USERS.name(), id);
        }

        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);
            UserModel user = session.users().getUserByUsername(realm, "alice");

            manager.runScheduledActions();
            assertNotNull(user.getAttributes().get("message"));
        }));
    }

    @Test
    public void testRunAdHocTimedScheduledPolicy() {
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(EventBasedResourcePolicyProviderFactory.ID)
                .withActions(ResourcePolicyActionRepresentation.create()
                        .of(SetUserAttributeActionProviderFactory.ID)
                        .withConfig("message", "message")
                        .build())
                .build()).close();

        List<ResourcePolicyRepresentation> policies = managedRealm.admin().resources().policies().list();
        assertThat(policies, hasSize(1));
        ResourcePolicyRepresentation policy = policies.get(0);
        String id;

        try (Response response = managedRealm.admin().users().create(getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"))) {
            id = ApiUtil.getCreatedId(response);
            managedRealm.admin().resources().policies().policy(policy.getId()).bind(ResourceType.USERS.name(), id, Duration.ofDays(5).toMillis());
        }

        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);
            UserModel user = session.users().getUserByUsername(realm, "alice");

            manager.runScheduledActions();
            assertNull(user.getAttributes().get("message"));

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledActions();
                user = session.users().getUserByUsername(realm, "alice");
                assertNotNull(user.getAttributes().get("message"));
            } finally {
                user.removeAttribute("message");
                Time.setOffset(0);
            }
        }));

        managedRealm.admin().resources().policies().policy(policy.getId()).bind(ResourceType.USERS.name(), id, Duration.ofDays(10).toMillis());

        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);
            UserModel user = session.users().getUserByUsername(realm, "alice");

            manager.runScheduledActions();
            assertNull(user.getAttributes().get("message"));

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledActions();
                user = session.users().getUserByUsername(realm, "alice");
                assertNull(user.getAttributes().get("message"));
            } finally {
                Time.setOffset(0);
            }

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(11).toSeconds()));
                manager.runScheduledActions();
                user = session.users().getUserByUsername(realm, "alice");
                assertNotNull(user.getAttributes().get("message"));
            } finally {
                Time.setOffset(0);
            }
        }));
    }

    private static RealmModel configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        session.getContext().setRealm(realm);
        return realm;
    }

    private UserRepresentation getUserRepresentation(String username, String firstName, String lastName, String email) {
        UserRepresentation representation = new UserRepresentation();
        representation.setUsername(username);
        representation.setFirstName(firstName);
        representation.setLastName(lastName);
        representation.setEmail(email);
        representation.setEnabled(true);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        representation.setCredentials(List.of(credential));
        return representation;
    }
}
