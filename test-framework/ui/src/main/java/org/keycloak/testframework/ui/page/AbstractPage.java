package org.keycloak.testframework.ui.page;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

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

    private boolean isActivePage() {
        return getExpectedPageId().equals(getCurrentPageId());
    }

    public void assertCurrent() {
        Assertions.assertEquals(getExpectedPageId(), getCurrentPageId(), "Not on the expected page");
    }
}
