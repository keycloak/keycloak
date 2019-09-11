package org.keycloak.testsuite.springboot;

import org.apache.commons.lang3.math.NumberUtils;
import org.keycloak.testsuite.pages.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SessionPage extends AbstractSpringbootPage {

    @FindBy(id = "counter")
    private WebElement counterElement;

    public static final String PAGE_TITLE = "session counter page";

    public SessionPage() {
        super(PAGE_TITLE);
    }

    public int getCounter() {
        String counterString = counterElement.getText();

        return NumberUtils.toInt(counterString, 0);
    }
}
