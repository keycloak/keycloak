package org.keycloak.tests.admin.user;

import java.util.List;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.providers.actions.DummyRequiredActionFactory;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = UserRequiredActionsTest.CustomProvidersServerConfig.class)
public class UserRequiredActionsTest extends AbstractUserTest {

    // Deliberately different from DummyRequiredActionFactory.PROVIDER_ID so that the required action
    // stored on the user (a realm alias) is not equal to any registered provider factory id. This is
    // what reproduces the reported failure (#48144): the previous logic only iterated over provider
    // factory ids and therefore could neither remove nor retain an action keyed by such an alias.
    private static final String CUSTOM_ALIAS = "custom-dummy-alias";

    @Test
    public void addRequiredAction() {
        String id = createUser();

        UserResource user = managedRealm.admin().users().get(id);
        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());

        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        updateUser(user, userRep);

        assertEquals(1, user.toRepresentation().getRequiredActions().size());
        assertEquals(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), user.toRepresentation().getRequiredActions().get(0));
    }

    @Test
    public void removeRequiredAction() {
        String id = createUser();

        UserResource user = managedRealm.admin().users().get(id);
        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());

        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        updateUser(user, userRep);

        user = managedRealm.admin().users().get(id);
        userRep = user.toRepresentation();
        userRep.getRequiredActions().clear();
        updateUser(user, userRep);

        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());
    }

    @Test
    public void testDefaultRequiredActionAdded() {
        // Add UPDATE_PASSWORD as default required action
        RequiredActionProviderRepresentation updatePasswordReqAction = managedRealm.admin().flows().getRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        updatePasswordReqAction.setDefaultAction(true);
        managedRealm.admin().flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), updatePasswordReqAction);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authRequiredActionPath(UserModel.RequiredAction.UPDATE_PASSWORD.toString()), updatePasswordReqAction, ResourceType.REQUIRED_ACTION);

        // Create user
        String userId = createUser("user1", "user1@localhost");

        UserRepresentation userRep = managedRealm.admin().users().get(userId).toRepresentation();
        Assertions.assertEquals(1, userRep.getRequiredActions().size());
        Assertions.assertEquals(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), userRep.getRequiredActions().get(0));

        // Remove UPDATE_PASSWORD default action
        updatePasswordReqAction = managedRealm.admin().flows().getRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        updatePasswordReqAction.setDefaultAction(false);
        managedRealm.admin().flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), updatePasswordReqAction);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authRequiredActionPath(UserModel.RequiredAction.UPDATE_PASSWORD.toString()), updatePasswordReqAction, ResourceType.REQUIRED_ACTION);
    }

    @Test
    @DatabaseTest
    public void removeCustomRequiredAction() {
        registerDummyRequiredAction();

        String id = createUser();
        UserResource user = managedRealm.admin().users().get(id);

        // Add the custom required action (by its realm alias) to the user
        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add(CUSTOM_ALIAS);
        updateUser(user, userRep);

        userRep = user.toRepresentation();
        assertEquals(1, userRep.getRequiredActions().size());
        assertEquals(CUSTOM_ALIAS, userRep.getRequiredActions().get(0));

        // Remove the custom required action by sending an empty list
        userRep.getRequiredActions().clear();
        updateUser(user, userRep);

        userRep = user.toRepresentation();
        assertTrue(userRep.getRequiredActions().isEmpty(),
                "Custom required action should be removed but was still present: " + userRep.getRequiredActions());
    }

    @Test
    @DatabaseTest
    public void removeCustomRequiredActionKeepBuiltIn() {
        registerDummyRequiredAction();

        String id = createUser();
        UserResource user = managedRealm.admin().users().get(id);

        // Add both a built-in and custom required action
        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        userRep.getRequiredActions().add(CUSTOM_ALIAS);
        updateUser(user, userRep);

        userRep = user.toRepresentation();
        assertEquals(2, userRep.getRequiredActions().size());

        // Remove only the custom action, keep the built-in one
        userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PASSWORD.toString()));
        updateUser(user, userRep);

        userRep = user.toRepresentation();
        assertEquals(1, userRep.getRequiredActions().size());
        assertEquals(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), userRep.getRequiredActions().get(0));
    }

    @Test
    @DatabaseTest
    public void keepCustomRequiredActionWithDistinctAlias() {
        registerDummyRequiredAction();

        String id = createUser();
        UserResource user = managedRealm.admin().users().get(id);

        // Add a built-in and a custom action whose alias differs from its provider id
        UserRepresentation userRep = user.toRepresentation();
        userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), CUSTOM_ALIAS));
        updateUser(user, userRep);

        userRep = user.toRepresentation();
        assertEquals(2, userRep.getRequiredActions().size());

        // Re-send both actions; the alias-keyed custom action must be retained, not silently dropped
        userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PASSWORD.toString(), CUSTOM_ALIAS));
        updateUser(user, userRep);

        userRep = user.toRepresentation();
        assertEquals(2, userRep.getRequiredActions().size(),
                "Custom required action with a distinct alias should be retained but was: " + userRep.getRequiredActions());
        assertTrue(userRep.getRequiredActions().contains(CUSTOM_ALIAS),
                "Custom required action should be retained but was dropped: " + userRep.getRequiredActions());
        assertTrue(userRep.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_PASSWORD.toString()));
    }

    private void registerDummyRequiredAction() {
        RequiredActionProviderSimpleRepresentation action = managedRealm.admin().flows().getUnregisteredRequiredActions()
                .stream()
                .filter(a -> a.getProviderId().equals(DummyRequiredActionFactory.PROVIDER_ID))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dummy required action not found"));
        managedRealm.admin().flows().registerRequiredAction(action);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE,
                AdminEventPaths.authMgmtBasePath() + "/register-required-action", action, ResourceType.REQUIRED_ACTION);

        // Rename the alias so it no longer matches the provider factory id. Required-action values
        // stored on a user are realm aliases, so this makes the tests exercise the alias/provider-id
        // distinction rather than the trivial case where the two happen to be equal.
        RequiredActionProviderRepresentation registered = managedRealm.admin().flows().getRequiredAction(DummyRequiredActionFactory.PROVIDER_ID);
        registered.setAlias(CUSTOM_ALIAS);
        managedRealm.admin().flows().updateRequiredAction(DummyRequiredActionFactory.PROVIDER_ID, registered);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE,
                AdminEventPaths.authRequiredActionPath(DummyRequiredActionFactory.PROVIDER_ID), registered, ResourceType.REQUIRED_ACTION);

        managedRealm.cleanup().add(r -> {
            try {
                r.flows().removeRequiredAction(CUSTOM_ALIAS);
            } catch (jakarta.ws.rs.NotFoundException ignored) {
            }
        });
    }

    public static class CustomProvidersServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }

    }
}
