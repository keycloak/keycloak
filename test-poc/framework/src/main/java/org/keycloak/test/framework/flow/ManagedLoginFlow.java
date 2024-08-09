package org.keycloak.test.framework.flow;

import org.keycloak.test.framework.page.LoginPage;
import org.openqa.selenium.WebDriver;

public class ManagedLoginFlow implements ManagedFlow{

    private ManagedFlowLibrary library;
    private WebDriver driver;
    private LoginPage page;

    ManagedLoginFlow(WebDriver driver, LoginPage page) {
        this.driver = driver;
        this.page = page;
        build();
    }

    public void build() {
        this.library = new ManagedFlowLibrary();
    }

    @Override
    public void execute() {
        library.navigateToAdmin(driver)
                .loginToAdmin((LoginPage) page)
                .complete(ManagedLoginFlow.class.getSimpleName());
    }

    @Override
    public void rollback() {
        library.logout()
                .navigateToAdmin(driver)
                .complete(ManagedLoginFlow.class.getSimpleName());
    }

    @Override
    public void close() {
        // remove unused object
    }
}
