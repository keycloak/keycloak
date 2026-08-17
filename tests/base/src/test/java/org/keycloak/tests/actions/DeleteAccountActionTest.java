package org.keycloak.tests.actions;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.requiredactions.DeleteAccount;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.LegacyRealmConfig;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.auth.page.login.DeleteAccountActionConfirmPage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

@KeycloakIntegrationTest
public class DeleteAccountActionTest extends AbstractActionsTest {

    @InjectRealm(config = DeleteAccountActionRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

  @InjectPage
  public DeleteAccountActionConfirmPage deleteAccountPage;

  @InjectPage
  protected LoginPage loginPage;

  @InjectPage
  protected ErrorPage errorPage;

  @BeforeEach
  public void setUpAction() {
    UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
    UserBuilder.update(user).requiredActions(DeleteAccount.PROVIDER_ID);
    managedRealm.admin().users().get(user.getId()).update(user);
    addDeleteAccountRoleToUserClientRoles();

    RequiredActionProviderRepresentation rep = managedRealm.admin().flows().getRequiredAction(DeleteAccount.PROVIDER_ID);
    rep.setEnabled(true);
    adminClient.realm("test").flows().updateRequiredAction(DeleteAccount.PROVIDER_ID, rep);
  }

  @Test
  public void deleteAccountActionSucceeds() {
    oauth.openLoginForm();

    loginPage.login("test-user@localhost", "password");

    Assertions.assertTrue(deleteAccountPage.isCurrent());

    deleteAccountPage.clickConfirmAction();

    EventAssertion.assertSuccess(events.poll()).type(EventType.DELETE_ACCOUNT);

    List<UserRepresentation> users = managedRealm.admin().users().search("test-user@localhost");

    Assertions.assertEquals(users.size(), 0);
  }

    @Test
    public void testReauthenticateAfterDeletingAccount() {
        oauth.openLoginForm();

        UserRepresentation userRep = UserBuilder.create()
                .username("delete-user")
                .password("password")
                .enabled(true)
                .requiredActions(DeleteAccount.PROVIDER_ID)
                .build();
        managedRealm.admin().users().create(userRep).close();
        addDeleteAccountRoleToUserClientRoles(userRep.getUsername());

        loginPage.login(userRep.getUsername(), "password");

        Assertions.assertTrue(deleteAccountPage.isCurrent());

        Cookie authSessionCookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        deleteAccountPage.clickConfirmAction();

        EventAssertion.assertSuccess(events.poll()).type(EventType.DELETE_ACCOUNT);

        List<UserRepresentation> users = managedRealm.admin().users().search(userRep.getUsername());

        Assertions.assertEquals(users.size(), 0);

        managedRealm.admin().users().create(userRep).close();
        addDeleteAccountRoleToUserClientRoles(userRep.getUsername());
        oauth.openLoginForm();
        Cookie newAuthSessionCookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        Assertions.assertFalse(authSessionCookie.getValue().equals(newAuthSessionCookie.getValue()));
        loginPage.login(userRep.getUsername(), "password");
        Assertions.assertTrue(deleteAccountPage.isCurrent());
        deleteAccountPage.clickConfirmAction();
        users = managedRealm.admin().users().search(userRep.getUsername());
        Assertions.assertEquals(users.size(), 0);
    }

  @Test
  public void deleteAccountFailsWithoutRoleFails() {
    removeDeleteAccountRoleFromUserClientRoles();
    oauth.openLoginForm();

    loginPage.login("test-user@localhost", "password");

    errorPage.assertCurrent();

    Assertions.assertEquals(errorPage.getError(), "You do not have enough permissions to delete your own account, contact admin.");
  }


  private void addDeleteAccountRoleToUserClientRoles() {
    addDeleteAccountRoleToUserClientRoles("test-user@localhost");
  }

  private void addDeleteAccountRoleToUserClientRoles(String username) {
    UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, username);
    AdminApiUtil.assignClientRoles(adminClient.realm("test"), user.getId(), "account", AccountRoles.DELETE_ACCOUNT);
  }

  private void removeDeleteAccountRoleFromUserClientRoles() {
    UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
    UserResource userResource = managedRealm.admin().users().get(user.getId());
    ClientRepresentation clientRepresentation = managedRealm.admin().clients().findByClientId("account").get(0);
    String deleteRoleId = userResource.roles().clientLevel(clientRepresentation.getId()).listAll().stream().filter(role -> Objects
        .equals(role.getName(), "delete-account")).findFirst().get().getId();
    RoleRepresentation deleteRole = new RoleRepresentation();
    deleteRole.setName("delete-account");
    deleteRole.setId(deleteRoleId);
    userResource.roles().clientLevel(clientRepresentation.getId()).remove(Arrays.asList(deleteRole));
  }

    private static class DeleteAccountActionRealmConfig extends LegacyRealmConfig {

      @Override
      public void configureTestRealm(RealmRepresentation testRealm) {
      }
    }
}
