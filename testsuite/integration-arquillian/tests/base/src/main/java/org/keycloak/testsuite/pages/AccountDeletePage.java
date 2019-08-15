package org.keycloak.testsuite.pages;

import javax.ws.rs.core.UriBuilder;
import org.keycloak.services.Urls;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class AccountDeletePage extends AbstractAccountPage {

  @FindBy(id = "delete")
  private WebElement deleteAccountButton;

  @FindBy(className = "alert-error")
  private WebElement errorMessage;

  private String realmName = "test";

  @Override
  public boolean isCurrent() {
    return driver.getTitle().contains("Account Management") && driver.getCurrentUrl().split("\\?")[0].endsWith("/account/deleteAccount");
  }

  @Override
  public void open() {
    driver.navigate().to(getPath());
  }

  public String getPath() {
    return Urls.deleteAccountPage(UriBuilder.fromUri(getAuthServerRoot()).build(), this.realmName).toString();
  }

  public WebElement getDeleteAccountButton() {
    return deleteAccountButton;
  }

  public WebElement getErrorMessage() {
    return errorMessage;
  }
}