package org.keycloak.testsuite.util;

import io.appium.java_client.android.AndroidDriver;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.safari.SafariDriver;

import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElementClassContains;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class UIUtils {

    public static final String VALUE_ATTR_NAME = "value";
    public static final String ARIA_INVALID_ATTR_NAME = "aria-invalid";


    /**
     * Safely performs an operation which is expected to cause a page reload, e.g. a link click.
     * This ensures the page will be fully loaded after the operation.
     * This is intended for use in UI tests only.
     *
     * @param operation
     */
    public static void performOperationWithPageReload(Runnable operation) {
        operation.run();
    }

    public static void clickLink(WebElement element) {
        WebDriver driver = getCurrentDriver();

        waitUntilElement(element).is().clickable();

        if (driver instanceof SafariDriver && !element.isDisplayed()) { // Safari sometimes thinks an element is not visible
                                                                        // even though it is. In this case we just move the cursor and click.
            performOperationWithPageReload(() -> new Actions(driver).click(element).perform());
        }
        else {
            performOperationWithPageReload(element::click);
        }
    }

    public static String getTextInputValue(WebElement input) {
        return input.getAttribute(VALUE_ATTR_NAME);
    }

    public static void setTextInputValue(WebElement input, String value) {
        input.click();
        input.clear();
        if (!StringUtils.isEmpty(value)) { // setting new input
            input.sendKeys(value);
        }
        else { // just clearing the input; input.clear() may not fire all JS events so we need to let the page know that something's changed
            input.sendKeys("1");
            input.sendKeys(Keys.BACK_SPACE);
        }

        WebDriver driver = getCurrentDriver();
        if (driver instanceof AndroidDriver) {
            AndroidDriver androidDriver = (AndroidDriver) driver;
            androidDriver.hideKeyboard(); // stability improvement
        }
    }

    /**
     * Contains some browser-specific tweaks for getting an element text.
     *
     * @param element
     * @return
     */
    public static String getTextFromElement(WebElement element) {
        String text = element.getText();
        if (getCurrentDriver() instanceof SafariDriver) {
            try {
                // Safari on macOS doesn't comply with WebDriver specs yet again - getText() retrieves hidden text by CSS.
                text = element.findElement(By.xpath("./span[not(contains(@class,'ng-hide'))]")).getText();
            } catch (NoSuchElementException e) {
                // no op
            }
            return text.trim(); // Safari on macOS sometimes for no obvious reason surrounds the text with spaces
        }
        return text;
    }

    /**
     * Should be used solely with {@link org.jboss.arquillian.graphene.GrapheneElement}, i.e. all elements annotated by
     * {@link org.openqa.selenium.support.FindBy}. CANNOT be used with elements found directly using
     * {@link WebDriver#findElement(By)} and similar.
     *
     * @param element
     * @return true if element is present and visible
     */
    public static boolean isElementVisible(WebElement element) {
        try {
            return element.isDisplayed();
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    public static boolean doesElementClassContain(WebElement element, String value) {
        try {
            waitUntilElementClassContains(element, value);
        }
        catch (TimeoutException e) {
            return false;
        }
        return true;
    }

    public static boolean isElementDisabled(WebElement element) {
        return element.getAttribute("disabled") != null;
    }

    /**
     * Relies on aria-invalid attribute.
     *
     * @param element an input element
     * @return true iff the element contains "aria-invalid" attribute AND its value is not set to "false",
     *         false otherwise
     */
    public static boolean isInputElementValid(WebElement element) {
        String ariaInvalid = element.getAttribute(ARIA_INVALID_ATTR_NAME);
        return !Boolean.parseBoolean(ariaInvalid);
    }

    public static String getRawPageSource(WebDriver driver) {
        if (driver instanceof FirefoxDriver) {
            // firefox has some weird "bug" – it wraps xml in html
            return driver.findElement(By.tagName("body")).getText();
        }
        else {
            return driver.getPageSource();
        }
    }

    public static String getRawPageSource() {
        return getRawPageSource(getCurrentDriver());
    }
}
