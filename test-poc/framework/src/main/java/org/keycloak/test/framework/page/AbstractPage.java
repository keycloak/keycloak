package org.keycloak.test.framework.page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class AbstractPage {

    protected final WebDriver driver;

    public AbstractPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

}
