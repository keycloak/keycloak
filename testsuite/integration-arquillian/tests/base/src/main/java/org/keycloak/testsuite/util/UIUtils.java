package org.keycloak.testsuite.util;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import org.keycloak.testsuite.page.AbstractPatternFlyAlert;
import org.keycloak.testsuite.pages.AbstractPage;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;
import static org.keycloak.testsuite.util.WaitUtils.log;
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
            (new WebDriverWait(getCurrentDriver(), Duration.ofSeconds(5))).until(ExpectedConditions.titleIs(url));
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

    /**
     * The method executes click or sendKeys(Keys.ENTER) in the element.
     * In the chrome driver click is emulated by pressing the ENTER key. Since
     * the upgrade to chrome 128 some clicks are missed and that triggers CI
     * failures. The method is intended to be used for buttons and links which
     * accept clicking by pressing the ENTER key. If the element passed does
     * not allow clicking using keys use the {@link #click(WebElement) click}
     * method.
     *
     * @param element The element to click
     */
    public static void clickLink(WebElement element) {
        clickElement(element, Keys.ENTER);
    }

    /**
     * Same than clickLink but it does not perform any wait after clicking or sending the
     * enter key.
     *
     * @param element The element to click
     */
    public static void clickLinkWithoutWait(WebElement element) {
        clickLinkWithoutWait(element, Keys.ENTER);
    }

    private static void clickElement(WebElement element, CharSequence key) {
        WebDriver driver = getCurrentDriver();

        waitUntilElement(element).is().clickable();

        performOperationWithPageReload(BrowserDriverUtil.isDriverChrome(driver)
                ? () -> element.sendKeys(key)
                : element::click);
    }

    private static void clickLinkWithoutWait(WebElement element, CharSequence key) {
        WebDriver driver = getCurrentDriver();

        waitUntilElement(element).is().clickable();

        if (BrowserDriverUtil.isDriverChrome(driver)) {
            element.sendKeys(key);
        } else {
            element.click();
        }
    }

    /**
     * The method executes click in the element. This method always uses click and
     * is not emulated by key pressing in chrome.
     *
     * @param element The element to click
     */
    public static void click(WebElement element) {
        waitUntilElement(element).is().clickable();
        performOperationWithPageReload(element::click);
    }

    /**
     * The method switches the checkbox to the expected state.
     *
     * It looks that since chrome 128, the single click sometimes does
     * not work (See also similar issue {@link #clickLink(WebElement)}, so it is possible repeated multiple times until it reach
     * the desired state
     *
     * @param checkbox Checkbox element to enable or disable
     * @param enable If true, the checkbox should be switched to enabled (checked). If false, the checkbox should be switched to disabled (unchecked)
     */
    public static void switchCheckbox(WebElement checkbox, boolean enable) {
        boolean current = checkbox.isSelected();
        if (current != enable) {
            clickElement(checkbox, Keys.SPACE);
            Assert.assertNotEquals("Checkbox " + checkbox + " is still in the state " + current + " after click.", current, checkbox.isSelected());
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
    }

    /**
     * Contains some browser-specific tweaks for getting an element text.
     *
     * @param element
     * @return
     */
    public static String getTextFromElement(WebElement element) {
        String text = element.getText();
        return text;
    }

    /**
     * Get text from required WebElement
     *
     * @param element required WebElement
     * @return text from element, or null if element is not found
     */
    public static String getTextFromElementOrNull(Supplier<WebElement> element) {
        return Optional.ofNullable(getElementOrNull(element))
                .map(UIUtils::getTextFromElement)
                .orElse(null);
    }

    /**
     * Get required WebElement
     *
     * @param element required WebElement
     * @return element, or null if element is not found
     */
    public static WebElement getElementOrNull(Supplier<WebElement> element) {
        try {
            return element.get();
        } catch (NoSuchElementException e) {
            log.debug("Particular element is not found.", e);
            return null;
        }
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

    public static String getRawPageSource(WebDriver driver) {
        if ((driver instanceof FirefoxDriver) || (driver instanceof ChromeDriver)) {
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

    /**
     * Navigates the driver back but it refreshes the page if it is not the expected one for
     * chrome. Chrome 136 does not respect cache-control and refresh is needed
     * to reach the server again (the page is cached no matter the cache-control
     * directive returned).
     * See https://issues.chromium.org/issues/415773538
     *
     * @param driver The driver used
     * @param expectedPage The expected page
     */
    public static void navigateBackWithRefresh(WebDriver driver, AbstractPage expectedPage) {
        driver.navigate().back();
        if (!expectedPage.isCurrent() && BrowserDriverUtil.isDriverChrome(driver)) {
            driver.navigate().refresh();
        }
        expectedPage.assertCurrent();
    }
}
