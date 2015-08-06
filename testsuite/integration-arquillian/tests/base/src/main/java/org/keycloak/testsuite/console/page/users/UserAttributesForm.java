package org.keycloak.testsuite.console.page.users;

import java.util.List;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.util.SeleniumUtils.waitAjaxForElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Filip Kiss
 * @author tkyjovsk
 */
public class UserAttributesForm extends Form {

    @FindBy(id = "id")
    private WebElement idInput;

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(xpath = "//div[@class='onoffswitch' and ./input[@id='userEnabled']]")
    private OnOffSwitch userEnabledSwitch;

    @FindBy(xpath = "//div[@class='onoffswitch' and ./input[@id='emailVerified']]")
    private OnOffSwitch emailVerifiedSwitch;

    @FindBy(css = "input[id='s2id_autogen3']")
    private WebElement requiredUserActionsInput;

    @FindBy(className = "select2-result-label")
    private WebElement requiredUserActionsConfirm;

    @FindBy(className = "select2-search-choice-close")
    private List<WebElement> removeRequiredActionsList;

    public String getId() {
        return idInput.getText();
    }

    public String getUsername() {
        return usernameInput.getText();
    }

    public void setUsername(String username) {
        usernameInput.clear();
        if (username != null) {
            usernameInput.sendKeys(username);
        }
    }

    public String getEmail() {
        return emailInput.getText();
    }

    public void setEmail(String email) {
        emailInput.clear();
        if (email != null) {
            emailInput.sendKeys(email);
        }
    }

    public String getFirstName() {
        return firstNameInput.getText();
    }

    public void setFirstName(String firstName) {
        firstNameInput.clear();
        if (firstName != null) {
            firstNameInput.sendKeys(firstName);
        }
    }

    public String getLastName() {
        return lastNameInput.getText();
    }

    public void setLastName(String lastname) {
        lastNameInput.clear();
        if (lastname != null) {
            lastNameInput.sendKeys(lastname);
        }
    }

    public boolean isEnabled() {
        return userEnabledSwitch.isOn();
    }

    public void setEnabled(boolean enabled) {
        userEnabledSwitch.setOn(enabled);
    }

    public boolean isEmailVerified() {
        return emailVerifiedSwitch.isOn();
    }

    public void setEmailVerified(boolean emailVerified) {
        emailVerifiedSwitch.setOn(emailVerified);
    }

    public void addRequiredAction(String requiredAction) {
        requiredUserActionsInput.sendKeys(requiredAction);
        requiredUserActionsConfirm.click();
    }

    public void setRequiredActions(List<String> requiredActions) {
        for (WebElement e : removeRequiredActionsList) {
            e.click();
        }
        if (requiredActions != null && !requiredActions.isEmpty()) {
            for (String action : requiredActions) {
                addRequiredAction(action);
            }
        }
    }

    public void setValues(UserRepresentation user) {
        waitAjaxForElement(usernameInput);
        setUsername(user.getUsername());
        setEmail(user.getEmail());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
        setEnabled(user.isEnabled());
        setEmailVerified(user.isEmailVerified());
        setRequiredActions(user.getRequiredActions());
    }

    // TODO Contact Information section
}
