package org.keycloak.testsuite.ui.account2.page;

import javax.ws.rs.core.UriBuilder;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author Zakaria Amine <zakaria.amine88@gmail.com>
 */
public class DeleteAccountPage extends AbstractLoggedInPage  {

  @Override
  public String getPageId() {
    return "delete-account";
  }

  @Override
  public boolean isCurrent() {
    return driver.getTitle().contains("Keycloak Account Management") && driver.getCurrentUrl().split("\\?")[0].endsWith("/account/#/delete-account");
  }

  public void clickDeleteAccountButton() {
    clickLink(driver.findElement(By.id("delete-account-btn")));
  }
}
