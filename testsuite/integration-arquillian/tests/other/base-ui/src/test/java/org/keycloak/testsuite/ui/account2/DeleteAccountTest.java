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

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.DeleteAccountPage;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @author Zakaria Amine <zakaria.amine88@gmail.com>
 */
public class DeleteAccountTest extends BaseAccountPageTest {

  @Page
  private DeleteAccountPage deleteAccountPage;

  @Rule
  public AssertEvents events = new AssertEvents(this);

  @Override
  protected AbstractLoggedInPage getAccountPage() {
    return deleteAccountPage;
  }

  @Before
  public void createDeleteAccountRealmRole() {
    testRealmResource().roles().deleteRole("delete-account");
    RoleRepresentation roleRepresentation = new RoleRepresentation();
    roleRepresentation.setName("delete-account");
    testRealmResource().roles().create(roleRepresentation);
  }


  @Test
  public void deleteOwnAccountPageNotVisibleAndNotAccessibleWithoutUserRole() {
    enableDeleteAccountRequiredAction();
    loginToAccount();
    List<WebElement> accountLinks = driver.findElements(By.cssSelector("div[id^=\"landing-\"]"));
    Assert.assertTrue(accountLinks.stream().noneMatch(link -> !link.getAttribute("id").contains("delete-account")));
    driver.navigate().to(deleteAccountPage.getPath());
    pageNotFound.assertCurrent();
    //reset role back since realm is shared among tests
    disableDeleteAccountRequiredAction();
  }


  @Test
  public void deleteOwnAccountPageNotVisibleAndNotAccessibleWithoutDeleteAccountActionEnabled() {
    addUserDeleteRole();
    loginToAccount();
    List<WebElement>  accountLinks = driver.findElements(By.cssSelector("div[id^=\"landing-\"]"));
    Assert.assertTrue(accountLinks.stream().noneMatch(link -> !link.getAttribute("id").contains("delete-account")));
    driver.navigate().to(deleteAccountPage.getPath());
    pageNotFound.assertCurrent();
    removeUserDeleteAccountRole();
  }

  @Test
  public void deleteOwnAccountAIACancellationSucceeds() {
    enableDeleteAccountRequiredAction();
    addUserDeleteRole();
    loginToAccount();
    driver.navigate().to(deleteAccountPage.getPath());
    deleteAccountPage.getDeleteAccountButton().click();
    Assert.assertTrue(loginPage.isCurrent());
    loginToAccount();
    URI uri = URI.create(driver.getCurrentUrl());
    Assert.assertTrue(uri.getPath().contains("login-actions"));
    Assert.assertTrue(uri.getQuery().contains("execution=delete_account"));
    driver.findElement(By.cssSelector("button[name='cancel-aia']")).click();
    Assert.assertTrue(deleteAccountPage.isCurrent());
    disableDeleteAccountRequiredAction();
    removeUserDeleteAccountRole();
  }

  @Test
  public void deleteOwnAccountForbiddenWithoutDeleteAccountActionEnabled() {
    enableDeleteAccountRequiredAction();
    addUserDeleteRole();
    loginToAccount();
    driver.navigate().to(deleteAccountPage.getPath());
    deleteAccountPage.getDeleteAccountButton().click();
    Assert.assertTrue(loginPage.isCurrent());
    loginToAccount();
    disableDeleteAccountRequiredAction();
    deleteAccountPage.getDeleteAccountButton().click();
    Assert.assertTrue(deleteAccountPage.isCurrent());
    //Assert.assertEquals(deleteAccountPage.getErrorMessage().getText(), "You do not have enough permissions to delete your own account, contact admin.");
  }

  @Test
  public void deleteOwnAccountSucceeds() {
    enableDeleteAccountRequiredAction();
    String userId = createUserToBeDeleted("test-user-to-be-deleted@localhost", "password");
    loginToAccount();
    loginPage.form().login(testRealmResource().users().get(userId).toRepresentation());
    driver.navigate().to(deleteAccountPage.getPath());
    Assert.assertTrue(deleteAccountPage.isCurrent());
    deleteAccountPage.getDeleteAccountButton().click();
    loginPage.form().login(testRealmResource().users().get(userId).toRepresentation());
    driver.findElement(By.cssSelector("input[type='submit']")).click();
    events.expectAccount(EventType.DELETE_ACCOUNT);
    Assert.assertTrue(loginPage.isCurrent());
    Assert.assertTrue(testRealmResource().users().search("test-user-to-be-deleted@localhost").isEmpty());
    disableDeleteAccountRequiredAction();
  }

  public String createUserToBeDeleted(String username, String password) {
    UserRepresentation userToBeDeleted = UserBuilder.create()
        .enabled(true)
        .username(username)
        .email(username)
        .password(password)
        .build();

    testRealmResource().users().create(userToBeDeleted);
    return testRealmResource().users().search(username).get(0).getId();
  }


  private void addUserDeleteRole() {
    RoleRepresentation deleteRole = new RoleRepresentation();
    deleteRole.setName("delete-account");
    ApiUtil.assignRealmRoles(testRealmResource(), testUser.getId(), Constants.DELETE_ACCOUNT_ROLE);
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

  private void removeUserDeleteAccountRole() {
    RoleRepresentation deleteRole = new RoleRepresentation();
    deleteRole.setName("delete-account");
    realmsResouce().realm(testRealmResource().toRepresentation().getId()).users().get(testUser.getId()).roles().realmLevel().remove(
        Arrays.asList(deleteRole));
  }
}
