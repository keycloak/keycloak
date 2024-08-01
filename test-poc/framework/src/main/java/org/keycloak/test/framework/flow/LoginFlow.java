package org.keycloak.test.framework.flow;

import org.openqa.selenium.WebDriver;

public class LoginFlow implements ManagedFlow{

    private LoginFlowLibrary library;

    public LoginFlow(WebDriver driver) {
        this.library = new LoginFlowLibrary(driver);
    }

    @Override
    public void execute() {
        this.library.execute(LoginFlow.class.getSimpleName())
                .navigateToWelcomeScreen()
                .loginAsAdmin()
                .complete(LoginFlow.class.getSimpleName());
    }

    @Override
    public void rollback() {
        this.library.execute(LoginFlow.class.getSimpleName())
                .logout()
                .navigateToWelcomeScreen()
                .complete(LoginFlow.class.getSimpleName());
    }
}
