package org.keycloak.testsuite.page.auth;

import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.model.User;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class UpdateAccountPage extends AdminConsole {

    @FindBy(id = "email")
    private WebElement email;

    @FindBy(id = "firstName")
    private WebElement firstName;

    @FindBy(id = "lastName")
    private WebElement lastName;

    public void updateAccountInfo(User user) {
        email.clear();
        email.sendKeys(user.getEmail());
        firstName.clear();
        firstName.sendKeys(user.getFirstName());
        lastName.clear();
        lastName.sendKeys(user.getLastName());
        primaryButton.click();
    }

    public void updateAccountInfo(String email, String firstName, String lastName) {
        User u = new User("", "", email, firstName, lastName);
        updateAccountInfo(u);
    }

}
