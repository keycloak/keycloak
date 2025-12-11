package org.keycloak.testsuite.actions;

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
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.login.DeleteAccountActionConfirmPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.UserBuilder;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Cookie;

public class DeleteAccountActionTest extends AbstractTestRealmKeycloakTest {

  @Rule
  public AssertEvents events = new AssertEvents(this);

  @Page
  public DeleteAccountActionConfirmPage deleteAccountPage;

  @Page
  protected LoginPage loginPage;

  @Page
  protected ErrorPage errorPage;

  @Override
  public void configureTestRealm(RealmRepresentation testRealm) {
  }

  @Before
  public void setUpAction() {
    UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
    UserBuilder.edit(user).requiredAction(DeleteAccount.PROVIDER_ID);
    testRealm().users().get(user.getId()).update(user);
    addDeleteAccountRoleToUserClientRoles();

    RequiredActionProviderRepresentation rep = testRealm().flows().getRequiredAction(DeleteAccount.PROVIDER_ID);
    rep.setEnabled(true);
    adminClient.realm("test").flows().updateRequiredAction(DeleteAccount.PROVIDER_ID, rep);
  }

  @Test
  public void deleteAccountActionSucceeds() {
    loginPage.open();

    loginPage.login("test-user@localhost", "password");

    Assert.assertTrue(deleteAccountPage.isCurrent());

    deleteAccountPage.clickConfirmAction();

    events.expect(EventType.DELETE_ACCOUNT);

    List<UserRepresentation> users = testRealm().users().search("test-user@localhost");

    Assert.assertEquals(users.size(), 0);
  }

    @Test
    public void testReauthenticateAfterDeletingAccount() {
        loginPage.open();

        UserRepresentation userRep = UserBuilder.create()
                .username("delete-user")
                .password("password")
                .enabled(true)
                .requiredAction(DeleteAccount.PROVIDER_ID)
                .build();
        testRealm().users().create(userRep).close();
        addDeleteAccountRoleToUserClientRoles(userRep.getUsername());

        loginPage.login(userRep.getUsername(), "password");

        Assert.assertTrue(deleteAccountPage.isCurrent());

        Cookie authSessionCookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        deleteAccountPage.clickConfirmAction();

        events.expect(EventType.DELETE_ACCOUNT);

        List<UserRepresentation> users = testRealm().users().search(userRep.getUsername());

        Assert.assertEquals(users.size(), 0);

        testRealm().users().create(userRep).close();
        addDeleteAccountRoleToUserClientRoles(userRep.getUsername());
        loginPage.open();
        Cookie newAuthSessionCookie = driver.manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        Assert.assertFalse(authSessionCookie.getValue().equals(newAuthSessionCookie.getValue()));
        loginPage.login(userRep.getUsername(), "password");
        Assert.assertTrue(deleteAccountPage.isCurrent());
        deleteAccountPage.clickConfirmAction();
        users = testRealm().users().search(userRep.getUsername());
        Assert.assertEquals(users.size(), 0);
    }

  @Test
  public void deleteAccountFailsWithoutRoleFails() {
    removeDeleteAccountRoleFromUserClientRoles();
    loginPage.open();

    loginPage.login("test-user@localhost", "password");

    Assert.assertTrue(errorPage.isCurrent());

    Assert.assertEquals(errorPage.getError(), "You do not have enough permissions to delete your own account, contact admin.");
  }


  private void addDeleteAccountRoleToUserClientRoles() {
    addDeleteAccountRoleToUserClientRoles("test-user@localhost");
  }

  private void addDeleteAccountRoleToUserClientRoles(String username) {
    UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, username);
    ApiUtil.assignClientRoles(adminClient.realm("test"), user.getId(), "account", AccountRoles.DELETE_ACCOUNT);
  }

  private void removeDeleteAccountRoleFromUserClientRoles() {
    UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
    UserResource userResource = testRealm().users().get(user.getId());
    ClientRepresentation clientRepresentation = testRealm().clients().findByClientId("account").get(0);
    String deleteRoleId = userResource.roles().clientLevel(clientRepresentation.getId()).listAll().stream().filter(role -> Objects
        .equals(role.getName(), "delete-account")).findFirst().get().getId();
    RoleRepresentation deleteRole = new RoleRepresentation();
    deleteRole.setName("delete-account");
    deleteRole.setId(deleteRoleId);
    userResource.roles().clientLevel(clientRepresentation.getId()).remove(Arrays.asList(deleteRole));
  }
}
