package org.keycloak.testsuite.auth.page.login;

import org.keycloak.representations.idm.UserRepresentation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class UpdateAccountFields {

    @FindBy(id = "email")
    private WebElement emailInput;
    @FindBy(id = "firstName")
    private WebElement firstNameInput;
    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    public UpdateAccountFields setEmail(String email) {
        emailInput.clear();
        emailInput.sendKeys(email);
        return this;
    }
    public UpdateAccountFields setFirstName(String firstName) {
        firstNameInput.clear();
        firstNameInput.sendKeys(firstName);
        return this;
    }
    public UpdateAccountFields setLastName(String lastName) {
        lastNameInput.clear();
        lastNameInput.sendKeys(lastName);
        return this;
    }
    
    public void setValues(String email, String firstName, String lastName) {
        setEmail(email);
        setFirstName(firstName);
        setLastName(lastName);
    }
    
    public void setValues(UserRepresentation user) {
        setValues(user.getEmail(), 
                user.getFirstName(), user.getLastName());
    }
    
}
