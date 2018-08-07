package org.keycloak.testsuite.console.page.fragment;

import io.appium.java_client.ios.IOSDriver;
import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class LocaleDropdown {
    @Root
    private WebElement root;

    @FindBy(tagName = "ul")
    private WebElement dropDownMenu;

    @FindBy(id = "kc-current-locale-link")
    private WebElement currentLocaleLink;

    @ArquillianResource
    private WebDriver driver;

    public String getSelected() {
        return getTextFromElement(currentLocaleLink);
    }

    public void selectByText(String text) {
        // open the menu
        if (driver instanceof FirefoxDriver) { // GeckoDriver hack
            Actions actions = new Actions(driver);
            actions.moveToElement(root).perform();
            pause(500);
        }
        else if (driver instanceof IOSDriver) { // TODO: Fix this! It's a very, very, ... very nasty hack for Safari on iOS - see KEYCLOAK-7947
            ((IOSDriver) driver).executeScript("arguments[0].setAttribute('style', 'display: block')", dropDownMenu);
        }
        else {
            root.click();
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
