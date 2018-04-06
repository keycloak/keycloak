package org.keycloak.testsuite.springboot;

import org.apache.commons.lang3.math.NumberUtils;
import org.keycloak.testsuite.pages.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SessionPage extends AbstractPage {

    public static final String PAGE_TITLE = "session counter page";

    @FindBy(id = "counter")
    private WebElement counterElement;

    @Override
    public boolean isCurrent() {
        return driver.getTitle().equalsIgnoreCase(PAGE_TITLE);
    }

    @Override
    public void open() throws Exception {
    }

    public int getCounter() {
        String counterString = counterElement.getText();

        return NumberUtils.toInt(counterString, 0);
    }
}
