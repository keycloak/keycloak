package org.keycloak.testsuite.auth.page.login;

import javax.ws.rs.core.UriBuilder;
import org.keycloak.testsuite.auth.page.AuthRealm;
import static org.keycloak.testsuite.util.WaitUtils.waitGuiForElementPresent;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class LoginActions extends AuthRealm {

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("login-actions");
    }

    @FindBy(css = "input[type='submit']")
    private WebElement submitButton;

    @FindBy(css = "div[id='kc-form-options'] span a")
    private WebElement backToLoginForm;

    @FindBy(css = "span.kc-feedback-text")
    private WebElement feedbackText;
    
    public String getFeedbackText() {
        waitGuiForElementPresent(feedbackText, "Feedback message should be visible");
        return feedbackText.getText();
    }
    
    public void backToLoginPage() {
        backToLoginForm.click();
    }

    public void submit() {
        submitButton.click();
    }

}
