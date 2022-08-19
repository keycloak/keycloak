package org.keycloak.testsuite.console.page.clients.roles;

import org.keycloak.testsuite.console.page.roles.*;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class ClientRole extends ClientRoles {

    public static final String ROLE_ID = "roleId";
    
    @FindBy(xpath = "//i[contains(@class, 'delete')]")
    private WebElement deleteIcon;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + ROLE_ID + "}";
    }

    public void setRoleId(String id) {
        setUriParameter(ROLE_ID, id);
    }

    public String getRoleId() {
        return getUriParameter(ROLE_ID).toString();
    }

    private RoleDetailsForm form;

    public RoleDetailsForm form() {
        return form;
    }

    public void backToClientRolesViaBreadcrumb() {
        breadcrumb().clickItemOneLevelUp();
    }
    
    @Override
    public void delete() {
        deleteIcon.click();
        modalDialog.confirmDeletion();
    }

}
