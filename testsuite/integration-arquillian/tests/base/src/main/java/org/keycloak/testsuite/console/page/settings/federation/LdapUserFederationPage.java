package org.keycloak.testsuite.console.page.settings.federation;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.SeleniumUtils.waitGuiForElement;

/**
 * Created by fkiss.
 */
public class LdapUserFederationPage extends AdminConsole {

    @FindBy(id = "consoleDisplayName")
    private WebElement consoleDisplayNameInput;

    @FindBy(id = "priority")
    private WebElement priorityInput;

    @FindBy(id = "usernameLDAPAttribute")
    private WebElement usernameLDAPAttributeInput;

    @FindBy(id = "userObjectClasses")
    private WebElement userObjectClassesInput;

    @FindBy(id = "ldapConnectionUrl")
    private WebElement ldapConnectionUrlInput;

    @FindBy(id = "ldapBaseDn")
    private WebElement ldapBaseDnInput;

    @FindBy(id = "ldapUserDnSuffix")
    private WebElement ldapUserDnSuffixInput;

    @FindBy(id = "ldapBindDn")
    private WebElement ldapBindDnInput;

    @FindBy(id = "ldapBindCredential")
    private WebElement ldapBindCredentialInput;

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

    @FindByJQuery("div[class='onoffswitch']:eq(0)")
    private OnOffSwitch syncRegistrations;

    @FindByJQuery("div[class='onoffswitch']:eq(1)")
    private OnOffSwitch connectionPooling;

    @FindByJQuery("div[class='onoffswitch']:eq(2)")
    private OnOffSwitch pagination;

    @FindByJQuery("div[class='onoffswitch']:eq(3)")
    private OnOffSwitch allowKerberosAuth;

    @FindByJQuery("div[class='onoffswitch']:eq(4)")
    private OnOffSwitch debug;

    @FindByJQuery("div[class='onoffswitch']:eq(5)")
    private OnOffSwitch useKerberosForPwdAuth;

    @FindByJQuery("div[class='onoffswitch']:eq(6)")
    private OnOffSwitch periodicFullSync;

    @FindByJQuery("div[class='onoffswitch']:eq(7)")
    private OnOffSwitch periodicChangedUsersSync;

    public void selectEditMode(String mode){
        waitGuiForElement(By.id("editMode"));
        editModeSelect.selectByVisibleText(mode);
    }

    public void selectVendor(String vendor){
        waitGuiForElement(By.id("editMode"));
        vendorSelect.selectByVisibleText(vendor);
    }
}
