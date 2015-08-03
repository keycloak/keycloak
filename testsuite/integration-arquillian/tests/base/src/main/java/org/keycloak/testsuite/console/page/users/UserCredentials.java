package org.keycloak.testsuite.console.page.users;

import org.keycloak.representations.idm.UserRepresentation;
import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
import static org.keycloak.testsuite.util.Users.getPasswordCredentialValueOf;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class UserCredentials extends User {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/user-credentials";
    }

    @FindBy(id = "password")
    private WebElement password;

    @FindBy(id = "confirmPassword")
    private WebElement confirmPassword;

    public void addPasswordForUser(UserRepresentation user) {
        String pass = getPasswordCredentialValueOf(user);
        password.sendKeys(pass);
        confirmPassword.sendKeys(pass);
        dangerButton.click();
        waitAjaxForElement(deleteConfirmationButton);
        deleteConfirmationButton.click();
    }

}
