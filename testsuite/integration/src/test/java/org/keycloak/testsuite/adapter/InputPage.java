package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.pages.AbstractPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InputPage extends AbstractPage {
    @FindBy(id = "parameter")
    private WebElement parameter;

    @FindBy(name = "submit")
    private WebElement submit;

    public void execute(String param) {
        parameter.clear();
        parameter.sendKeys(param);

        submit.click();
    }


    public boolean isCurrent() {
        return driver.getTitle().equals("Input Page");
    }

    @Override
    public void open() {
    }


}
