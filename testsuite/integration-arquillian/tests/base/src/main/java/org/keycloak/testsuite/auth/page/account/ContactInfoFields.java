package org.keycloak.testsuite.auth.page.account;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class ContactInfoFields extends Form {

    @FindBy(id = "user.attributes.street")
    private WebElement streetInput;
    @FindBy(id = "user.attributes.locality")
    private WebElement localityInput;
    @FindBy(id = "user.attributes.region")
    private WebElement regionInput;
    @FindBy(id = "user.attributes.postal_code")
    private WebElement postalCodeInput;
    @FindBy(id = "user.attributes.country")
    private WebElement counryInput;

}
