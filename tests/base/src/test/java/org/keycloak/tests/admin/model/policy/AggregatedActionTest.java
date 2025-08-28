package org.keycloak.tests.admin.model.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.keycloak.models.policy.AggregatedActionProviderFactory.CONFIG_ACTION_PROVIDER_IDS;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.AggregatedActionProviderFactory;
import org.keycloak.models.policy.DisableUserActionProviderFactory;
import org.keycloak.models.policy.NotifyUserActionProviderFactory;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.models.policy.UserCreationTimeResourcePolicyProviderFactory;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

@KeycloakIntegrationTest(config = RLMServerConfig.class)
public class AggregatedActionTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectRealm
    ManagedRealm managedRealm;

    @Test
    public void testActionRun() {
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(AggregatedActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .withConfig(CONFIG_ACTION_PROVIDER_IDS, List.of(NotifyUserActionProviderFactory.ID, DisableUserActionProviderFactory.ID))
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(
                this.getUserRepresentation("alice", "Alice", "Wonderland", "alice@wornderland.org"));

        // test running the scheduled actions
        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            try {
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledTasks();
                UserModel user = session.users().getUserByUsername(realm, "alice");
                assertNotNull(user.getAttributes().get("message"));
                assertFalse(user.isEnabled());
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
