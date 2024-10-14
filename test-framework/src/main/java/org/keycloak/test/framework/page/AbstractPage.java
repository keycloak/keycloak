package org.keycloak.test.framework.page;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public abstract class AbstractPage {

    protected final WebDriver driver;

    public AbstractPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    abstract public void open() throws Exception;

    abstract public boolean isCurrent();

    public void assertCurrent() {
        String name = getClass().getSimpleName();
        Assertions.assertTrue(isCurrent(), "Expected " + name + " but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")");
    }

}
