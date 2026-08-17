package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

public class LoginUsernameOnlyPage extends LoginUsernamePage {

    public LoginUsernameOnlyPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void login(String username) {
        fillLoginWithUsernameOnly(username);
        submit();
    }

    public String getUsernameError() {
        return getUsernameInputError();
    }

    public void clickSubmitButton() {
        submit();
    }
}
