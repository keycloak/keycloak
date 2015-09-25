package org.keycloak.testsuite.console.page.authentication;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * Created by mhajas on 8/21/15.
 */
public class Bindings extends Authentication {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/flow-binding";
    }

    @FindBy(id = "browser")
    private Select BrowserFlowSelect;

    @FindBy(id = "registration")
    private Select RegistrationFlowSelect;

    @FindBy(id = "grant")
    private Select DirectGrantFlowSelect;

    @FindBy(id = "resetCredentials")
    private Select ResetCredentialsSelect;

    @FindBy(id = "clientAuthentication")
    private Select ClientAuthenticationSelect;

    @FindBy(xpath = "//button[text()='Save']")
    private WebElement saveButton;

    @FindBy(xpath = "//button[text()='Cancel']")
    private WebElement cancelButton;

    public void changeBrowserFlowSelect(BrowserFlowSelectValues value) {
        BrowserFlowSelect.selectByVisibleText(value.getName());
    }

    public void changeRegistrationFlowSelect(RegistrationFlowSelectValues value) {
        RegistrationFlowSelect.selectByVisibleText(value.getName());
    }

    public void changeDirectGrantFlowSelect(DirectGrantFlowSelectValues value) {
        DirectGrantFlowSelect.selectByVisibleText(value.getName());
    }

    public void changeResetCredentialsSelect(ResetCredentialsSelectValues value) {
        ResetCredentialsSelect.selectByVisibleText(value.getName());
    }

    public void changeClientAuthenticationSelect(ClientAuthenticationSelectValues value) {
        ClientAuthenticationSelect.selectByVisibleText(value.getName());
    }

    public void clickSave() {
        saveButton.click();
    }

    public void clickCancel() {
        cancelButton.click();
    }

    public enum BrowserFlowSelectValues {

        DIRECT_GRANT("direct grant"), REGISTRATION("registration"), BROWSER("browser"),
        RESET_CREDENTIALS("reset credentials");

        private String name;

        private BrowserFlowSelectValues(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum RegistrationFlowSelectValues {

        DIRECT_GRANT("direct grant"), REGISTRATION("registration"), BROWSER("browser"),
        RESET_CREDENTIALS("reset credentials");

        private String name;

        private RegistrationFlowSelectValues(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum DirectGrantFlowSelectValues {

        DIRECT_GRANT("direct grant"), REGISTRATION("registration"), BROWSER("browser"),
        RESET_CREDENTIALS("reset credentials");

        private String name;

        private DirectGrantFlowSelectValues(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum ResetCredentialsSelectValues {

        DIRECT_GRANT("direct grant"), REGISTRATION("registration"), BROWSER("browser"),
        RESET_CREDENTIALS("reset credentials"), NOTHING("");

        private String name;

        private ResetCredentialsSelectValues(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum ClientAuthenticationSelectValues {

        CLIENTS("clients");

        private String name;

        private ClientAuthenticationSelectValues(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }


}
