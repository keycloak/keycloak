package org.keycloak.testsuite.console.page.federation;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.clickBtnAndWaitForAlert;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author fkiss, pdrozd
 */
public class LdapUserProviderForm extends Form {

    @FindBy(id = "consoleDisplayName")
    private WebElement consoleDisplayNameInput;

    @FindBy(id = "priority")
    private WebElement priorityInput;

    @FindBy(id = "usernameLDAPAttribute")
    private WebElement usernameLDAPAttributeInput;

    @FindBy(id = "rdnLDAPAttribute")
    private WebElement rdnLDAPAttributeInput;

    @FindBy(id = "uuidLDAPAttribute")
    private WebElement uuidLDAPAttributeInput;

    @FindBy(id = "userObjectClasses")
    private WebElement userObjectClassesInput;

    @FindBy(id = "ldapConnectionUrl")
    private WebElement ldapConnectionUrlInput;

    @FindBy(id = "ldapUsersDn")
    private WebElement ldapUserDnInput;

    @FindBy(id = "authType")
    private Select authTypeSelect;

    @FindBy(id = "ldapBindDn")
    private WebElement ldapBindDnInput;

    @FindBy(id = "ldapBindCred")
    private WebElement ldapBindCredentialInput;

    @FindBy(id = "customUserSearchFilter")
    private WebElement customUserSearchFilterInput;

    @FindBy(id = "searchScope")
    private Select searchScopeSelect;

    @FindBy(id = "kerberosIntegrationHeader")
    private WebElement kerberosIntegrationHeader;

    @FindBy(id = "kerberosRealm")
    private WebElement kerberosRealmInput;

    @FindBy(id = "serverPrincipal")
    private WebElement serverPrincipalInput;

    @FindBy(id = "keyTab")
    private WebElement keyTabInput;

    @FindBy(id = "batchSizeForSync")
    private WebElement batchSizeForSyncInput;

    @FindBy(id = "fullSyncPeriod")
    private WebElement fullSyncPeriodInput;

    @FindBy(id = "changedSyncPeriod")
    private WebElement changedSyncPeriodInput;

    @FindBy(id = "editMode")
    private Select editModeSelect;

    @FindBy(id = "vendor")
    private Select vendorSelect;

    @FindByJQuery("a:contains('Test connection')")
    private WebElement testConnectionButton;

    @FindByJQuery("a:contains('Test authentication')")
    private WebElement testAuthenticationButton;

    @FindByJQuery("a:contains('Synchronize changed users')")
    private WebElement synchronizeChangedUsersButton;

    @FindByJQuery("button:contains('Synchronize all users')")
    private WebElement synchronizeAllUsersButton;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='syncRegistrations']]")
    private OnOffSwitch syncRegistrations;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='connectionPooling']]")
    private OnOffSwitch connectionPooling;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='pagination']]")
    private OnOffSwitch pagination;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='userAccountControlsAfterPasswordUpdate']]")
    private OnOffSwitch enableAccountAfterPasswordUpdate;

    @FindBy(xpath = "//div[contains(@class,'onoffswitch') and ./input[@id='allowKerberosAuthentication']]")
    private OnOffSwitch allowKerberosAuth;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='debug']]")
    private OnOffSwitch debug;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='useKerberosForPasswordAuthentication']]")
    private OnOffSwitch useKerberosForPwdAuth;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='compositeSwitch']]")
    private OnOffSwitch periodicFullSync;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='changedSyncEnabled']]")
    private OnOffSwitch periodicChangedUsersSync;

    @FindBy(id = "connectionPoolSettingsHeader")
    private WebElement connectionPoolingSettingsButton;

    @FindBy(id = "connectionPoolingAuthentication")
    private WebElement connectionPoolingAuthenticationInput;

    @FindBy(id = "connectionPoolingDebug")
    private WebElement connectionPoolingDebugInput;

    @FindBy(id = "connectionPoolingInitSize")
    private WebElement connectionPoolingInitSizeInput;

    @FindBy(id = "connectionPoolingMaxSize")
    private WebElement connectionPoolingMaxSizeInput;

    @FindBy(id = "connectionPoolingPrefSize")
    private WebElement connectionPoolingPrefSizeInput;

    @FindBy(id = "connectionPoolingProtocol")
    private WebElement connectionPoolingProtocolInput;

    @FindBy(id = "connectionPoolingTimeout")
    private WebElement connectionPoolingTimeoutInput;

    public void setConsoleDisplayNameInput(String name) {
        UIUtils.setTextInputValue(consoleDisplayNameInput, name);
    }

    public void setPriorityInput(Integer priority) {
        UIUtils.setTextInputValue(priorityInput, String.valueOf(priority));
    }

    public void setUsernameLDAPAttributeInput(String usernameLDAPAttribute) {
        UIUtils.setTextInputValue(usernameLDAPAttributeInput, usernameLDAPAttribute);
    }

    public void setRdnLDAPAttributeInput(String rdnLDAPAttribute) {
        UIUtils.setTextInputValue(rdnLDAPAttributeInput, rdnLDAPAttribute);
    }

    public void setUuidLDAPAttributeInput(String uuidLDAPAttribute) {
        UIUtils.setTextInputValue(uuidLDAPAttributeInput, uuidLDAPAttribute);
    }

    public void setUserObjectClassesInput(String userObjectClasses) {
        UIUtils.setTextInputValue(userObjectClassesInput, userObjectClasses);
    }

    public void setLdapConnectionUrlInput(String ldapConnectionUrl) {
        UIUtils.setTextInputValue(ldapConnectionUrlInput, ldapConnectionUrl);
    }

    public void setLdapUserDnInput(String ldapUserDn) {
        UIUtils.setTextInputValue(ldapUserDnInput, ldapUserDn);
    }

    public void setLdapBindDnInput(String ldapBindDn) {
        UIUtils.setTextInputValue(ldapBindDnInput, ldapBindDn);
    }

    public void setLdapBindCredentialInput(String ldapBindCredential) {
        UIUtils.setTextInputValue(ldapBindCredentialInput, ldapBindCredential);
    }

    public void setCustomUserSearchFilter(String customUserSearchFilter) {
        UIUtils.setTextInputValue(customUserSearchFilterInput, customUserSearchFilter);
    }

    public void uncollapseKerberosIntegrationHeader() {
        if (UIUtils.isElementVisible(kerberosRealmInput)) {
            // Already collapsed
            return;
        }

        kerberosIntegrationHeader.click();
        waitUntilElement(By.id("kerberosRealm")).is().present();
    }

    public void setKerberosRealmInput(String kerberosRealm) {
        UIUtils.setTextInputValue(kerberosRealmInput, kerberosRealm);
    }

    public void setServerPrincipalInput(String serverPrincipal) {
        UIUtils.setTextInputValue(serverPrincipalInput, serverPrincipal);
    }

    public void setKeyTabInput(String keyTab) {
        UIUtils.setTextInputValue(keyTabInput, keyTab);
    }

    public void setBatchSizeForSyncInput(String batchSizeForSync) {
        UIUtils.setTextInputValue(batchSizeForSyncInput, batchSizeForSync);
    }

    public void selectEditMode(String mode) {
        waitUntilElement(By.id("editMode")).is().present();
        editModeSelect.selectByVisibleText(mode);
    }

    public void selectVendor(String vendor) {
        waitUntilElement(By.id("vendor")).is().present();
        vendorSelect.selectByVisibleText(vendor);
    }

    public void selectVendor(int index) {
        waitUntilElement(By.id("vendor")).is().present();
        vendorSelect.selectByIndex(index);
    }

    public List<String> getVendors() {
        waitUntilElement(By.id("vendor")).is().present();

        List<WebElement> vendorsElements = vendorSelect.getOptions();
        List<String> vendorsString = new ArrayList<>();

        for (WebElement vendorElement : vendorsElements) {
            String text = getTextFromElement(vendorElement);
            if (text.equals("")) {continue;}
            vendorsString.add(text);
        }

        return vendorsString;
    }

    public void selectAuthenticationType(String authenticationType) {
        waitUntilElement(By.id("authType")).is().present();
        authTypeSelect.selectByVisibleText(authenticationType);
    }

    public void selectSearchScope(String searchScope) {
        waitUntilElement(By.id("searchScope")).is().present();
        searchScopeSelect.selectByVisibleText(searchScope);
    }

    public void setSyncRegistrationsEnabled(boolean syncRegistrationsEnabled) {
        this.syncRegistrations.setOn(syncRegistrationsEnabled);
    }

    public void setConnectionPoolingEnabled(boolean connectionPoolingEnabled) {
        this.connectionPooling.setOn(connectionPoolingEnabled);
    }

    public void setPaginationEnabled(boolean paginationEnabled) {
        this.pagination.setOn(paginationEnabled);
    }

    public void setAccountAfterPasswordUpdateEnabled(boolean enabled) {
        enableAccountAfterPasswordUpdate.setOn(enabled);
    }

    public void setAllowKerberosAuthEnabled(boolean enabled) {
        allowKerberosAuth.setOn(enabled);
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debug.setOn(debugEnabled);
    }

    public void setUseKerberosForPwdAuthEnabled(boolean useKerberosForPwdAuthEnabled) {
        this.useKerberosForPwdAuth.setOn(useKerberosForPwdAuthEnabled);
    }

    public void setPeriodicFullSyncEnabled(boolean periodicFullSyncEnabled) {
        this.periodicFullSync.setOn(periodicFullSyncEnabled);
    }

    public void setPeriodicChangedUsersSyncEnabled(boolean periodicChangedUsersSyncEnabled) {
        this.periodicChangedUsersSync.setOn(periodicChangedUsersSyncEnabled);
    }

    public void connectionPoolingSettings() {
        connectionPoolingSettingsButton.click();
    }

    public void setConnectionPoolingAuthentication(String connectionPoolingAuthentication) {
        UIUtils.setTextInputValue(connectionPoolingAuthenticationInput, connectionPoolingAuthentication);
    }

    public void setConnectionPoolingDebug(String connectionPoolingDebug) {
        UIUtils.setTextInputValue(connectionPoolingDebugInput, connectionPoolingDebug);
    }

    public void setConnectionPoolingInitSize(String connectionPoolingInitSize) {
        UIUtils.setTextInputValue(connectionPoolingInitSizeInput, connectionPoolingInitSize);
    }

    public void setConnectionPoolingMaxSize(String connectionPoolingMaxSize) {
        UIUtils.setTextInputValue(connectionPoolingMaxSizeInput, connectionPoolingMaxSize);
    }

    public void setConnectionPoolingPrefSize(String connectionPoolingPrefSize) {
        UIUtils.setTextInputValue(connectionPoolingPrefSizeInput, connectionPoolingPrefSize);
    }

    public void setConnectionPoolingProtocol(String connectionPoolingProtocol) {
        UIUtils.setTextInputValue(connectionPoolingProtocolInput, connectionPoolingProtocol);
    }

    public void setConnectionPoolingTimeout(String connectionPoolingTimeout) {
        UIUtils.setTextInputValue(connectionPoolingTimeoutInput, connectionPoolingTimeout);
    }

    public void testConnection() {
        clickBtnAndWaitForAlert(testConnectionButton);
    }

    public void testAuthentication() {
        clickBtnAndWaitForAlert(testAuthenticationButton);
    }

    public void synchronizeAllUsers() {
        waitUntilElement(synchronizeAllUsersButton).is().present();
        clickBtnAndWaitForAlert(synchronizeAllUsersButton);
    }
}
