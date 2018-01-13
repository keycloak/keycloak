package org.keycloak.testsuite.springboot;

import org.keycloak.testsuite.pages.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SpringAdminPage extends AbstractPage {

    @FindBy(className = "test")
    private WebElement testDiv;


    @Override
    public boolean isCurrent() {
        return driver.getTitle().equalsIgnoreCase("springboot admin page");
    }

    @Override
    public void open() throws Exception {

    }

    public String getTestDivString() {
        return testDiv.getText();
    }
}
