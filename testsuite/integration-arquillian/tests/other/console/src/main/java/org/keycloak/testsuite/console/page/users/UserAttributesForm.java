package org.keycloak.testsuite.console.page.users;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

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

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='userEnabled']]")
    private OnOffSwitch userEnabledSwitch;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='emailVerified']]")
    private OnOffSwitch emailVerifiedSwitch;

    @FindBy(xpath = ".//div[./label[contains(text(), 'Required User Actions')]]//input")
    private WebElement requiredUserActionsInput;

    @FindBy(id = "reqActions")
    private Select requiredUserActionsSelect;

    @FindBy(className = "select2-result-label")
    private WebElement requiredUserActionsConfirm;

    @FindBy(className = "select2-search-choice-close")
    private List<WebElement> removeRequiredActionsList;

    @FindBy(xpath = "//button[@data-ng-click='unlockUser()']")
    private WebElement unlockUserButton;

    public String getId() {
        return UIUtils.getTextInputValue(idInput);
    }

    public String getUsername() {
        return UIUtils.getTextInputValue(usernameInput);
    }

    public void setUsername(String username) {
        UIUtils.setTextInputValue(usernameInput, username);
    }

    public String getEmail() {
        return UIUtils.getTextInputValue(emailInput);
    }

    public void setEmail(String email) {
        UIUtils.setTextInputValue(emailInput, email);
    }

    public String getFirstName() {
        return UIUtils.getTextInputValue(firstNameInput);
    }

    public void setFirstName(String firstName) {
        UIUtils.setTextInputValue(firstNameInput, firstName);
    }

    public String getLastName() {
        return UIUtils.getTextInputValue(lastNameInput);
    }

    public void setLastName(String lastname) {
        UIUtils.setTextInputValue(lastNameInput, lastname);
    }

    public boolean isEnabled() {
        return userEnabledSwitch.isOn();
    }

    public void setEnabled(boolean enabled) {
        userEnabledSwitch.setOn(enabled);
    }

    public void unlockUser() {
        unlockUserButton.click();
    }

    public boolean isEmailVerified() {
        return emailVerifiedSwitch.isOn();
    }

    public void setEmailVerified(boolean emailVerified) {
        emailVerifiedSwitch.setOn(emailVerified);
    }

    public void addRequiredAction(String requiredAction) {
        requiredUserActionsInput.click();
        requiredUserActionsSelect.selectByVisibleText(requiredAction);
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
        waitUntilElement(usernameInput).is().present();
        setUsername(user.getUsername());
        setEmail(user.getEmail());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
        if (user.isEnabled() != null) setEnabled(user.isEnabled());
        if (user.isEmailVerified() != null) setEmailVerified(user.isEmailVerified());
        setRequiredActions(user.getRequiredActions());
    }

    // TODO Contact Information section
}
