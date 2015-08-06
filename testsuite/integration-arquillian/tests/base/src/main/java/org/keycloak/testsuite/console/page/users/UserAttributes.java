package org.keycloak.testsuite.console.page.users;

import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class UserAttributes extends User {

    @FindBy(name = "userForm")
    private UserAttributesForm form;

    public UserAttributesForm form() {
        return form;
    }

    public void backToUsersViaBreadcrumb() {
        breadcrumb().clickItemOneLevelUp();
    }

}
