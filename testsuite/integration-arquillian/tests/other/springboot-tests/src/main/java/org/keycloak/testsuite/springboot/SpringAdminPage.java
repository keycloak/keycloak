package org.keycloak.testsuite.springboot;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SpringAdminPage extends AbstractSpringbootPage {

    @FindBy(className = "test")
    private WebElement testDiv;

    public static final String PAGE_TITLE = "springboot admin page";

    public SpringAdminPage() {
        super(PAGE_TITLE);
    }

    public String getTestDivString() {
        return testDiv.getText();
    }
}
