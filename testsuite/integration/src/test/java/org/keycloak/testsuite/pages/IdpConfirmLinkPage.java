package org.keycloak.testsuite.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpConfirmLinkPage extends AbstractPage {

    @FindBy(id = "updateProfile")
    private WebElement updateProfileButton;

    @FindBy(id = "linkAccount")
    private WebElement linkAccountButton;

    @FindBy(className = "instruction")
    private WebElement message;

    @Override
    public boolean isCurrent() {
        return driver.getTitle().equals("Account already exists");
    }

    public String getMessage() {
        return message.getText();
    }

    public void clickReviewProfile() {
        updateProfileButton.click();
    }

    public void clickLinkAccount() {
        linkAccountButton.click();
    }

    @Override
    public void open() throws Exception {
        throw new UnsupportedOperationException();
    }
}
