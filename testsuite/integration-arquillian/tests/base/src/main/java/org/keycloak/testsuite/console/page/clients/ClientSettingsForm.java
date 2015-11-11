package org.keycloak.testsuite.console.page.clients;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.auth.page.login.Login.OIDC;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author tkyjovsk
 */
public class ClientSettingsForm extends CreateClientForm {

    @FindBy(id = "baseUrl")
    private WebElement baseUrlInput;
    @FindBy(id = "adminUrl")
    private WebElement adminUrlInput;

    @FindBy(id = "newWebOrigin")
    private WebElement newWebOriginInput;
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
        setBaseUrl(client.getBaseUrl());
        if (OIDC.equals(client.getProtocol())) {
            setAdminUrl(client.getAdminUrl());
            setWebOrigins(client.getWebOrigins());
        }
    }

    @Override
    public ClientRepresentation getValues() {
        ClientRepresentation values = super.getValues();
        values.setBaseUrl(getBaseUrl());
        if (OIDC.equals(values.getProtocol())) {
            values.setAdminUrl(getAdminUrl());
            values.setWebOrigins(getWebOrigins());
        }
        return values;
    }

    public void setConsentRequired(boolean value) {
        consentRequired.setOn(value);
    }

}
