package org.keycloak.testsuite.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpLinkEmailPage extends AbstractPage {

    @FindBy(id = "instruction1")
    private WebElement message;

    @Override
    public boolean isCurrent() {
        return driver.getTitle().startsWith("Link ");
    }

    @Override
    public void open() throws Exception {
        throw new UnsupportedOperationException();
    }

    public String getMessage() {
        return message.getText();
    }
}
