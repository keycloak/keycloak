package org.keycloak.testsuite.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class UpdateAccountInformationPage extends LanguageComboboxAwarePage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    public void updateAccountInformation(String userName,
                                         String email,
                                         String firstName,
                                         String lastName) {
        usernameInput.clear();
        usernameInput.sendKeys(userName);

        emailInput.clear();
        emailInput.sendKeys(email);

        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);

        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);

        submitButton.click();
    }

    public void updateAccountInformation(String email,
                                         String firstName,
                                         String lastName) {
        emailInput.clear();
        emailInput.sendKeys(email);

        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);

        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);

        submitButton.click();
    }

    public void updateAccountInformation(String firstName,
                                         String lastName) {
        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);

        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);

        submitButton.click();
    }

    @Override
    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).equalsIgnoreCase("update account information");
    }

    @Override
    public void open() throws Exception {

    }
}
