package org.keycloak.testsuite.console.page.authentication;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * Created by mhajas on 8/21/15.
 */
public class Bindings extends Authentication{

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/flow-binding";
    }

    @FindBy(id = "browser")
    private Select BrowserFlowSelect;

    public void changeBrowserFlowSelect(BrowserFlowSelectValues value) {
        BrowserFlowSelect.selectByVisibleText(value.getName());
    }

    @FindBy(id = "registration")
    private Select RegistrationFlowSelect;

    public void changeRegistrationFlowSelect(RegistrationFlowSelectValues value) {
        RegistrationFlowSelect.selectByVisibleText(value.getName());
    }

    @FindBy(id = "grant")
    private Select DirectGrantFlowSelect;

    public void changeDirectGrantFlowSelect(DirectGrantFlowSelectValues value) {
        DirectGrantFlowSelect.selectByVisibleText(value.getName());
    }

    @FindBy(id = "resetCredentials")
    private Select ResetCredentialsSelect;

    public void changeResetCredentialsSelect(ResetCredentialsSelectValues value) {
        ResetCredentialsSelect.selectByVisibleText(value.getName());
    }

    @FindBy(id = "clientAuthentication")
    private Select ClientAuthenticationSelect;

    public void changeClientAuthenticationSelect(ClientAuthenticationSelectValues value) {
        ClientAuthenticationSelect.selectByVisibleText(value.getName());
    }

    @FindBy(xpath = "//button[text()='Save']")
    private WebElement saveButton;

    public void clickSave() {
        saveButton.click();
    }

    @FindBy(xpath = "//button[text()='Cancel']")
    private WebElement cancelButton;

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
