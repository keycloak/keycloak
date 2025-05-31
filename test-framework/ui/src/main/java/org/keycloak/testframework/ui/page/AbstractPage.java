package org.keycloak.testframework.ui.page;

import org.junit.jupiter.api.Assertions;
import org.keycloak.testframework.ui.util.PageUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class AbstractPage {

    @FindBy(xpath = "//body")
    private WebElement body;

    protected final WebDriver driver;

    public AbstractPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public abstract String getExpectedPageId();

    public String getCurrentPageId() {
        return body.getAttribute("data-page-id");
    }

    public void waitForPage() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> isActivePage());
        } catch (RuntimeException e) {
            throw new RuntimeException("Waiting for '" + getExpectedPageId() + "', but was '" + getCurrentPageId() + "'");
        }
    }

    public boolean isActivePage() {
        return getExpectedPageId().equals(getCurrentPageId());
    }

    public void assertCurrent() {
        String name = getClass().getSimpleName();
        Assertions.assertTrue(isCurrent(), "Expected " + name + " but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")");
    }

    abstract public boolean isCurrent();

    public boolean isCurrent(String expectedTitle) {
        return PageUtils.getPageTitle(driver).equals(expectedTitle);
    }
}
