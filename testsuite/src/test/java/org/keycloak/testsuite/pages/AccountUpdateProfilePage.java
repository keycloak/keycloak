package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class AccountUpdateProfilePage extends Page {

    private static String PATH = Constants.AUTH_SERVER_ROOT + "/rest/realms/test/account";

    @WebResource
    private WebDriver browser;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(css = "button[type=\"submit\"]")
    private WebElement submitButton;

    public void updateProfile(String firstName, String lastName, String email) {
        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);
        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);
        emailInput.clear();
        emailInput.sendKeys(email);

        submitButton.click();
    }

    public String getFirstName() {
        return firstNameInput.getAttribute("value");
    }

    public String getLastName() {
        return lastNameInput.getAttribute("value");
    }

    public String getEmail() {
        return emailInput.getAttribute("value");
    }

    public boolean isCurrent() {
        return browser.getPageSource().contains("Edit Account");
    }

    public void open() {
        browser.navigate().to(PATH);
    }

}
