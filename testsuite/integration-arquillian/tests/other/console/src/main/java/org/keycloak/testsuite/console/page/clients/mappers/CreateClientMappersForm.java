package org.keycloak.testsuite.console.page.clients.mappers;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.testsuite.console.page.clients.authorization.policy.ClientSelectModal;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;


/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class CreateClientMappersForm extends Form {

    // Mappers types
    public static final String HARDCODED_ROLE = "Hardcoded Role";
    public static final String HARDCODED_CLAIM = "Hardcoded claim";
    public static final String USER_SESSION_NOTE = "User Session Note";
    public static final String ROLE_NAME_MAPPER = "Role Name Mapper";
    public static final String USER_ADDRESS = "User Address";
    public static final String USERS_FULL_NAME = "User's full name";
    public static final String USER_ATTRIBUTE = "User Attribute";
    public static final String USER_PROPERTY = "User Property";
    public static final String GROUP_MEMBERSHIP = "Group Membership";
    public static final String ROLE_LIST = "Role list";
    public static final String HARDCODED_ATTRIBUTE = "Hardcoded attribute";
    public static final String GROUP_LIST = "Group list";
    public static final String HARDCODED_ROLE_SAML = "Hardcoded role";

    // Role types
    public static final String REALM_ROLE = "realm";
    public static final String CLIENT_ROLE = "client";
    
    @FindBy(id = "name")
    private WebElement nameElement;

    @FindBy(id = "mapperTypeCreate")
    private Select mapperTypeSelect;

    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Property')]//following-sibling::node()//input[@type='text']")
    private WebElement propertyInput;

    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'User Attribute')]//following-sibling::node()//input[@type='text']")
    private WebElement userAttributeInput;

    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'User Session Note')]//following-sibling::node()//input[@type='text']")
    private WebElement userSessionNoteInput;

    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Multivalued')]//following-sibling::node()//div[@class='onoffswitch']")
    private OnOffSwitch multivaluedInput;

    @FindBy(xpath = ".//button[text() = 'Select Role']/../..//input")
    private WebElement roleInput;

    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'New Role Name')]//following-sibling::node()//input[@type='text']")
    private WebElement newRoleInput;

    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Token Claim Name')]//following-sibling::node()//input[@type='text']")
    private WebElement tokenClaimNameInput;

    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Claim value')]//following-sibling::node()//input[@type='text']")
    private WebElement tokenClaimValueInput;

    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Claim JSON Type')]//following-sibling::node()//select")
    private Select claimJSONTypeInput;

    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Add to ID token')]//following-sibling::node()//div[@class='onoffswitch']")
    private OnOffSwitch addToIDTokenInput;

    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Add to access token')]//following-sibling::node()//div[@class='onoffswitch']")
    private OnOffSwitch addToAccessTokenInput;
    
    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Full group path')]//following-sibling::node()//div[@class='onoffswitch']")
    private OnOffSwitch fullGroupPath;

    @FindBy(xpath = ".//button[text() = 'Select Role']")
    private WebElement selectRoleButton;
            
    @FindBy(xpath = "//div[@class='modal-dialog']")
    private RoleSelectorModalDialog roleSelectorModalDialog;

    public class RoleSelectorModalDialog {
        @Drone
        private WebDriver driver;

        @FindBy(id = "available")
        private Select realmAvailable;
        @FindBy(xpath = ".//button[@tooltip='Select realm role' and not(@disabled)]")
        private WebElement selectRealmRoleButton;
        
        @FindBy(id = "available-client")
        private Select clientAvailable;
        @FindBy(id = "clients")
        private Select clientSelect;
        @FindBy(xpath = ".//button[@tooltip='Select client role' and not(@disabled)]")
        private WebElement selectClientRoleButton;
        @FindBy(xpath = ".//button[@class='close']")
        private WebElement closeButton;

        @FindBy(id = "s2id_clients")
        private ClientSelectModal clientsInput;

        public void closeRoleSelectorModalDialog() {
            closeButton.click();
        }
        
        public void selectRealmRole(String roleName) {
            if (roleName != null) {
                realmAvailable.selectByVisibleText(roleName);
            }
            WaitUtils.pause(1000);
            selectRealmRoleButton.click();
            WaitUtils.waitForModalFadeOut();
        }
        
        public void selectClientRole(String clientName, String roleName) {
            WaitUtils.pause(100);
            if (roleName != null || clientName != null) {
                clientAvailable.selectByVisibleText(roleName);
            }
            WaitUtils.pause(1000);
            selectClientRoleButton.click();
            WaitUtils.waitForModalFadeOut();
        }

        public void selectClient(String clientName) {
            clientsInput.select(clientName);
        }
    }
    
    public void selectRole(String roleType, String roleName, String clientName) {
        selectRoleButton.click();
        switch (roleType) {
            case REALM_ROLE:
                roleSelectorModalDialog.selectRealmRole(roleName);
                break;
            case CLIENT_ROLE:
                roleSelectorModalDialog.selectClient(clientName);
                roleSelectorModalDialog.selectClientRole(clientName, roleName);
                break;
            default:
                throw new IllegalArgumentException("No such role type, use \"" + 
                        REALM_ROLE + "\" or \"" + CLIENT_ROLE + "\"");
        }
    }
    
    public void closeRoleSelectorModalDialog() {
        roleSelectorModalDialog.closeRoleSelectorModalDialog();
    }
    
    public void setName(String value) {
        UIUtils.setTextInputValue(nameElement, value);
    }

    public void setMapperType(String type) {
        mapperTypeSelect.selectByVisibleText(type);
    }
    
    public String getProperty() {
        return UIUtils.getTextInputValue(propertyInput);
    }
    
    public void setProperty(String value) {
        UIUtils.setTextInputValue(propertyInput, value);
    }

    public String getUserAttribute() {
        return UIUtils.getTextInputValue(userAttributeInput);
    }

    public void setUserAttribute(String value) {
        UIUtils.setTextInputValue(userAttributeInput, value);
    }

    public String getUserSessionNote() {
        return UIUtils.getTextInputValue(userSessionNoteInput);
    }

    public void setUserSessionNote(String value) {
        UIUtils.setTextInputValue(userSessionNoteInput, value);
    }

    public boolean isMultivalued() {
        return multivaluedInput.isOn();
    }

    public void setMultivalued(boolean value) {
        multivaluedInput.setOn(value);
    }

    public String getRole() {
        return UIUtils.getTextInputValue(roleInput);
    }

    public void setRole(String value) {
        UIUtils.setTextInputValue(roleInput, value);
    }

    public String getNewRole() {
        return UIUtils.getTextInputValue(newRoleInput);
    }

    public void setNewRole(String value) {
        UIUtils.setTextInputValue(newRoleInput, value);
    }

    public String getTokenClaimName() {
        return UIUtils.getTextInputValue(tokenClaimNameInput);
    }

    public void setTokenClaimName(String value) {
        UIUtils.setTextInputValue(tokenClaimNameInput, value);
    }

    public String getTokenClaimValue() {
        return UIUtils.getTextInputValue(tokenClaimValueInput);
    }

    public void setTokenClaimValue(String value) {
        UIUtils.setTextInputValue(tokenClaimValueInput, value);
    }

    public String getClaimJSONType() {
        return getTextFromElement(claimJSONTypeInput.getFirstSelectedOption());
    }

    public void setClaimJSONType(String value) {
        claimJSONTypeInput.selectByVisibleText(value);
    }

    public boolean isAddToIDToken() {
        return addToIDTokenInput.isOn();
    }

    public void setAddToIDToken(boolean value) {
        addToIDTokenInput.setOn(value);
    }

    public boolean isAddToAccessToken() {
        return addToAccessTokenInput.isOn();
    }

    public void setAddToAccessToken(boolean value) {
        addToAccessTokenInput.setOn(value);
    }
    
    public boolean isFullGroupPath() {
        return fullGroupPath.isOn();
    }
    
    public void setFullGroupPath(boolean value) {
        fullGroupPath.setOn(value);
    }
    
    //SAML
    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Role attribute name')]//following-sibling::node()//input[@type='text']")
    private WebElement roleAttributeNameInput;
    
    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Friendly Name')]//following-sibling::node()//input[@type='text']")
    private WebElement friendlyNameInput;
    
    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'SAML Attribute NameFormat')]//following-sibling::node()//select")
    private Select samlAttributeNameFormatSelect;
    
    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Single Role Attribute')]//following-sibling::node()//div[@class='onoffswitch']")
    private OnOffSwitch singleRoleAttributeSwitch;
    
    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Attribute value')]//following-sibling::node()//input[@type='text']")
    private WebElement attributeValueInput;
    
    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Group attribute name')]//following-sibling::node()//input[@type='text']")
    private WebElement groupAttributeNameInput;
    
    @FindBy(xpath = ".//div[@properties='model.mapperType.properties']//label[contains(text(),'Single Group Attribute')]//following-sibling::node()//div[@class='onoffswitch']")
    private OnOffSwitch singleGroupAttributeSwitch;
    
    public void setRoleAttributeName(String value) {
        UIUtils.setTextInputValue(roleAttributeNameInput, value);
    }
    
    public void setFriendlyName(String value) {
        UIUtils.setTextInputValue(friendlyNameInput, value);
    }

    public void setSamlAttributeNameFormat(String value) {
        samlAttributeNameFormatSelect.selectByVisibleText(value);
    }
    
    public void setSingleRoleAttribute(boolean value) {
        singleRoleAttributeSwitch.setOn(value);
    }
    
    public void setAttributeValue(String value) {
        UIUtils.setTextInputValue(attributeValueInput, value);
    }
    
    public void setGroupAttributeName(String value) {
        UIUtils.setTextInputValue(groupAttributeNameInput, value);
    }
    
    public void setSingleGroupAttribute(boolean value) {
        singleGroupAttributeSwitch.setOn(value);
    }
}
