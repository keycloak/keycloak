package org.keycloak.testsuite.console.page.federation;

import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author pdrozd
 */
public class KerberosUserProviderForm extends Form {

    @FindBy(id = "consoleDisplayName")
    private WebElement consoleDisplayNameInput;

    @FindBy(id = "priority")
    private WebElement priorityInput;

    @FindBy(id = "kerberosRealm")
    private WebElement kerberosRealmInput;

    @FindBy(id = "serverPrincipal")
    private WebElement serverPrincipalInput;

    @FindBy(id = "keyTab")
    private WebElement keyTabInput;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='debug']]")
    private OnOffSwitch debug;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='allowPasswordAuthentication']]")
    private OnOffSwitch allowPwdAuth;

    @FindBy(id = "editMode")
    private Select editModeSelect;

    @FindBy(xpath = ".//div[contains(@class,'onoffswitch') and ./input[@id='updateProfileFirstLogin']]")
    private OnOffSwitch updateProfileFirstLogin;

    public void setConsoleDisplayNameInput(String name) {
        setInputValue(consoleDisplayNameInput, name);
    }

    public String getConsoleDisplayNameInput() {
        return getInputValue(consoleDisplayNameInput);
    }

    public void setPriorityInput(Integer priority) {
        setInputValue(priorityInput, String.valueOf(priority));
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

    public void setDebugEnabled(boolean debugEnabled) {
        this.debug.setOn(debugEnabled);
    }

    public void setAllowPasswordAuthentication(boolean enabled) {
        allowPwdAuth.setOn(enabled);
    }

    public void selectEditMode(String mode) {
        editModeSelect.selectByVisibleText(mode);
    }

    public void setUpdateProfileFirstLogin(boolean enabled) {
        updateProfileFirstLogin.setOn(enabled);
    }
}
