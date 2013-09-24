package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.rule.Driver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class AppPage {

    private static String PATH = Constants.APP_ROOT + "/user.jsp";

    @Driver
    private WebDriver browser;

    @FindBy(id = "logout")
    private WebElement logoutLink;

    @FindBy(id = "user")
    private WebElement user;
    
    public void open() {
        browser.navigate().to(PATH);
    }

    public String getUser() {
        return user.getText();
    }

    public boolean isCurrent() {
        return browser.getCurrentUrl().equals(PATH);
    }

    public void logout() {
        logoutLink.click();
    }

}
