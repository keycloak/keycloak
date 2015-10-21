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

    // Attributes labels
    public final static String PROPERTY_LABEL = "Property";
    public final static String USER_ATTRIBUTE_LABEL = "User Attribute";
    public final static String USER_SESSION_NOTE_LABEL = "User Session Note";
    public final static String MULTIVALUED_LABEL = "Multivalued";
    public final static String SELECT_ROLE_LABEL = "Select Role";
    public final static String TOKEN_CLAIM_NAME_LABEL = "Token Claim Name";
    public final static String CLAIM_JSON_TYPE_LABEL = "Claim JSON Type";
    public final static String ADD_TO_ID_TOKEN_LABEL = "Add to ID token";
    public final static String ADD_TO_ACCESS_TOKEN_LABEL = "Add to access token";

    @FindBy(id = "name")
    private WebElement nameElement;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='consentRequired']]")
    private OnOffSwitch consentRequiredSwitch;

    @FindBy(id = "consentText")
    private WebElement consentTextElement;

    @FindBy(id = "mapperTypeCreate")
    private Select mapperTypeSelect;

    @FindBy
    private WebElement roleElement;

    @ArquillianResource
    private Actions actions;

    public void getName() {
        getInputValue(nameElement);
    }

    public void setName(String name) {
        setInputValue(nameElement, name);
    }

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

    protected String getConfigBaseXPath(String label) {
        return ".//div[@properties='mapperType.properties']//label[text()='" + label + "']//following-sibling::node()";
    }

    protected WebElement getTextInputElement(String label) {
        return driver.findElement(By.xpath(getConfigBaseXPath(label) + "//input[@type='text']"));
    }

    protected Select getSelectElement(String label) {
        return new Select(driver.findElement(By.xpath(getConfigBaseXPath(label) + "//select")));
    }

    public String getTextInput(String label) {
        return getInputValue(getTextInputElement(label));
    }

    public void setTextInput(String label, String value) {
        setInputValue(getTextInputElement(label), value);
    }

    public String getSelect(String label) {
        return getSelectElement(label).getFirstSelectedOption().getText();
    }

    public void setSelect(String label, String value) {
        getSelectElement(label).selectByVisibleText(value);
    }

    public OnOffSwitch onOffSwitch(String label) {
        WebElement root = driver.findElement(By.xpath(getConfigBaseXPath(label) + "//div[@class='onoffswitch']"));
        return new OnOffSwitch(root, actions);
    }
}
