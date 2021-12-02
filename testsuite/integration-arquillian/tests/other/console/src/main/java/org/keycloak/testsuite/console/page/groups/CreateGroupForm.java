package org.keycloak.testsuite.console.page.groups;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.getTextInputValue;
import static org.keycloak.testsuite.util.WaitUtils.*;

/**
 *
 * @author clementcur
 */
public class CreateGroupForm extends Form {

    @FindBy(id = "name")
    private WebElement groupNameInput;
    
    public void setValues(GroupRepresentation group) {
        waitUntilElement(groupNameInput).is().present();

        setGroupName(group.getName());
    }

    public String getGroupName() {
        return getTextInputValue(groupNameInput);
    }

    public void setGroupName(String groupName) {
        UIUtils.setTextInputValue(groupNameInput, groupName);
    }
}