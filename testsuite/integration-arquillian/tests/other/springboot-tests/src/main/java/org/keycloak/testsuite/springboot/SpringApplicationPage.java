package org.keycloak.testsuite.springboot;

import org.keycloak.testsuite.pages.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SpringApplicationPage extends AbstractPage {

    @FindBy(className = "test")
    private WebElement testDiv;

    @FindBy(className = "adminlink")
    private WebElement adminLink;

    private String title;

    public SpringApplicationPage() {
        super();

        title = "springboot test page";
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean isCurrent() {
        return driver.getTitle().equalsIgnoreCase(title);
    }

    @Override
    public void open() throws Exception {

    }

    public void goAdmin() {
        adminLink.click();
    }
}
