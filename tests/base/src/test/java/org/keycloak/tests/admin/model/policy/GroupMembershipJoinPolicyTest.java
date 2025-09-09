package org.keycloak.tests.admin.model.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResourcePolicies;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.EventBasedResourcePolicyProviderFactory;
import org.keycloak.models.policy.NotifyUserActionProviderFactory;
import org.keycloak.models.policy.UserSessionRefreshTimeResourcePolicyProviderFactory;
import org.keycloak.models.policy.SetUserAttributeActionProviderFactory;
import org.keycloak.models.policy.conditions.GroupMembershipPolicyConditionFactory;
import org.keycloak.models.policy.ResourceOperationType;
import org.keycloak.models.policy.ResourcePolicyManager;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyConditionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
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

    @Test
    public void testEventsOnGroupMembershipJoin() {
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);
        managedRealm.admin().users().userProfile().update(upConfig);
        String groupId;

        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("generic-group").build())) {
            groupId = ApiUtil.getCreatedId(response);
        }

        List<ResourcePolicyRepresentation> expectedPolicies = ResourcePolicyRepresentation.create()
                .of(EventBasedResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.GROUP_MEMBERSHIP_JOIN.name())
                .onConditions(ResourcePolicyConditionRepresentation.create()
                        .of(GroupMembershipPolicyConditionFactory.ID)
                        .withConfig(GroupMembershipPolicyConditionFactory.EXPECTED_GROUPS, groupId)
                        .build())
                .withActions(
                        ResourcePolicyActionRepresentation.create()
                                .of(SetUserAttributeActionProviderFactory.ID)
                                .withConfig("attribute", "attr1")
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

        UserResource userResource = managedRealm.admin().users().get(userId);

        userResource.joinGroup(groupId);

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

        UserRepresentation rep = userResource.toRepresentation();
        assertNotNull(rep.getAttributes().get("attribute"));
    }

    @Test
    public void testRemoveAssociatedGroup() {
        String groupId;

        try (Response response = managedRealm.admin().groups().add(GroupConfigBuilder.create()
                .name("generic-group").build())) {
            groupId = ApiUtil.getCreatedId(response);
        }

        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(UserSessionRefreshTimeResourcePolicyProviderFactory.ID)
                .onEvent(ResourceOperationType.LOGIN.toString())
                .onConditions(ResourcePolicyConditionRepresentation.create()
                        .of(GroupMembershipPolicyConditionFactory.ID)
                        .withConfig(GroupMembershipPolicyConditionFactory.EXPECTED_GROUPS, groupId)
                        .build())
                .withActions(
                        ResourcePolicyActionRepresentation.create().of(NotifyUserActionProviderFactory.ID)
                                .after(Duration.ofDays(1))
                                .build()
                ).build()).close();

        List<ResourcePolicyRepresentation> policies = managedRealm.admin().resources().policies().list();
        assertThat(policies, hasSize(1));

        ResourcePolicyRepresentation policyRep = managedRealm.admin().resources().policies().policy(policies.get(0).getId()).toRepresentation();
        assertThat(policyRep.getConfig().getFirst("enabled"), nullValue());

        // remove group
        managedRealm.admin().groups().group(groupId).remove();

        // create new user - it will trigger an activation event and therefore should disable the policy
        managedRealm.admin().users().create(UserConfigBuilder.create().username("test").build()).close();

        // check the policy is disabled
        policyRep = managedRealm.admin().resources().policies().policy(policies.get(0).getId()).toRepresentation();
        assertThat(policyRep.getConfig().getFirst("enabled"), allOf(notNullValue(), is("false")));
        List<String> validationErrors = policyRep.getConfig().get("validation_error");
        assertThat(validationErrors, notNullValue());
        assertThat(validationErrors, hasSize(1));
        assertThat(validationErrors.get(0), containsString("Group with id %s does not exist.".formatted(groupId)));
    }

    private static RealmModel configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        session.getContext().setRealm(realm);
        return realm;
    }
}
