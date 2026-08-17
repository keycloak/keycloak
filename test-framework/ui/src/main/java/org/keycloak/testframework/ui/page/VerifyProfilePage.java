package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

public class VerifyProfilePage extends LoginUpdateProfilePage {

    public VerifyProfilePage(ManagedWebDriver driver) {
        super(driver);
    }

    public void updateEmail(String email, String firstName, String lastName) {
        prepareUpdate().email(email).firstName(firstName).lastName(lastName).submit();
    }
}
