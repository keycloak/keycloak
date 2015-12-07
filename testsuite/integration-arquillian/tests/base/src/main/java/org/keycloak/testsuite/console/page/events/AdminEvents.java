package org.keycloak.testsuite.console.page.events;

import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class AdminEvents extends Events {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/admin-events";
    }

    @FindBy(tagName = "table")
    private AdminEventsTable table;

    public AdminEventsTable table() {
        return table;
    }

    public class AdminEventsTable extends DataTable {

        @FindBy(xpath = "//button[text()[contains(.,'Filter')]]")
        private WebElement filterButton;

        @FindBy(tagName = "form")
        private AdminEventsTableFilterForm filterForm;

        public void update() {
            waitForBody();
            clickHeaderButton("Update");
        }

        public void reset() {
            waitForBody();
            clickHeaderButton("Reset");
        }

        public void filter() {
            waitForBody();
            filterButton.click();
        }

        public AdminEventsTableFilterForm filterForm() {
            return filterForm;
        }

        public class AdminEventsTableFilterForm extends Form {

            @FindBy(id = "resource")
            private WebElement resourcePathInput;

            @FindBy(id = "realm")
            private WebElement realmInput;

            @FindBy(id = "client")
            private WebElement clientInput;

            @FindBy(id = "user")
            private WebElement userInput;

            @FindBy(id = "ipAddress")
            private WebElement ipAddressInput;

            @FindBy(xpath = "//div[@id='s2id_adminEnabledEventOperations']/ul")
            private WebElement operationTypesInput;

            @FindBy(xpath = "//div[@id='select2-drop']")
            private WebElement operationTypesValues;

            public void addOperationType(String type) {
                operationTypesInput.click();
                operationTypesValues.findElement(By.xpath("//div[text() = '" + type + "']")).click();
            }

            public void removeOperationType(String type) {
                operationTypesInput.findElement(By.xpath("//div[text()='" + type + "']/../a")).click();
            }

            public void setResourcePathInput(String value) {
                setInputValue(resourcePathInput, value);
            }

            public void setRealmInput(String value) {
                setInputValue(realmInput, value);
            }

            public void setClientInput(String value) {
                setInputValue(clientInput, value);
            }

            public void setUserInput(String value) {
                setInputValue(userInput, value);
            }

            public void setIpAddressInput(String value) {
                setInputValue(ipAddressInput, value);
            }
        }

    }

}
