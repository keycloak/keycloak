package org.keycloak.testsuite.console.page.clients.settings;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.util.Timer;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.testsuite.console.page.clients.CreateClientForm;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.auth.page.login.Login.OIDC;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.OidcAccessType.BEARER_ONLY;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.OidcAccessType.CONFIDENTIAL;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.OidcAccessType.PUBLIC;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author tkyjovsk
 */
public class ClientSettingsForm extends CreateClientForm {

    @FindBy(id = "name")
    private WebElement nameInput;

    @FindBy(id = "baseUrl")
    private WebElement baseUrlInput;
    @FindBy(id = "adminUrl")
    private WebElement adminUrlInput;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='enabled']]")
    private OnOffSwitch enabledSwitch;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='consentRequired']]")
    private OnOffSwitch consentRequiredSwitch;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='standardFlowEnabled']]")
    private OnOffSwitch standardFlowEnabledSwitch;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='implicitFlowEnabled']]")
    private OnOffSwitch implicitFlowEnabledSwitch;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='directAccessGrantsEnabled']]")
    private OnOffSwitch directAccessGrantsEnabledSwitch;

    @FindBy(id = "accessType")
    private Select accessTypeSelect;
    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='serviceAccountsEnabled']]")
    private OnOffSwitch serviceAccountsEnabledSwitch;

    @FindBy(id = "newRedirectUri")
    private WebElement newRedirectUriInput;
    @FindBy(xpath = ".//i[contains(@data-ng-click, 'newRedirectUri')]")
    private WebElement newRedirectUriSubmit;
    @FindBy(xpath = ".//input[@ng-model='client.redirectUris[i]']")
    private List<WebElement> redirectUriInputs;
    @FindBy(xpath = ".//i[contains(@data-ng-click, 'deleteRedirectUri')]")
    private List<WebElement> deleteRedirectUriIcons;

    @FindBy(id = "newWebOrigin")
    private WebElement newWebOriginInput;
    @FindBy(xpath = ".//i[contains(@data-ng-click, 'newWebOrigin')]")
    private WebElement newWebOriginSubmit;
    @FindBy(xpath = ".//input[ng-model='client.webOrigins[i]']")
    private List<WebElement> webOriginInputs;
    @FindBy(xpath = ".//i[contains(@data-ng-click, 'deleteWebOrigin')]")
    private List<WebElement> deleteWebOriginIcons;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='consentRequired']]")
    private OnOffSwitch consentRequired;

    public void setBaseUrl(String baseUrl) {
        setInputValue(baseUrlInput, baseUrl);
    }

    public String getBaseUrl() {
        return getInputValue(baseUrlInput);
    }

    public void setAdminUrl(String adminUrl) {
        setInputValue(adminUrlInput, adminUrl);
    }

    public String getAdminUrl() {
        return getInputValue(adminUrlInput);
    }

    public void addWebOrigin(String redirectUri) {
        newWebOriginInput.sendKeys(redirectUri);
        newWebOriginSubmit.click();
    }

    public List<String> getWebOrigins() {
        List<String> values = new ArrayList<>();
        for (WebElement input : webOriginInputs) {
            values.add(getInputValue(input));
        }
        return values;
    }

    public void setWebOrigins(List<String> webOrigins) {
        while (!deleteWebOriginIcons.isEmpty()) {
            deleteWebOriginIcons.get(0).click();
            pause(100);
        }
        if (webOrigins != null) {
            for (String redirectUri : webOrigins) {
                addWebOrigin(redirectUri);
                pause(100);
            }
        }
    }

    @Override
    public void setValues(ClientRepresentation client) {
        super.setValues(client);
        setName(client.getName());
        setEnabled(client.isEnabled());
        setConsentRequired(client.isConsentRequired());
        setBaseUrl(client.getBaseUrl());
        if (OIDC.equals(client.getProtocol())) {
            setAccessType(client);
            if (!client.isBearerOnly()) {
                setStandardFlowEnabled(client.isStandardFlowEnabled());
                setDirectAccessGrantsEnabled(client.isDirectAccessGrantsEnabled());
                if (client.isPublicClient()) {
                    setImplicitFlowEnabled(client.isImplicitFlowEnabled());
                } else {//confidential
                    setServiceAccountsEnabled(client.isServiceAccountsEnabled());
                }
                if (client.isStandardFlowEnabled() || client.isImplicitFlowEnabled()) {
                    setRedirectUris(client.getRedirectUris());
                }
            }
            setAdminUrl(client.getAdminUrl());
            setWebOrigins(client.getWebOrigins());
        }
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

    public boolean isConsentRequired() {
        return consentRequiredSwitch.isOn();
    }

    public void setConsentRequired(boolean consentRequired) {
        consentRequiredSwitch.setOn(consentRequired);
    }

    public void setAccessType(ClientRepresentation client) {
        if (client.isBearerOnly()) {
            accessTypeSelect.selectByVisibleText(BEARER_ONLY.getName());
        } else if (client.isPublicClient()) {
            accessTypeSelect.selectByVisibleText(PUBLIC.getName());
        } else {
            accessTypeSelect.selectByVisibleText(CONFIDENTIAL.getName());
        }
    }

    public void addRedirectUri(String redirectUri) {
        newRedirectUriInput.sendKeys(redirectUri);
        newRedirectUriSubmit.click();
    }

    public List<String> getRedirectUris() {
        List<String> values = new ArrayList<>();
        for (WebElement input : redirectUriInputs) {
            values.add(getInputValue(input));
        }
        return values;
    }

    public void setRedirectUris(List<String> redirectUris) {
        Timer.DEFAULT.reset();
        while (!deleteRedirectUriIcons.isEmpty()) {
            deleteRedirectUriIcons.get(0).click();
            pause(100);
        }
        Timer.DEFAULT.reset("deleteRedirectUris");
        if (redirectUris != null) {
            for (String redirectUri : redirectUris) {
                addRedirectUri(redirectUri);
                pause(100);
            }
        }
        Timer.DEFAULT.reset("addRedirectUris");
    }

    public boolean isStandardFlowEnabled() {
        return standardFlowEnabledSwitch.isOn();
    }

    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        standardFlowEnabledSwitch.setOn(standardFlowEnabled);
    }

    public boolean isImplicitFlowEnabled() {
        return implicitFlowEnabledSwitch.isOn();
    }

    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        implicitFlowEnabledSwitch.setOn(implicitFlowEnabled);
    }

    public boolean isDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabledSwitch.isOn();
    }

    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        directAccessGrantsEnabledSwitch.setOn(directAccessGrantsEnabled);
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabledSwitch.isOn();
    }

    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        serviceAccountsEnabledSwitch.setOn(serviceAccountsEnabled);
    }

}
