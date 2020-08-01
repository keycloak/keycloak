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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.DeleteAccountPage;
import org.keycloak.testsuite.ui.account2.page.WelcomeScreen;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @author Zakaria Amine <zakaria.amine88@gmail.com>
 */
public class DeleteAccountTest extends BaseAccountPageTest {

  @Page
  private DeleteAccountPage deleteAccountPage;

  @Page
  private WelcomeScreen welcomeScreen;

  @Rule
  public AssertEvents events = new AssertEvents(this);

  @Override
  protected AbstractLoggedInPage getAccountPage() {
    return deleteAccountPage;
  }

  @Override
  public void navigateBeforeTest() {
    super.navigateBeforeTest();
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
    List<WebElement> accountLinks = driver.findElements(By.cssSelector("div[id^=\"landing-\"]"));
   Assert.assertTrue(accountLinks.stream().noneMatch(link -> link.getAttribute("id").contains("delete-account")));
    deleteAccountPage.navigateTo();
    pageNotFound.assertCurrent();
    //reset role back since realm is shared among tests
    disableDeleteAccountRequiredAction();
  }


  @Test
  public void deleteOwnAccountPageNotVisibleAndNotAccessibleWithoutDeleteAccountActionEnabled() {
    addDeleteAccountRoleToUserClientRoles();
    List<WebElement>  accountLinks = driver.findElements(By.cssSelector("div[id^=\"landing-\"]"));
    Assert.assertTrue(accountLinks.stream().noneMatch(link -> link.getAttribute("id").contains("delete-account")));
    deleteAccountPage.navigateTo();
    pageNotFound.assertCurrent();
    removeDeleteAccountRoleToUserClientRoles();
  }

  @Test
  public void deleteOwnAccountAIACancellationSucceeds() {
    enableDeleteAccountRequiredAction();
    addDeleteAccountRoleToUserClientRoles();
    deleteAccountPage.navigateTo();
    deleteAccountPage.getDeleteAccountButton().click();
    loginToAccount();
    URI uri = URI.create(driver.getCurrentUrl());
    Assert.assertTrue(uri.getPath().contains("login-actions"));
    Assert.assertTrue(uri.getQuery().contains("execution=delete_account"));
    driver.findElement(By.cssSelector("button[name='cancel-aia']")).click();
    Assert.assertTrue(deleteAccountPage.isCurrent());
    disableDeleteAccountRequiredAction();
    removeDeleteAccountRoleToUserClientRoles();
  }

  @Test
  public void deleteOwnAccountForbiddenWithoutDeleteAccountActionEnabled() {
    enableDeleteAccountRequiredAction();
    addDeleteAccountRoleToUserClientRoles();
    deleteAccountPage.navigateTo();
    deleteAccountPage.getDeleteAccountButton().click();
    loginToAccount();
    disableDeleteAccountRequiredAction();
    driver.findElement(By.cssSelector("input[type='submit']")).click();
    Assert.assertEquals(driver.findElements(By.cssSelector(".alert-error")).size(), 1);
    Assert.assertEquals(driver.findElement(By.cssSelector("#kc-content-wrapper > div > span.kc-feedback-text")).getText(), "You do not have enough permissions to delete your own account, contact admin.");
    removeDeleteAccountRoleToUserClientRoles();
  }

  @Test
  public void deleteOwnAccountSucceeds() {
    String userId = createUserToBeDeleted("test-user-to-be-deleted@localhost", "password");
    enableDeleteAccountRequiredAction();
    addDeleteAccountRoleToUserClientRoles(userId);
    addDeleteAccountRoleToUserClientRoles();
    deleteAccountPage.navigateTo();
    deleteAccountPage.header().clickLogoutBtn();
    welcomeScreen.header().clickLoginBtn();
    loginPage.form().login("test-user-to-be-deleted@localhost", "password");
    deleteAccountPage.navigateTo();
    Assert.assertTrue(deleteAccountPage.isCurrent());
    deleteAccountPage.getDeleteAccountButton().click();
    driver.findElement(By.id("username")).sendKeys("test-user-to-be-deleted@localhost");
    driver.findElement(By.id("password")).sendKeys("password");
    driver.findElement(By.id("kc-login")).click();
    driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    driver.findElement(By.cssSelector("input[type='submit']")).click();
    events.expectAccount(EventType.DELETE_ACCOUNT);
    Assert.assertTrue(testRealmResource().users().search("test-user-to-be-deleted@localhost").isEmpty());
    disableDeleteAccountRequiredAction();
    //no need to clean account role, user is deleted
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


  private void addDeleteAccountRoleToUserClientRoles() {
    createDeleteAccountRoleIfNotExists();
    ApiUtil.assignClientRoles(testRealmResource(), testUser.getId(), "account",AccountRoles.DELETE_ACCOUNT);
  }

  private void addDeleteAccountRoleToUserClientRoles(String userId) {
    createDeleteAccountRoleIfNotExists();
    ApiUtil.assignClientRoles(testRealmResource(), userId, "account",AccountRoles.DELETE_ACCOUNT);
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
