package org.keycloak.testsuite.console.page.fragment;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class LocaleDropdown {
    protected Logger log = Logger.getLogger(this.getClass());

    @Root
    private WebElement root;

    @FindBy(tagName = "ul")
    private WebElement dropDownMenu;

    @FindBy(id = "kc-current-locale-link")
    private WebElement currentLocaleLink;

    @Drone
    private WebDriver driver;

    public String getSelected() {
        return getTextFromElement(currentLocaleLink);
    }

    public void selectByText(String text) {
        // open the menu
        if (driver instanceof IOSDriver) { // TODO: Fix this! It's a very, very, ... very nasty hack for Safari on iOS - see KEYCLOAK-7947
            ((IOSDriver) driver).executeScript("arguments[0].setAttribute('style', 'display: block')", dropDownMenu);
        }
        else if (driver instanceof AndroidDriver || driver instanceof InternetExplorerDriver) { // Android needs to tap (no cursor)
                                                                                                // and IE has some bug so needs to click as well (instead of moving cursor)
            currentLocaleLink.click();
        }
        else {
            Actions actions = new Actions(driver);
            log.info("Moving mouse cursor to the localization menu");
            actions.moveToElement(currentLocaleLink).perform();
        }

        // click desired locale
        clickLink(dropDownMenu.findElement(By.xpath("./li/a[text()='" + text + "']")));
    }

    public void selectAndAssert(String text) {
        assertNotEquals(text, getSelected());
        selectByText(text);
        assertEquals(text, getSelected());
    }
}
