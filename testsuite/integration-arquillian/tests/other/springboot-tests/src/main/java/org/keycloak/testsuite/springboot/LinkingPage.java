package org.keycloak.testsuite.springboot;

import org.keycloak.testsuite.pages.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LinkingPage extends AbstractPage {

    @FindBy(id = "error")
    private WebElement errorMessage;

    @Override
    public boolean isCurrent() {
        return driver.getTitle().equalsIgnoreCase("linking page result");
    }

    @Override
    public void open() throws Exception {
    }

    public String getErrorMessage() {
        return errorMessage.getText();
    }
}
