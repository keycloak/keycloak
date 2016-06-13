package org.keycloak.testsuite.console.page.federation;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;

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

    @FindBy(id = "ldapBindCredential")
    private WebElement ldapBindCredentialInput;

    @FindBy(id = "customUserSearchFilter")
    private WebElement customUserSearchFilterInput;

    @FindBy(id = "searchScope")
    private Select searchScopeSelect;

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

    public void setConsoleDisplayNameInput(String name) {
        setInputValue(consoleDisplayNameInput, name);
    }

    public void setPriorityInput(Integer priority) {
        setInputValue(priorityInput, String.valueOf(priority));
    }

    public void setUsernameLDAPAttributeInput(String usernameLDAPAttribute) {
        setInputValue(usernameLDAPAttributeInput, usernameLDAPAttribute);
    }

    public void setRdnLDAPAttributeInput(String rdnLDAPAttribute) {
        setInputValue(rdnLDAPAttributeInput, rdnLDAPAttribute);
    }

    public void setUuidLDAPAttributeInput(String uuidLDAPAttribute) {
        setInputValue(uuidLDAPAttributeInput, uuidLDAPAttribute);
    }

    public void setUserObjectClassesInput(String userObjectClasses) {
        setInputValue(userObjectClassesInput, userObjectClasses);
    }

    public void setLdapConnectionUrlInput(String ldapConnectionUrl) {
        setInputValue(ldapConnectionUrlInput, ldapConnectionUrl);
    }

    public void setLdapUserDnInput(String ldapUserDn) {
        setInputValue(ldapUserDnInput, ldapUserDn);
    }

    public void setLdapBindDnInput(String ldapBindDn) {
        setInputValue(ldapBindDnInput, ldapBindDn);
    }

    public void setLdapBindCredentialInput(String ldapBindCredential) {
        setInputValue(ldapBindCredentialInput, ldapBindCredential);
    }

    public void setCustomUserSearchFilter(String customUserSearchFilter) {
        setInputValue(customUserSearchFilterInput, customUserSearchFilter);
    }

    public void setKerberosRealmInput(String kerberosRealm) {
        setInputValue(kerberosRealmInput, kerberosRealm);
    }

    public void setServerPrincipalInput(String serverPrincipal) {
        setInputValue(serverPrincipalInput, serverPrincipal);
    }

    public void setKeyTabInput(String keyTab) {
        setInputValue(keyTabInput, keyTab);
    }

    public void setBatchSizeForSyncInput(String batchSizeForSync) {
        setInputValue(batchSizeForSyncInput, batchSizeForSync);
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
            String text = vendorElement.getText();
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

    public void testConnection() {
        testConnectionButton.click();
    }

    public void testAuthentication() {
        testAuthenticationButton.click();
    }

    public void synchronizeAllUsers() {
        waitUntilElement(synchronizeAllUsersButton).is().present();
        synchronizeAllUsersButton.click();
    }
}
