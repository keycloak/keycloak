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

    @Test
    @DatabaseTest
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
    @DatabaseTest
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

        // Add the custom required action to the user
        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add(DummyRequiredActionFactory.PROVIDER_ID);
        updateUser(user, userRep);

        userRep = user.toRepresentation();
        assertEquals(1, userRep.getRequiredActions().size());
        assertEquals(DummyRequiredActionFactory.PROVIDER_ID, userRep.getRequiredActions().get(0));

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
        userRep.getRequiredActions().add(DummyRequiredActionFactory.PROVIDER_ID);
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

    private void registerDummyRequiredAction() {
        RequiredActionProviderSimpleRepresentation action = managedRealm.admin().flows().getUnregisteredRequiredActions()
                .stream()
                .filter(a -> a.getProviderId().equals(DummyRequiredActionFactory.PROVIDER_ID))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Dummy required action not found"));
        managedRealm.admin().flows().registerRequiredAction(action);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE,
                AdminEventPaths.authMgmtBasePath() + "/register-required-action", action, ResourceType.REQUIRED_ACTION);

        managedRealm.cleanup().add(r -> {
            try {
                r.flows().removeRequiredAction(DummyRequiredActionFactory.PROVIDER_ID);
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
