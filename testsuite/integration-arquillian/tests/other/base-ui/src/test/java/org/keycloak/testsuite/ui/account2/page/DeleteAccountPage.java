package org.keycloak.testsuite.ui.account2.page;

import javax.ws.rs.core.UriBuilder;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Zakaria Amine <zakaria.amine88@gmail.com>
 */
public class DeleteAccountPage extends AbstractLoggedInPage  {

  @FindBy(id = "delete-account-btn")
  private WebElement deleteButton;

  @Override
  public String getPageId() {
    return "delete-account";
  }

  public String getPath() {
    return UriBuilder.fromPath(this.getAuthRoot()).path("realms").path(TEST).path("account").path("/#/delete-account").build().toString();
  }

  public boolean isCurrent() {
    return driver.getTitle().contains("Keycloak Account Management") && driver.getCurrentUrl().split("\\?")[0].endsWith("/account/#/delete-account");
  }

  public WebElement getDeleteAccountButton() {
    return deleteButton;
  }
}
