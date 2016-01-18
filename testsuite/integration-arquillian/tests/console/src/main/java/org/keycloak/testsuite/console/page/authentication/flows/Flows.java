package org.keycloak.testsuite.console.page.authentication.flows;

import org.keycloak.testsuite.console.page.authentication.Authentication;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class Flows extends Authentication {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/flows";
    }

    @FindBy(tagName = "select")
    private Select flowSelect;

    @FindBy(xpath = "//button[text() = 'New']")
    private WebElement newButton;

    @FindBy(xpath = "//button[text() = 'Copy']")
    private WebElement copyButton;

    @FindBy(xpath = "//button[text() = 'Delete']")
    private WebElement deleteButton;

    @FindBy(xpath = "//button[text() = 'Add Execution']")
    private WebElement addExecutionButton;

    @FindBy(xpath = "//button[text() = 'Add Flow']")
    private WebElement addFlowButton;

    @FindBy(tagName = "table")
    private FlowsTable flowsTable;

    public enum FlowOption {

        DIRECT_GRANT("Direct grant"), 
        REGISTRATION("Registration"), 
        BROWSER("Browser"),
        RESET_CREDENTIALS("Reset credentials"), 
        CLIENTS("Clients");

        private final String name;

        private FlowOption(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public void selectFlowOption(FlowOption option) {
        flowSelect.selectByVisibleText(option.getName());
    }

    public String getFlowSelectValue() {
        return flowSelect.getFirstSelectedOption().getText();
    }

    public FlowsTable table() {
        return flowsTable;
    }

    public void clickNew() {
        newButton.click();
    }

    public void clickCopy() {
        copyButton.click();
    }

    public void clickDelete() {
        deleteButton.click();
    }

    public void clickAddExecution() {
        addExecutionButton.click();
    }

    public void clickAddFlow() {
        addFlowButton.click();
    }
}
