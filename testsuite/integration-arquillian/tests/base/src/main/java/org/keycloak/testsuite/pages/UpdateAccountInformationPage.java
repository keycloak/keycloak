package org.keycloak.testsuite.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

public class UpdateAccountInformationPage extends LanguageComboboxAwarePage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "department")
    private WebElement departmentInput;
    
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

        clickLink(submitButton);
    }
    
    public void updateAccountInformation(String userName,
                                         String email,
                                         String firstName,
                                         String lastName, 
                                         String department) {
        usernameInput.clear();
        usernameInput.sendKeys(userName);
        
        emailInput.clear();
        emailInput.sendKeys(email);
        
        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);
        
        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);

        departmentInput.clear();
        departmentInput.sendKeys(department);
        
        clickLink(submitButton);
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

        clickLink(submitButton);
    }

    public void updateAccountInformation(String firstName,
                                         String lastName) {
        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);

        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);

        clickLink(submitButton);
    }

    @Override
    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).equalsIgnoreCase("update account information");
    }
    
    public String getLabelForField(String fieldId) {
        return driver.findElement(By.cssSelector("label[for="+fieldId+"]")).getText();
    }
    
    public boolean isDepartmentPresent() {
        try {
            return driver.findElement(By.id("department")).isDisplayed();
        } catch (NoSuchElementException nse) {
            return false;
        }
    }

    @Override
    public void open() throws Exception {

    }
}
