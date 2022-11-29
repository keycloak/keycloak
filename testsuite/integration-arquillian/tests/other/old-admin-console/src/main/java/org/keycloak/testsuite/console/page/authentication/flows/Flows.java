package org.keycloak.testsuite.console.page.authentication.flows;

import org.keycloak.testsuite.console.page.authentication.Authentication;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.performOperationWithPageReload;

/**
 * @author tkyjovsk
 * @author mhajas
 * @author pzaoral
 */
public class Flows extends Authentication {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/flows";
    }

    @FindBy(tagName = "select")
    private Select flowSelect;

    @FindBy(xpath = ".//button[@data-ng-click='createFlow()']")
    private WebElement newButton;

    @FindBy(xpath = ".//button[@data-ng-click='copyFlow()']")
    private WebElement copyButton;

    @FindBy(xpath = ".//button[@data-ng-click='deleteFlow()']")
    private WebElement deleteButton;

    @FindBy(xpath = ".//button[@data-ng-click='addExecution()']")
    private WebElement addExecutionButton;

    @FindBy(xpath = ".//button[@data-ng-click='addFlow()']")
    private WebElement addFlowButton;

    @FindBy(tagName = "table")
    private FlowsTable flowsTable;

    public enum FlowOption {

        DIRECT_GRANT("Direct Grant"),
        REGISTRATION("Registration"), 
        BROWSER("Browser"),
        RESET_CREDENTIALS("Reset Credentials"),
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
        performOperationWithPageReload(() -> flowSelect.selectByVisibleText(option.getName()));
    }

    public String getFlowSelectValue() {
        return getTextFromElement(flowSelect.getFirstSelectedOption());
    }

    public List<String> getFlowAllValues() {
        return flowSelect.getOptions().stream().map(WebElement::getText).collect(Collectors.toList());
    }

    public FlowsTable table() {
        return flowsTable;
    }

    public void clickNew() {
        clickLink(newButton);
    }

    public void clickCopy() {
        clickLink(copyButton);
    }

    public void clickDelete() {
        clickLink(deleteButton);
    }

    public void clickAddExecution() {
        clickLink(addExecutionButton);
    }

    public void clickAddFlow() {
        clickLink(addFlowButton);
    }
}
