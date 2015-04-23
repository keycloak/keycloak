package org.keycloak.testsuite.ui.page.account;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.ui.model.Account;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class AccountPage extends AbstractPage {

    @FindBy(id = "username")
    private WebElement username;

    @FindBy(id = "email")
    private WebElement email;

    @FindBy(id = "lastName")
    private WebElement lastName;

    @FindBy(id = "firstName")
    private WebElement firstName;

    @FindByJQuery("button[value='Save']")
    private WebElement save;
	
	@FindByJQuery(".nav li:eq(0) a")
    private WebElement keyclockConsole;

    @FindByJQuery(".nav li:eq(1) a")
    private WebElement signOutLink;

    @FindByJQuery(".bs-sidebar ul li:eq(0) a")
    private WebElement accountLink;

    @FindByJQuery(".bs-sidebar ul li:eq(1) a")
    private WebElement passwordLink;

    @FindByJQuery(".bs-sidebar ul li:eq(2) a")
    private WebElement authenticatorLink;

    @FindByJQuery(".bs-sidebar ul li:eq(3) a")
    private WebElement sessionsLink;


    public Account getAccount() {
        return new Account(username.getAttribute("value"), email.getAttribute("value"), lastName.getAttribute("value"), firstName.getAttribute("value"));
    }

    public void setAccount(Account account) {
        email.clear();
        email.sendKeys(account.getEmail());
        lastName.clear();
        lastName.sendKeys(account.getLastName());
        firstName.clear();
        firstName.sendKeys(account.getFirstName());
    }

    public void save() {
        save.click();
    }
	
	 public void keycloakConsole() {
        keyclockConsole.click();
    }

    public void signOut() {
        signOutLink.click();
    }

    public void account() {
        accountLink.click();
    }

    public void password() {
        passwordLink.click();
    }

    public void authenticator() {
        authenticatorLink.click();
    }

    public void sessions() {
        sessionsLink.click();
    }
}
