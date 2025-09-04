package org.keycloak.tests.admin.model.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.keycloak.tests.admin.model.policy.ResourcePolicyManagementTest.findEmailByRecipient;

import java.time.Duration;
import java.util.List;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResourcePolicies;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.EventBasedResourcePolicyProviderFactory;
import org.keycloak.models.policy.conditions.GroupMembershipPolicyConditionFactory;
import org.keycloak.models.policy.NotifyUserActionProviderFactory;
import org.keycloak.models.policy.ResourceOperationType;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyConditionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;

@KeycloakIntegrationTest(config = RLMServerConfig.class)
public class GroupMembershipJoinPolicyTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testEventsOnGroupMembershipJoin() {
        String groupId;

        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("generic-group").build())) {
            groupId = ApiUtil.getCreatedId(response);
        }

        List<ResourcePolicyRepresentation> expectedPolicies = ResourcePolicyRepresentation.create()
                .of(EventBasedResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.GROUP_MEMBERSHIP_JOIN.name())
                .onCoditions(ResourcePolicyConditionRepresentation.create()
                        .of(GroupMembershipPolicyConditionFactory.ID)
                        .withConfig(GroupMembershipPolicyConditionFactory.EXPECTED_GROUPS, groupId)
                        .build())
                .withActions(
                        ResourcePolicyActionRepresentation.create()
                                .of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(5))
                                .build()
                ).build();

        RealmResourcePolicies policies = managedRealm.admin().resources().policies();

        try (Response response = policies.create(expectedPolicies)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        String userId;

        try (Response response = managedRealm.admin().users().create(UserConfigBuilder.create()
                .username("generic-user").email("generic-user@example.com").build())) {
            userId = ApiUtil.getCreatedId(response);
        }

        managedRealm.admin().users().get(userId).joinGroup(groupId);

        runOnServer.run((session -> {
            RealmModel realm = configureSessionContext(session);
            ResourcePolicyManager manager = new ResourcePolicyManager(session);

            UserModel user = session.users().getUserById(realm, userId);

            try {
                // set offset to 7 days - notify action should run now
                Time.setOffset(Math.toIntExact(Duration.ofDays(6).toSeconds()));
                manager.runScheduledActions();
            } finally {
                Time.setOffset(0);
            }
        }));

        // Verify that the notify action was executed by checking email was sent
        MimeMessage testUserMessage = findEmailByRecipient(mailServer, "generic-user@example.com");
        assertNotNull(testUserMessage, "The first action (notify) should have sent an email.");

        mailServer.runCleanup();
    }

    private static RealmModel configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        session.getContext().setRealm(realm);
        return realm;
    }
}
