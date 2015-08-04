package org.keycloak.testsuite.page.auth;

import javax.ws.rs.core.UriBuilder;
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

    @FindBy(css = "div[id='kc-form-options'] span a")
    private WebElement backToLoginForm;

    public void backToLoginPage() {
        backToLoginForm.click();
    }

}
