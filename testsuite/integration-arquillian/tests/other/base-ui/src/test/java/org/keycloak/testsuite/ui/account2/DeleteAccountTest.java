/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.ui.account2;

import java.util.Arrays;
import java.util.Objects;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.ui.account2.page.AIALoginPage;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.DeleteAccountActionConfirmPage;
import org.keycloak.testsuite.ui.account2.page.DeleteAccountPage;
import org.keycloak.testsuite.ui.account2.page.WelcomeScreen;

/**
 * @author Zakaria Amine <zakaria.amine88@gmail.com>
 */
public class DeleteAccountTest extends BaseAccountPageTest {

  @Page
  private DeleteAccountPage deleteAccountPage;

  @Page
  private WelcomeScreen welcomeScreen;

  @Page
  private AIALoginPage aiaLoginPage;

  @Page
  private DeleteAccountActionConfirmPage deleteAccountActionConfirmPage;

  @Rule
  public AssertEvents events = new AssertEvents(this);

  @Override
  protected AbstractLoggedInPage getAccountPage() {
    return deleteAccountPage;
  }

  @Override
  public void navigateBeforeTest() {
    getAccountPage().navigateTo();
    loginToAccount();
  }

  @Override
  @Test
  public void navigationTest() {
    enableDeleteAccountRequiredAction();
    addDeleteAccountRoleToUserClientRoles();
    driver.navigate().refresh();
    super.navigationTest();
    disableDeleteAccountRequiredAction();
    removeDeleteAccountRoleToUserClientRoles();
  }

  @Test
  public void deleteOwnAccountPageNotVisibleAndNotAccessibleWithoutUserRole() {
    enableDeleteAccountRequiredAction();
   Assert.assertFalse(welcomeScreen.isDeleteAccountLinkVisible());
    deleteAccountPage.navigateTo();
    pageNotFound.assertCurrent();
    //reset role back since realm is shared among tests
    disableDeleteAccountRequiredAction();
  }


  @Test
  public void deleteOwnAccountPageNotVisibleAndNotAccessibleWithoutDeleteAccountActionEnabled() {
    addDeleteAccountRoleToUserClientRoles();
    Assert.assertFalse(welcomeScreen.isDeleteAccountLinkVisible());
    deleteAccountPage.navigateTo();
    pageNotFound.assertCurrent();
    removeDeleteAccountRoleToUserClientRoles();
  }

  @Test
  public void deleteOwnAccountAIACancellationSucceeds() {
    enableDeleteAccountRequiredAction();
    addDeleteAccountRoleToUserClientRoles();
    driver.navigate().refresh();
    deleteAccountPage.navigateTo();
    deleteAccountPage.clickDeleteAccountButton();
    aiaLoginPage.form().login(testUser);
    Assert.assertTrue(deleteAccountActionConfirmPage.isCurrent());
    deleteAccountActionConfirmPage.clickCancelAIA();
    Assert.assertTrue(deleteAccountPage.isCurrent());
    disableDeleteAccountRequiredAction();
    removeDeleteAccountRoleToUserClientRoles();
  }

  @Test
  public void deleteOwnAccountForbiddenWithoutDeleteAccountActionEnabled() {
    enableDeleteAccountRequiredAction();
    addDeleteAccountRoleToUserClientRoles();
    deleteAccountPage.navigateTo();
    deleteAccountPage.clickDeleteAccountButton();
    aiaLoginPage.form().login(testUser);
    Assert.assertTrue(deleteAccountActionConfirmPage.isCurrent());
    disableDeleteAccountRequiredAction();
    deleteAccountActionConfirmPage.clickConfirmAction();
    Assert.assertTrue(deleteAccountActionConfirmPage.isErrorMessageDisplayed());
    Assert.assertEquals(deleteAccountActionConfirmPage.getErrorMessageText(), "You do not have enough permissions to delete your own account, contact admin.");
    removeDeleteAccountRoleToUserClientRoles();
  }

  @Test
  public void deleteOwnAccountSucceeds() {
    enableDeleteAccountRequiredAction();
    addDeleteAccountRoleToUserClientRoles();
    deleteAccountPage.navigateTo();
    Assert.assertTrue(deleteAccountPage.isCurrent());
    deleteAccountPage.clickDeleteAccountButton();
    Assert.assertTrue(aiaLoginPage.isCurrent());
    aiaLoginPage.form().login(testUser);
    deleteAccountActionConfirmPage.isCurrent();
    deleteAccountActionConfirmPage.clickConfirmAction();
    events.expectAccount(EventType.DELETE_ACCOUNT);
    Assert.assertTrue(testRealmResource().users().search(testUser.getUsername()).isEmpty());
    disableDeleteAccountRequiredAction();
    //no need to clean account role, user is deleted
  }

  private void addDeleteAccountRoleToUserClientRoles() {
    createDeleteAccountRoleIfNotExists();
    ApiUtil.assignClientRoles(testRealmResource(), testUser.getId(), "account",AccountRoles.DELETE_ACCOUNT);
  }

  private void createDeleteAccountRoleIfNotExists() {
    RoleRepresentation deleteRole = new RoleRepresentation();
    deleteRole.setName(AccountRoles.DELETE_ACCOUNT);
    try {
      testRealmResource().roles().create(deleteRole);
    }
    catch (Exception exp) {

    }
  }

  private void disableDeleteAccountRequiredAction() {
    RequiredActionProviderRepresentation deleteAccount = testRealmResource().flows().getRequiredAction("delete_account");
    deleteAccount.setEnabled(false);
    testRealmResource().flows().updateRequiredAction("delete_account", deleteAccount);
  }

  private void enableDeleteAccountRequiredAction() {
    RequiredActionProviderRepresentation deleteAccount = testRealmResource().flows().getRequiredAction("delete_account");
    deleteAccount.setEnabled(true);
    testRealmResource().flows().updateRequiredAction("delete_account", deleteAccount);
  }

  private void removeDeleteAccountRoleToUserClientRoles() {
    ClientRepresentation clientRepresentation = testRealmResource().clients().findByClientId("account").get(0);
    String deleteRoleId = testUserResource().roles().clientLevel(clientRepresentation.getId()).listAll().stream().filter(role -> Objects.equals(role.getName(), "delete-account")).findFirst().get().getId();
    RoleRepresentation deleteRole = new RoleRepresentation();
    deleteRole.setName("delete-account");
    deleteRole.setId(deleteRoleId);
    testUserResource().roles().clientLevel(clientRepresentation.getId()).remove(Arrays.asList(deleteRole));
  }
}
