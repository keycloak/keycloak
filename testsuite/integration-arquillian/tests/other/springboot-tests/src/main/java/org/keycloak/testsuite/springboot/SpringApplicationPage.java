package org.keycloak.testsuite.springboot;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

public class SpringApplicationPage extends AbstractSpringbootPage {

    @FindBy(className = "test")
    private WebElement testDiv;

    @FindBy(className = "adminlink")
    private WebElement adminLink;

    public static final String PAGE_TITLE = "springboot test page";

    public SpringApplicationPage() {
        super(PAGE_TITLE);
    }

    public void goAdmin() {
        clickLink(adminLink);
    }
}
