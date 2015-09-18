package org.keycloak.testsuite.console.page.clients;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.representations.idm.ClientRepresentation;
import static org.keycloak.testsuite.auth.page.login.OIDCLogin.OIDC;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.page.Form.getInputValue;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitAjaxForElement;
import org.keycloak.testsuite.util.Timer;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author tkyjovsk
 */
public class CreateClientForm extends Form {

    @FindBy(id = "clientId")
    private WebElement clientIdInput;

    @FindBy(id = "name")
    private WebElement nameInput;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='enabled']]")
    private OnOffSwitch enabledSwitch;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='consentRequired']]")
    private OnOffSwitch consentRequiredSwitch;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='directGrantsOnly']]")
    private OnOffSwitch directGrantsOnlySwitch;

    @FindBy(id = "protocol")
    private Select protocolSelect;
    @FindBy(id = "protocol")
    private WebElement protocolSelectElement;

    @FindBy
    private SAMLClientSettingsForm samlForm;

    public SAMLClientSettingsForm samlForm() {
        return samlForm;
    }

    @FindBy(id = "accessType")
    private Select accessTypeSelect;
    @FindBy(id = "accessType")
    private WebElement accessTypeSelectElement;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='serviceAccountsEnabled']]")
    private OnOffSwitch serviceAccountsEnabledSwitch;

    @FindBy(id = "newRedirectUri")
    private WebElement newRedirectUriInput;
    @FindBy(xpath = ".//input[@ng-model='client.redirectUris[i]']")
    private List<WebElement> redirectUriInputs;
    @FindBy(xpath = ".//i[contains(@data-ng-click, 'deleteRedirectUri')]")
    private List<WebElement> deleteRedirectUriIcons;

    public void setValues(ClientRepresentation client) {
        waitAjaxForElement(clientIdInput);

        setClientId(client.getClientId());
        setName(client.getName());
        setEnabled(client.isEnabled());
        setConsentRequired(client.isConsentRequired());
        setDirectGrantsOnly(client.isDirectGrantsOnly());
        setProtocol(client.getProtocol());
        if (OIDC.equals(client.getProtocol())) {
            setAccessType(client);
            if (!client.isBearerOnly()) {
                if (!client.isPublicClient()) {
                    setServiceAccountsEnabled(client.isServiceAccountsEnabled());
                }
                setRedirectUris(client.getRedirectUris());
            }
        }
    }

    public ClientRepresentation getValues() {
        ClientRepresentation values = new ClientRepresentation();
        values.setClientId(getClientId());
        values.setName(getName());
        values.setEnabled(isEnabled());
        values.setConsentRequired(isConsentRequired());
        values.setDirectGrantsOnly(isDirectGrantsOnly());
        values.setProtocol(getProtocol());
        if (OIDC.equals(values.getProtocol())) {
            values.setBearerOnly(isBearerOnly());
            if (!values.isBearerOnly()) {
                values.setPublicClient(isPublicClient());
                if (!values.isPublicClient()) {
                    values.setServiceAccountsEnabled(isServiceAccountsEnabled());
                }
                values.setRedirectUris(getRedirectUris());
            }
        }
        return values;
    }

    public String getClientId() {
        return getInputValue(clientIdInput);
    }

    public void setClientId(String clientId) {
        setInputValue(clientIdInput, clientId);
    }

    public String getName() {
        return getInputValue(nameInput);
    }

    public void setName(String name) {
        setInputValue(nameInput, name);
    }

    public boolean isEnabled() {
        return enabledSwitch.isOn();
    }

    public void setEnabled(boolean enabled) {
        enabledSwitch.setOn(enabled);
    }

    public static final String BEARER_ONLY = "bearer-only";
    public static final String PUBLIC = "public";
    public static final String CONFIDENTIAL = "confidential";

    public boolean isBearerOnly() {
        return BEARER_ONLY.equals(
                accessTypeSelect.getFirstSelectedOption().getAttribute(VALUE));
    }

    public boolean isPublicClient() {
        return PUBLIC.equals(
                accessTypeSelect.getFirstSelectedOption().getAttribute(VALUE));
    }

    public void setBearerOnly(boolean bearerOnly) {
        accessTypeSelectElement.sendKeys(BEARER_ONLY);
//        accessTypeSelect.selectByVisibleText(BEARER_ONLY);
    }

    public void setPublicClient(boolean publicClient) {
        accessTypeSelectElement.sendKeys(PUBLIC);
//        accessTypeSelect.selectByVisibleText(PUBLIC);
    }

    public void setAccessType(ClientRepresentation client) { // TODO verify
        setBearerOnly(client.isBearerOnly());
        setPublicClient(client.isPublicClient());
        if (!client.isBearerOnly() && !client.isPublicClient()) {
            accessTypeSelect.selectByVisibleText(CONFIDENTIAL);
        }
    }

    public void addRedirectUri(String redirectUri) {
        newRedirectUriInput.sendKeys(redirectUri);
    }

    public List<String> getRedirectUris() {
        List<String> values = new ArrayList<>();
        for (WebElement input : redirectUriInputs) {
            values.add(getInputValue(input));
        }
        return values;
    }

    public void setRedirectUris(List<String> redirectUris) {
        Timer.time();
        while (!deleteRedirectUriIcons.isEmpty()) {
            deleteRedirectUriIcons.get(0).click();
            pause(100);
        }
        Timer.time("deleteRedirectUris");
        if (redirectUris != null) {
            for (String redirectUri : redirectUris) {
                addRedirectUri(redirectUri);
                pause(100);
            }
        }
        Timer.time("addRedirectUris");
    }

    public boolean isConsentRequired() {
        return consentRequiredSwitch.isOn();
    }

    public void setConsentRequired(boolean consentRequired) {
        consentRequiredSwitch.setOn(consentRequired);
    }

    public boolean isDirectGrantsOnly() {
        return directGrantsOnlySwitch.isOn();
    }

    public void setDirectGrantsOnly(boolean directGrantsOnly) {
        directGrantsOnlySwitch.setOn(directGrantsOnly);
    }

    public String getProtocol() {
        waitAjaxForElement(protocolSelect.getFirstSelectedOption());
        return protocolSelect.getFirstSelectedOption().getText();
    }

    public void setProtocol(String protocol) {
        Timer.time();
        protocolSelectElement.sendKeys(protocol);
        Timer.time("clientSettings.setProtocol()");
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabledSwitch.isOn();
    }

    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        serviceAccountsEnabledSwitch.setOn(serviceAccountsEnabled);
    }

    public class SAMLClientSettingsForm extends Form {

        // TODO add SAML client attributes
    }

}
