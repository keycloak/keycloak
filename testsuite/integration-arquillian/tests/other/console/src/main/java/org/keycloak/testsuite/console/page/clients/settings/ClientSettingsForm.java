package org.keycloak.testsuite.console.page.clients.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.page.clients.CreateClientForm;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.Timer;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

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

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='alwaysDisplayInConsole']]")
    private OnOffSwitch alwaysDisplayInConsole;
    
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
    @FindBy(xpath = ".//button[contains(@data-ng-click,'addRedirectUri')]")
    private WebElement newRedirectUriSubmit;
    @FindBy(xpath = ".//input[@ng-model='client.redirectUris[i]']")
    private List<WebElement> redirectUriInputs;
    @FindBy(xpath = ".//button[contains(@data-ng-click, 'deleteRedirectUri')]")
    private List<WebElement> deleteRedirectUriIcons;

    @FindBy(id = "newWebOrigin")
    private WebElement newWebOriginInput;
    @FindBy(xpath = ".//button[contains(@data-ng-click,'addWebOrigin')]")
    private WebElement newWebOriginSubmit;
    @FindBy(xpath = ".//input[ng-model='client.webOrigins[i]']")
    private List<WebElement> webOriginInputs;
    @FindBy(xpath = ".//button[contains(@data-ng-click, 'deleteWebOrigin')]")
    private List<WebElement> deleteWebOriginIcons;

    @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='authorizationServicesEnabled']]")
    private OnOffSwitch authorizationSettingsEnabledSwitch;

    @FindBy(xpath = ACTIVE_DIV_XPATH + "/button[text()='Disable Authorization Settings']")
    private WebElement confirmDisableAuthorizationSettingsButton;

    public enum OidcAccessType {
        BEARER_ONLY("bearer-only"), PUBLIC("public"), CONFIDENTIAL("confidential");

        private final String name;

        private OidcAccessType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public void setBaseUrl(String baseUrl) {
        UIUtils.setTextInputValue(baseUrlInput, baseUrl);
    }

    public String getBaseUrl() {
        return UIUtils.getTextInputValue(baseUrlInput);
    }

    public void setAdminUrl(String adminUrl) {
        UIUtils.setTextInputValue(adminUrlInput, adminUrl);
    }

    public String getAdminUrl() {
        return UIUtils.getTextInputValue(adminUrlInput);
    }

    public void addWebOrigin(String redirectUri) {
        newWebOriginInput.sendKeys(redirectUri);
        newWebOriginSubmit.click();
    }

    public List<String> getWebOrigins() {
        List<String> values = new ArrayList<>();
        for (WebElement input : webOriginInputs) {
            values.add(UIUtils.getTextInputValue(input));
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

    public String getName() {
        return UIUtils.getTextInputValue(nameInput);
    }

    public void setName(String name) {
        UIUtils.setTextInputValue(nameInput, name);
    }

    public boolean isEnabled() {
        return enabledSwitch.isOn();
    }

    public void setEnabled(boolean enabled) {
        enabledSwitch.setOn(enabled);
    }

    public boolean isAlwaysDisplayInConsole() {
        return alwaysDisplayInConsole.isOn();
    }

    public void setAlwaysDisplayInConsole(boolean enabled) {
        alwaysDisplayInConsole.setOn(enabled);
    }

    public boolean isAlwaysDisplayInConsoleVisible() {
        return alwaysDisplayInConsole.isVisible();
    }

    public boolean isConsentRequired() {
        return consentRequiredSwitch.isOn();
    }

    public void setConsentRequired(boolean consentRequired) {
        consentRequiredSwitch.setOn(consentRequired);
    }

    public void setAccessType(OidcAccessType accessType) {
        accessTypeSelect.selectByVisibleText(accessType.getName());
    }

    public void addRedirectUri(String redirectUri) {
        newRedirectUriInput.sendKeys(redirectUri);
        newRedirectUriSubmit.click();
    }

    public List<String> getRedirectUris() {
        List<String> values = new ArrayList<>();
        for (WebElement input : redirectUriInputs) {
            values.add(UIUtils.getTextInputValue(input));
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

    public void setAuthorizationSettingsEnabled(boolean enabled) {
        authorizationSettingsEnabledSwitch.setOn(enabled);
    }

    public boolean isAuthorizationSettingsEnabled() {
        return authorizationSettingsEnabledSwitch.isOn();
    }

    public void confirmDisableAuthorizationSettings() {
        confirmDisableAuthorizationSettingsButton.click();
    }

    public class SAMLClientSettingsForm extends Form {

        public static final String SAML_ASSERTION_SIGNATURE = "saml.assertion.signature";
        public static final String SAML_AUTHNSTATEMENT = "saml.authnstatement";
        public static final String SAML_ONETIMEUSE_CONDITION = "saml.onetimeuse.condition";
        public static final String SAML_CLIENT_SIGNATURE = "saml.client.signature";
        public static final String SAML_ENCRYPT = "saml.encrypt";
        public static final String SAML_FORCE_POST_BINDING = "saml.force.post.binding";
        public static final String SAML_MULTIVALUED_ROLES = "saml.multivalued.roles";
        public static final String SAML_SERVER_SIGNATURE = "saml.server.signature";
        public static final String SAML_SERVER_SIGNATURE_KEYINFO_EXT = "saml.server.signature.keyinfo.ext";
        public static final String SAML_SIGNATURE_ALGORITHM = "saml.signature.algorithm";
        public static final String SAML_ASSERTION_CONSUMER_URL_POST = "saml_assertion_consumer_url_post";
        public static final String SAML_ASSERTION_CONSUMER_URL_REDIRECT = "saml_assertion_consumer_url_redirect";
        public static final String SAML_FORCE_NAME_ID_FORMAT = "saml_force_name_id_format";
        public static final String SAML_NAME_ID_FORMAT = "saml_name_id_format";
        public static final String SAML_SIGNATURE_CANONICALIZATION_METHOD = "saml_signature_canonicalization_method";
        public static final String SAML_SINGLE_LOGOUT_SERVICE_URL_POST = "saml_single_logout_service_url_post";
        public static final String SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT = "saml_single_logout_service_url_redirect";

        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='samlAuthnStatement']]")
        private OnOffSwitch samlAuthnStatement;
        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='samlOneTimeUseCondition']]")
        private OnOffSwitch samlOneTimeUseCondition;
        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='samlServerSignature']]")
        private OnOffSwitch samlServerSignature;
        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='samlServerSignatureEnableKeyInfoExtension']]")
        private OnOffSwitch samlServerSignatureKeyInfoExt;
        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='samlAssertionSignature']]")
        private OnOffSwitch samlAssertionSignature;
        @FindBy(id = "signatureAlgorithm")
        private Select signatureAlgorithm;
        @FindBy(id = "canonicalization")
        private Select canonicalization;
        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='samlEncrypt']]")
        private OnOffSwitch samlEncrypt;
        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='samlClientSignature']]")
        private OnOffSwitch samlClientSignature;
        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='samlForcePostBinding']]")
        private OnOffSwitch samlForcePostBinding;
        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='frontchannelLogout']]")
        private OnOffSwitch frontchannelLogout;
        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='samlForceNameIdFormat']]")
        private OnOffSwitch samlForceNameIdFormat;
        @FindBy(id = "samlNameIdFormat")
        private Select samlNameIdFormat;

        @FindBy(xpath = "//fieldset[contains(@data-ng-show, 'saml')]//i")
        private WebElement fineGrainCollapsor;

        @FindBy(id = "consumerServicePost")
        private WebElement consumerServicePostInput;
        @FindBy(id = "consumerServiceRedirect")
        private WebElement consumerServiceRedirectInput;
        @FindBy(id = "logoutPostBinding")
        private WebElement logoutPostBindingInput;
        @FindBy(id = "logoutRedirectBinding")
        private WebElement logoutRedirectBindingInput;

        public void setValues(ClientRepresentation client) {
            waitUntilElement(fineGrainCollapsor).is().visible();

            Map<String, String> attributes = client.getAttributes();
            samlAuthnStatement.setOn("true".equals(attributes.get(SAML_AUTHNSTATEMENT)));
            samlOneTimeUseCondition.setOn("true".equals(attributes.get(SAML_ONETIMEUSE_CONDITION)));
            samlServerSignature.setOn("true".equals(attributes.get(SAML_SERVER_SIGNATURE)));
            samlAssertionSignature.setOn("true".equals(attributes.get(SAML_ASSERTION_SIGNATURE)));
            if (samlServerSignature.isOn() || samlAssertionSignature.isOn()) {
                signatureAlgorithm.selectByVisibleText(attributes.get(SAML_SIGNATURE_ALGORITHM));
                canonicalization.selectByValue("string:" + attributes.get(SAML_SIGNATURE_CANONICALIZATION_METHOD));
                samlServerSignatureKeyInfoExt.setOn("true".equals(attributes.get(SAML_SERVER_SIGNATURE_KEYINFO_EXT)));
            }
            samlEncrypt.setOn("true".equals(attributes.get(SAML_ENCRYPT)));
            samlClientSignature.setOn("true".equals(attributes.get(SAML_CLIENT_SIGNATURE)));
            samlForcePostBinding.setOn("true".equals(attributes.get(SAML_FORCE_POST_BINDING)));
            frontchannelLogout.setOn(client.isFrontchannelLogout());
            samlForceNameIdFormat.setOn("true".equals(attributes.get(SAML_FORCE_NAME_ID_FORMAT)));
            samlNameIdFormat.selectByVisibleText(attributes.get(SAML_NAME_ID_FORMAT));

            fineGrainCollapsor.click();
            waitUntilElement(consumerServicePostInput).is().present();

            UIUtils.setTextInputValue(consumerServicePostInput, attributes.get(SAML_ASSERTION_CONSUMER_URL_POST));
            UIUtils.setTextInputValue(consumerServiceRedirectInput, attributes.get(SAML_ASSERTION_CONSUMER_URL_REDIRECT));
            UIUtils.setTextInputValue(logoutPostBindingInput, attributes.get(SAML_SINGLE_LOGOUT_SERVICE_URL_POST));
            UIUtils.setTextInputValue(logoutRedirectBindingInput, attributes.get(SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT));
        }
    }

}
