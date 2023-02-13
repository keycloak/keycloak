package org.keycloak.testsuite.springboot;

import org.keycloak.testsuite.pages.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LinkingPage extends AbstractSpringbootPage {

    public static final String PAGE_TITLE = "linking page result";

    public LinkingPage() {
        super(PAGE_TITLE);
    }

    @FindBy(id = "error")
    private WebElement errorMessage;


    public String getErrorMessage() {
        return errorMessage.getText();
    }
}
