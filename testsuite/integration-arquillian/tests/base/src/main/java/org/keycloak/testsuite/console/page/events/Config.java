package org.keycloak.testsuite.console.page.events;

import org.keycloak.testsuite.console.page.fragment.OnOffSwitch;
import org.keycloak.testsuite.page.Form;
import static org.keycloak.testsuite.util.WaitUtils.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author tkyjovsk
 * @author mhajas
 */
public class Config extends Events {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/events-settings";
    }

    @FindBy(xpath = "//form")
    private ConfigForm form;

    public ConfigForm form() {
        return form;
    }

    public class ConfigForm extends Form {
        @FindBy(id = "s2id_autogen1")
        private WebElement eventListenersInput;

        @FindBy(xpath = "//div[@id='s2id_autogen1']/..//select")
        private Select eventListenersSelect;

        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='enabled']]")
        private OnOffSwitch SaveEvents;

        @FindBy(xpath = "//div[@id='s2id_enabledEventTypes']//input")
        private WebElement savedTypesInput;

        @FindBy(xpath = "//div[@id='select2-drop']/ul")
        private WebElement savedTypesOptions;

        @FindBy(id = "expiration")
        private WebElement expirationInput;

        @FindBy(name = "expirationUnit")
        private Select expirationUnitSelect;

        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='adminEventsEnabled']]")
        private OnOffSwitch saveAdminEvents;

        @FindBy(xpath = ".//div[@class='onoffswitch' and ./input[@id='adminEventsDetailsEnabled']]")
        private OnOffSwitch includeRepresentation;

        @FindBy(xpath = "//button[@data-ng-click='clearEvents()']")
        private WebElement clearLoginEventsButton;

        @FindBy(xpath = "//button[@data-ng-click='clearAdminEvents()']")
        private WebElement clearAdminEventsButton;

        public void addEventListener(String listener) {
            eventListenersInput.click();
            eventListenersSelect.selectByVisibleText(listener);
        }

        public void removeEventListener(String listener) {
            eventListenersInput.findElement(By.xpath("//div[text()='" + listener + "']/../a")).click();
        }

        public void setSaveEvents(boolean value) {
            SaveEvents.setOn(value);
        }

        public void addSaveType(String type) {
            savedTypesInput.click();
            savedTypesOptions.findElement(By.xpath("//div[text()='" + type + "']")).click();
        }

        public void removeSaveType(String type) {
            savedTypesInput.findElement(By.xpath("//div[text()='" + type + "']/../a")).click();
        }

        public void clearLoginEvents() {
            clearLoginEventsButton.click();
        }

        public void setExpiration(String value, String unit) {
            expirationUnitSelect.selectByVisibleText(unit);
            Form.setInputValue(expirationInput, value);
        }

        public void setSaveAdminEvents(boolean value) {
            saveAdminEvents.setOn(value);
        }

        public void setIncludeRepresentation(boolean value) {
            includeRepresentation.setOn(value);
        }

        public void clearAdminEvents() {
            clearAdminEventsButton.click();
        }
        
        public void waitForClearEventsButtonPresent() {
            waitUntilElement(clearLoginEventsButton).is().present();
        }
    }
}
