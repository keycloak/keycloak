package org.keycloak.testsuite.console.page.users;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.page.fragment.MultipleStringSelect2;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
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

    @FindBy(id = "s2id_groups")
    private GroupSelect groupsInput;

    @FindBy(id = "s2id_reqActions")
    private MultipleStringSelect2 requiredUserActionsSelect;

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

    public void setGroups(Set<String> groups) { groupsInput.update(groups); }

    public void addRequiredAction(String requiredAction) {
        requiredUserActionsSelect.select(requiredAction);
    }

    public void setRequiredActions(Set<String> requiredActions) {
        requiredUserActionsSelect.update(requiredActions);
    }

    public void setValues(UserRepresentation user) {
        waitUntilElement(usernameInput).is().present();
        if (user.getUsername() != null) {
            setUsername(user.getUsername());
        }
        setEmail(user.getEmail());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
        if (user.isEnabled() != null) setEnabled(user.isEnabled());
        if (user.isEmailVerified() != null) setEmailVerified(user.isEmailVerified());
        if (user.getGroups() != null && user.getGroups().size() > 0) setGroups(new HashSet<String>(user.getGroups()));
        if (user.getRequiredActions() != null) setRequiredActions(new HashSet<>(user.getRequiredActions()));
    }

    // TODO Contact Information section

    public class GroupSelect extends MultipleStringSelect2 {

        @Override
        protected List<WebElement> getSelectedElements() {
            return getRoot().findElements(By.xpath("(//table[@id='selected-groups'])/tbody/tr")).stream()
                    .filter(webElement -> webElement.findElements(By.tagName("td")).size() > 1)
                    .collect(Collectors.toList());
        }

        @Override
        protected BiFunction<WebElement, String, Boolean> deselect() {
            return (webElement, name) -> {
                List<WebElement> tds = webElement.findElements(By.tagName("td"));

                if (!getTextFromElement(tds.get(0)).isEmpty()) {
                    if (getTextFromElement(tds.get(0)).equals(name)) {
                        tds.get(1).findElement(By.tagName("button")).click();
                        return true;
                    }
                }

                return false;
            };
        }

        @Override
        protected Function<WebElement, String> representation() {
            return webElement -> getTextFromElement(webElement.findElements(By.tagName("td")).get(0));
        }

        @Override
        protected boolean match(String result, String search) {
            return result != null && result.equalsIgnoreCase("/" + search);
        }
    }
}
