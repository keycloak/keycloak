package org.keycloak.testsuite.console.page.clients;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 *
 * TODO: SAML
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

    @FindBy(id = "name")
    private WebElement nameElement;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='consentRequired']]")
    private OnOffSwitch consentRequiredSwitch;

    @FindBy(id = "consentText")
    private WebElement consentTextElement;

    @FindBy(id = "mapperTypeCreate")
    private Select mapperTypeSelect;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='Property']//following-sibling::node()//input[@type='text']")
    private WebElement propertyInput;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='User Attribute']//following-sibling::node()//input[@type='text']")
    private WebElement userAttributeInput;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='User Session Note']//following-sibling::node()//input[@type='text']")
    private WebElement userSessionNoteInput;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='Multivalued']//following-sibling::node()//div[@class='onoffswitch']")
    private OnOffSwitch multivaluedInput;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='Role']//following-sibling::node()//input[@type='text']")
    private WebElement roleInput;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='New Role Name']//following-sibling::node()//input[@type='text']")
    private WebElement newRoleInput;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='Token Claim Name']//following-sibling::node()//input[@type='text']")
    private WebElement tokenClaimNameInput;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='Claim value']//following-sibling::node()//input[@type='text']")
    private WebElement tokenClaimValueInput;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='Claim JSON Type']//following-sibling::node()//select")
    private Select claimJSONTypeInput;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='Add to ID token']//following-sibling::node()//div[@class='onoffswitch']")
    private OnOffSwitch addToIDTokenInput;

    @FindBy(xpath = ".//div[@properties='mapperType.properties']//label[text()='Add to access token']//following-sibling::node()//div[@class='onoffswitch']")
    private OnOffSwitch addToAccessTokenInput;

    public boolean isConsentRequired() {
        return consentRequiredSwitch.isOn();
    }

    public void setConsentRequired(boolean consentRequired) {
        consentRequiredSwitch.setOn(consentRequired);
    }

    public String getConsentText() {
        return getInputValue(consentTextElement);
    }

    public void setConsentText(String consentText) {
        setInputValue(consentTextElement, consentText);
    }

    public String getMapperType() {
        return mapperTypeSelect.getFirstSelectedOption().getText();
    }

    public void setMapperType(String type) {
        mapperTypeSelect.selectByVisibleText(type);
    }
    
    public String getProperty() {
        return getInputValue(propertyInput);
    }
    
    public void setProperty(String value) {
        setInputValue(propertyInput, value);
    }

    public String getUserAttribute() {
        return getInputValue(userAttributeInput);
    }

    public void setUserAttribute(String value) {
        setInputValue(userAttributeInput, value);
    }

    public String getUserSessionNote() {
        return getInputValue(userSessionNoteInput);
    }

    public void setUserSessionNote(String value) {
        setInputValue(userSessionNoteInput, value);
    }

    public boolean isMultivalued() {
        return multivaluedInput.isOn();
    }

    public void setMultivalued(boolean value) {
        multivaluedInput.setOn(value);
    }

    public String getRole() {
        return getInputValue(roleInput);
    }

    public void setRole(String value) {
        setInputValue(roleInput, value);
    }

    public String getNewRole() {
        return getInputValue(newRoleInput);
    }

    public void setNewRole(String value) {
        setInputValue(newRoleInput, value);
    }

    public String getTokenClaimName() {
        return getInputValue(tokenClaimNameInput);
    }

    public void setTokenClaimName(String value) {
        setInputValue(tokenClaimNameInput, value);
    }

    public String getTokenClaimValue() {
        return getInputValue(tokenClaimValueInput);
    }

    public void setTokenClaimValue(String value) {
        setInputValue(tokenClaimValueInput, value);
    }

    public String getClaimJSONType() {
        return claimJSONTypeInput.getFirstSelectedOption().getText();
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
}
