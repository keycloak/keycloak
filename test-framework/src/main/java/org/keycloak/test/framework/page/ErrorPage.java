package org.keycloak.test.framework.page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ErrorPage extends AbstractPage {

    @FindBy(className = "instruction")
    private WebElement errorMessage;

    @FindBy(id = "backToApplication")
    private WebElement backToApplicationLink;
    public ErrorPage(WebDriver driver) {
        super(driver);
    }

    public String getError() {
        return errorMessage.getText();
    }

    @Override
    public void open() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCurrent() {
        return "We are sorry...".equals(driver.getTitle());
    }
}
