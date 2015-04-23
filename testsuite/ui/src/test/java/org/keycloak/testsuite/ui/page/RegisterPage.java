package org.keycloak.testsuite.ui.page;

import org.keycloak.testsuite.ui.model.User;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.concurrent.TimeUnit;

import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;

/**
 * Created by fkiss.
 */

public class RegisterPage extends AbstractPage {

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "password-confirm")
    private WebElement passwordConfirmInput;

    @FindBy(css = "span.kc-feedback-text")
    private WebElement feedbackError;

	public void registerNewUser(User user) {
		registerNewUser(user, user.getPassword());
	}
	
    public void registerNewUser(User user, String confirmPassword) {
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        waitGuiForElement(passwordConfirmInput, "Register form should be visible");
        clearAndType(usernameInput, user.getUserName());
        clearAndType(firstNameInput, user.getFirstName());
        clearAndType(lastNameInput, user.getLastName());
        clearAndType(emailInput, user.getEmail());
        clearAndType(passwordInput, user.getPassword());
        clearAndType(passwordConfirmInput, confirmPassword);
        primaryButton.click();
    }

    public void clearAndType(WebElement webElement, String text) {
            webElement.clear();
            webElement.sendKeys(text);
    }

    public boolean isInvalidEmail() {
        waitGuiForElement(feedbackError, "Feedback message should be visible");
        return feedbackError.getText().equals("Invalid email address");
    }

    public boolean isAttributeSpecified(String attribute) {
        waitGuiForElement(feedbackError, "Feedback message should be visible");
        return !feedbackError.getText().equals("Please specify " + attribute);
    }

    public boolean isPasswordSame() {
        waitGuiForElement(feedbackError, "Feedback message should be visible");
        return !feedbackError.getText().equals("Password confirmation doesn't match");
    }

}
