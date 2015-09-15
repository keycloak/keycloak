package org.keycloak.testsuite.console.page.events;

import org.keycloak.testsuite.console.page.fragment.DataTable;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
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

        public void update() {
            waitAjaxForBody();
            clickHeaderButton("Update");
        }

        public void reset() {
            waitAjaxForBody();
            clickHeaderButton("Reset");
        }

        @FindBy(xpath = "//button[text()[contains(.,'Filter')]]")
        private WebElement filterButton;

        public void filter() {
            waitAjaxForBody();
            filterButton.click();
        }

        @FindBy(tagName = "form")
        private AdminEventsTableFilterForm filterForm;

        public AdminEventsTableFilterForm filterForm() {
            return filterForm;
        }

        public class AdminEventsTableFilterForm extends Form {

            public void addOperationType(String type) {
                driver.findElement(By.xpath("//div[@id='s2id_adminEnabledEventOperations']/ul")).click();
                driver.findElement(By.xpath("//div[@id='select2-drop']//div[text()[contains(.,'" + type + "')]]/..")).click();
            }

            public void removeOperationType(String type) {
                driver.findElement(By.xpath("//div[@id='s2id_adminEnabledEventOperations']//div[text()='" + type + "']/../a")).click();
            }

            @FindBy(id = "resource")
            private WebElement resourcePathInput;

            public void setResourcePathInput(String value) {
                setInputValue(resourcePathInput, value);
            }

            @FindBy(id = "realm")
            private WebElement realmInput;

            public void setRealmInput(String value) {
                setInputValue(realmInput, value);
            }

            @FindBy(id = "client")
            private WebElement clientInput;

            public void setClientInput(String value) {
                setInputValue(clientInput, value);
            }

            @FindBy(id = "user")
            private WebElement userInput;

            public void setUserInput(String value) {
                setInputValue(userInput, value);
            }

            @FindBy(id = "ipAddress")
            private WebElement ipAddressInput;

            public void setIpAddressInput(String value) {
                setInputValue(ipAddressInput, value);
            }
        }

    }

}
