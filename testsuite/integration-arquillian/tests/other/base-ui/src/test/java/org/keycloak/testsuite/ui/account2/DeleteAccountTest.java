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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.auth.page.login.DeleteAccountActionConfirmPage;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;

import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;

/**
 * @author Zakaria Amine <zakaria.amine88@gmail.com>
 */
public class DeleteAccountTest extends BaseAccountPageTest {

  @Page
  private PersonalInfoPage personalInfoPage;

  @Page
  private DeleteAccountActionConfirmPage deleteAccountActionConfirmPage;

  @Rule
  public AssertEvents events = new AssertEvents(this);

  @Override
  protected AbstractLoggedInPage getAccountPage() {
    return personalInfoPage;
  }

  @Before
  public void setup() {
    enableDeleteAccountRequiredAction();
    addDeleteAccountRoleToUserClientRoles();
  }

  @After
  public void clean() {
    disableDeleteAccountRequiredAction();
  }

  @Test
  public void deleteOwnAccountSectionNotVisibleWithoutClientRole() {
    removeDeleteAccountRoleFromUserClientRoles();
    refreshPageAndWaitForLoad();
    personalInfoPage.assertDeleteAccountSectionVisible(false);
  }


  @Test
  public void deleteOwnAccountSectionNotVisibleWithoutDeleteAccountActionEnabled() {
    disableDeleteAccountRequiredAction();
    refreshPageAndWaitForLoad();
    personalInfoPage.assertDeleteAccountSectionVisible(false);
  }

  @Test
  public void deleteOwnAccountAIACancellationSucceeds() {
    refreshPageAndWaitForLoad();
    personalInfoPage.assertDeleteAccountSectionVisible(true);
    personalInfoPage.clickOpenDeleteExapandable();
    personalInfoPage.clickDeleteAccountButton();
    loginPage.form().login(testUser);
    Assert.assertTrue(deleteAccountActionConfirmPage.isCurrent());
    deleteAccountActionConfirmPage.clickCancelAIA();
    Assert.assertTrue(personalInfoPage.isCurrent());
  }

  @Test
  public void deleteOwnAccountForbiddenWithoutClientRole() {
    refreshPageAndWaitForLoad();
    personalInfoPage.assertDeleteAccountSectionVisible(true);
    personalInfoPage.clickOpenDeleteExapandable();
    personalInfoPage.clickDeleteAccountButton();
    loginPage.form().login(testUser);
    Assert.assertTrue(deleteAccountActionConfirmPage.isCurrent());
    removeDeleteAccountRoleFromUserClientRoles();
    deleteAccountActionConfirmPage.clickConfirmAction();
    Assert.assertTrue(deleteAccountActionConfirmPage.isErrorMessageDisplayed());
    Assert.assertEquals(deleteAccountActionConfirmPage.getErrorMessageText(), "You do not have enough permissions to delete your own account, contact admin.");
  }

  @Test
  public void deleteOwnAccountSucceeds() {
    personalInfoPage.navigateTo();
    personalInfoPage.assertDeleteAccountSectionVisible(true);
    personalInfoPage.clickOpenDeleteExapandable();
    personalInfoPage.clickDeleteAccountButton();
    loginPage.form().login(testUser);
    deleteAccountActionConfirmPage.isCurrent();
    deleteAccountActionConfirmPage.clickConfirmAction();
    events.expectAccount(EventType.DELETE_ACCOUNT);
    Assert.assertTrue(testRealmResource().users().search(testUser.getUsername()).isEmpty());
  }

  private void addDeleteAccountRoleToUserClientRoles() {
    ApiUtil.assignClientRoles(testRealmResource(), testUser.getId(), "account",AccountRoles.DELETE_ACCOUNT);
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

  private void removeDeleteAccountRoleFromUserClientRoles() {
    ClientRepresentation clientRepresentation = testRealmResource().clients().findByClientId("account").get(0);
    String deleteRoleId = testUserResource().roles().clientLevel(clientRepresentation.getId()).listAll().stream().filter(role -> Objects.equals(role.getName(), "delete-account")).findFirst().get().getId();
    RoleRepresentation deleteRole = new RoleRepresentation();
    deleteRole.setName("delete-account");
    deleteRole.setId(deleteRoleId);
    testUserResource().roles().clientLevel(clientRepresentation.getId()).remove(Arrays.asList(deleteRole));
  }
}
