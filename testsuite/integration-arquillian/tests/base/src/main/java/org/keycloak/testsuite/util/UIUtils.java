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
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class UIUtils {

    public static final String VALUE_ATTR_NAME = "value";
    public static final short EXPECTED_UI_LAYOUT = Short.parseShort(System.getProperty("testsuite.ui.layout")); // 0 == desktop layout, 1 == smartphone layout, 2 == tablet layout

    public static boolean selectContainsOption(Select select, String optionText) {
        for (WebElement option : select.getOptions()) {
            if (option.getText().equals(optionText)) {
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

    public static void clickLink(WebElement element) {
        performOperationWithPageReload(element::click);
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
            input.sendKeys("a");
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
}
