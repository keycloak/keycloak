package org.keycloak.tests.admin.user;

import java.io.IOException;
import java.util.List;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.VerifyEmailPage;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class UserRequiredActionsTest extends AbstractUserTest {

    @InjectMailServer
    MailServer mailServer;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    VerifyEmailPage verifyEmailPage;

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
    public void addVerifyEmailResetsEmailVerified() {
        String id = createUser(UserBuilder.create().username("user1").email("user1@localhost").emailVerified(true).build());

        UserResource user = managedRealm.admin().users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        assertTrue(userRep.isEmailVerified());

        // The representation still says emailVerified=true, as it does when saved from the admin console
        userRep.getRequiredActions().add(UserModel.RequiredAction.VERIFY_EMAIL.toString());
        updateUser(user, userRep);

        userRep = user.toRepresentation();
        assertFalse(userRep.isEmailVerified());
        assertEquals(List.of(UserModel.RequiredAction.VERIFY_EMAIL.toString()), userRep.getRequiredActions());
    }

    @Test
    public void setEmailVerifiedWhileVerifyEmailIsPending() {
        String id = createUser(UserBuilder.create().username("user1").email("user1@localhost")
                .requiredActions(UserModel.RequiredAction.VERIFY_EMAIL.toString()).build());

        UserResource user = managedRealm.admin().users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        assertFalse(userRep.isEmailVerified());

        userRep.setEmailVerified(true);
        updateUser(user, userRep);

        userRep = user.toRepresentation();
        assertTrue(userRep.isEmailVerified());
        assertEquals(List.of(UserModel.RequiredAction.VERIFY_EMAIL.toString()), userRep.getRequiredActions());
    }

    @Test
    public void addOtherRequiredActionKeepsEmailVerified() {
        String id = createUser(UserBuilder.create().username("user1").email("user1@localhost").emailVerified(true).build());

        UserResource user = managedRealm.admin().users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        updateUser(user, userRep);

        assertTrue(user.toRepresentation().isEmailVerified());
    }

    @Test
    public void addDisabledVerifyEmailKeepsEmailVerified() {
        String verifyEmailAlias = UserModel.RequiredAction.VERIFY_EMAIL.toString();
        RequiredActionProviderRepresentation verifyEmail = managedRealm.admin().flows().getRequiredAction(verifyEmailAlias);
        verifyEmail.setEnabled(false);
        managedRealm.admin().flows().updateRequiredAction(verifyEmailAlias, verifyEmail);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authRequiredActionPath(verifyEmailAlias), verifyEmail, ResourceType.REQUIRED_ACTION);
        managedRealm.cleanup().add(r -> {
            RequiredActionProviderRepresentation rep = r.flows().getRequiredAction(verifyEmailAlias);
            rep.setEnabled(true);
            r.flows().updateRequiredAction(verifyEmailAlias, rep);
        });

        String id = createUser(UserBuilder.create().username("user1").email("user1@localhost").emailVerified(true).build());

        UserResource user = managedRealm.admin().users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add(verifyEmailAlias);
        updateUser(user, userRep);

        userRep = user.toRepresentation();
        assertTrue(userRep.isEmailVerified());
        assertEquals(List.of(verifyEmailAlias), userRep.getRequiredActions());
    }

    @Test
    public void addVerifyEmailToVerifiedUserRequiresVerificationAtLogin() throws IOException {
        String id = createUser(UserBuilder.create().username("user1").name("User", "One").email("user1@localhost")
                .emailVerified(true).password("password").build());

        UserResource user = managedRealm.admin().users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add(UserModel.RequiredAction.VERIFY_EMAIL.toString());
        updateUser(user, userRep);

        oauth.openLoginForm();
        loginPage.fillLogin("user1", "password");
        loginPage.submit();

        verifyEmailPage.assertCurrent();
        assertEquals(1, mailServer.getReceivedMessages().length);
        assertFalse(user.toRepresentation().isEmailVerified());

        driver.open(MailUtils.getPasswordResetEmailLink(mailServer.getLastReceivedMessage()));

        userRep = user.toRepresentation();
        assertTrue(userRep.isEmailVerified());
        assertTrue(userRep.getRequiredActions().isEmpty());
    }
}
