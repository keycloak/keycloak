package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

public class UpdateAccountInformationPage extends LoginUpdateProfilePage {

    public UpdateAccountInformationPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void updateAccountInformation(String userName, String email, String firstName, String lastName) {
        prepareUpdate().username(userName).email(email).firstName(firstName).lastName(lastName).submit();
    }

    public void updateAccountInformation(String userName, String email, String firstName, String lastName, String department) {
        prepareUpdate().username(userName).email(email).firstName(firstName).lastName(lastName).department(department).submit();
    }

    public void updateAccountInformation(String email, String firstName, String lastName) {
        prepareUpdate().email(email).firstName(firstName).lastName(lastName).submit();
    }

    public void updateAccountInformation(String firstName, String lastName) {
        prepareUpdate().firstName(firstName).lastName(lastName).submit();
    }

    @Override
    public String getExpectedPageId() {
        return "login-idp-review-user-profile";
    }
}
