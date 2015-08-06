package org.keycloak.testsuite.console.page.roles;

import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class RoleForm extends Form {

    @FindBy(id = "name")
    private WebElement nameInput;

    @FindBy(id = "description")
    private WebElement descriptionInput;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='compositeSwitch']]")
    private OnOffSwitch compositeSwitch;

    @FindBy(xpath = ".//fieldset[./legend[contains(text(),'Composite Roles')]]")
    private RoleCompositeRoles compositeRoles;

    @FindBy(id = "removeRole")
    private WebElement removeIcon;

    @FindBy(css = ".modal-dialog")
    private ModalDialog modalDialog;

    public ModalDialog deleteDialog() {
        return modalDialog;
    }

    public RoleRepresentation getRole() {
        RoleRepresentation role = new RoleRepresentation(getName(), getDescription());
        role.setComposite(isComposite());
        if (role.isComposite()) {
            role.setComposites(compositeRoles.getComposites());
        }
        return role;
    }
    
    public void setRole(RoleRepresentation role) {
        setBasicAttributes(role);
        if (role.isComposite()) {
            setCompositeRoles(role);
        }
    }

    public RoleRepresentation getBasicAttributes() {
        RoleRepresentation role = new RoleRepresentation(getName(), getDescription());
        role.setComposite(isComposite());
        return role;
    }

    public void setBasicAttributes(RoleRepresentation role) {
        setName(role.getName());
        setDescription(role.getDescription());
    }

    // TODO KEYCLOAK-1364 enabling/disabling composite role seems unintuitive
    // it should be possible to remove all composite roles by switching to OFF
    public void setCompositeRoles(RoleRepresentation role) {
        if (role.isComposite() && role.getComposites() != null) {
            setComposite(true);
        }
        compositeRoles.setComposites(role.getComposites());
    }

    public void setName(String name) {
        setInputText(nameInput, name);
    }

    public String getName() {
        return nameInput.getText();
    }

    public void setDescription(String description) {
        setInputText(descriptionInput, description);
    }

    public String getDescription() {
        return descriptionInput.getText();
    }

    public void setComposite(boolean composite) {
        compositeSwitch.setOn(composite);
    }

    public boolean isComposite() {
        return compositeSwitch.isOn();
    }

    public RoleCompositeRoles compositeRoles() {
        return compositeRoles;
    }
    
    public void delete() {
        delete(true);
    }

    public void delete(boolean confirm) {
        removeIcon.click();
        if (confirm) {
            modalDialog.confirmDeletion();
        } else {
            modalDialog.cancel();
        }
    }

}
