package org.keycloak.testsuite.auth.page.account;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class AccountFields extends Form {

    @FindBy(id = "username")
    private WebElement usernameInput;
    @FindBy(id = "email")
    private WebElement emailInput;
    @FindBy(id = "firstName")
    private WebElement firstNameInput;
    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    public void setUsername(String username) {
        Form.setInputValue(usernameInput, username);
    }

    public AccountFields setEmail(String email) {
        Form.setInputValue(emailInput, email);
        return this;
    }

    public AccountFields setFirstName(String firstName) {
        Form.setInputValue(firstNameInput, firstName);
        return this;
    }

    public AccountFields setLastName(String lastName) {
        Form.setInputValue(lastNameInput, lastName);
        return this;
    }

    public void setValues(UserRepresentation user) {
        setUsername(user.getUsername());
        setEmail(user.getEmail());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
    }

    public void waitForUsernameInputPresent() {
        waitUntilElement(usernameInput).is().present();
    }

    public void waitForUsernameInputNotPresent() {
        waitUntilElement(usernameInput).is().not().present();
    }

}
