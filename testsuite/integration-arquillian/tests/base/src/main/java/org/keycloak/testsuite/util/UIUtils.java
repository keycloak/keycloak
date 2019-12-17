package org.keycloak.testsuite.util;

import io.appium.java_client.android.AndroidDriver;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.testsuite.page.AbstractPatternFlyAlert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElementClassContains;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class UIUtils {

    public static final String VALUE_ATTR_NAME = "value";
    public static final String ARIA_INVALID_ATTR_NAME = "aria-invalid";

    public static boolean selectContainsOption(Select select, String optionText) {
        for (WebElement option : select.getOptions()) {
            if (option.getText().trim().equals(optionText)) {
                return true;
            }
        }
        return false;
    }

    public static boolean currentTitleEquals(String url) {
        try {
            (new WebDriverWait(getCurrentDriver(), 5)).until(ExpectedConditions.titleIs(url));
        }
        catch (TimeoutException e) {
            return false;
        }
        return true;
    }

    /**
     * Safely performs an operation which is expected to cause a page reload, e.g. a link click.
     * This ensures the page will be fully loaded after the operation.
     * This is intended for use in UI tests only.
     *
     * @param operation
     */
    public static void performOperationWithPageReload(Runnable operation) {
        operation.run();
        waitForPageToLoad();
    }

    public static void refreshPageAndWaitForLoad() {
        performOperationWithPageReload(() -> getCurrentDriver().navigate().refresh());
    }

    public static void clickLink(WebElement element) {
        WebDriver driver = getCurrentDriver();

        // Sometimes at some weird specific conditions, Firefox fail to click an element
        // because the element is at the edge of the view and need to be scrolled on "manually" (normally the driver
        // should do this automatically)
        if (driver instanceof FirefoxDriver) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        }

        if (driver instanceof SafariDriver && !element.isDisplayed()) { // Safari sometimes thinks an element is not visible
                                                                        // even though it is. In this case we just move the cursor and click.
            performOperationWithPageReload(() -> new Actions(driver).click(element).perform());
        }
        else {
            performOperationWithPageReload(element::click);
        }
    }

    /**
     * This is as an alternative for {@link #clickLink(WebElement)} and should be used in situations where we can't use
     * {@link WaitUtils#waitForPageToLoad()}. This is because {@link WaitUtils#waitForPageToLoad()} would wait until the
     * alert would disappeared itself (timeout).
     *
     * @param button to click on
     */
    public static void clickBtnAndWaitForAlert(WebElement button) {
        button.click();
        AbstractPatternFlyAlert.waitUntilDisplayed();
    }

    /**
     * Navigates to a link directly instead of clicking on it.
     * Some browsers are sometimes having problems with clicking on links, so this should be used only in that cases,
     * i.e. only when clicking directly doesn't work
     *
     * @param element
     */
    public static void navigateToLink(WebElement element) {
        URLUtils.navigateToUri(element.getAttribute("href"));
    }

    /**
     * This is meant mainly for file uploads in Admin Console where the input fields are hidden
     *
     * @param element
     * @param keys
     */
    public static void sendKeysToInvisibleElement(WebElement element, CharSequence... keys) {
        if (element.isDisplayed()) {
            element.sendKeys(keys);
            return;
        }

        JavascriptExecutor jsExecutor = (JavascriptExecutor) getCurrentDriver();
        String styleBckp = element.getAttribute("style");

        jsExecutor.executeScript("arguments[0].setAttribute('style', 'display:block !important');", element);
        waitUntilElement(element).is().visible();
        element.sendKeys(keys);
        jsExecutor.executeScript("arguments[0].setAttribute('style', '" + styleBckp + "');", element);
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
            }
            catch (NoSuchElementException e) {
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

    /**
     * To be used only when absolutely necessary. Browsers should handle scrolling automatically but at some unknown
     * conditions some of them (GeckoDriver) won't scroll.
     *
     * @param element
     */
    public static void scrollElementIntoView(WebElement element) {
        ((JavascriptExecutor) getCurrentDriver()).executeScript("arguments[0].scrollIntoView(true);", element);
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
}
