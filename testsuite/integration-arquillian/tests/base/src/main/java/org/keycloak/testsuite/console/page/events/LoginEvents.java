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
public class LoginEvents extends Events {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/events";
    }

    @FindBy(tagName = "table")
    private LoginEventsTable table;

    public LoginEventsTable table() {
        return table;
    }

    public class LoginEventsTable extends DataTable {

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
        private LoginEventsTableFilterForm filterForm;

        public LoginEventsTableFilterForm filterForm() {
            return filterForm;
        }

        public class LoginEventsTableFilterForm extends Form {

            public void addEventType(String type) {
                driver.findElement(By.xpath("//div[@id='s2id_eventTypes']/ul")).click();
                driver.findElement(By.xpath("//div[@id='select2-drop']//div[text()='" + type + "']/..")).click();
            }

            public void removeOperationType(String type) {
                driver.findElement(By.xpath("//div[@id='s2id_eventTypes']//div[text()='" + type + "']/../a")).click();
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
        }
    }
}
