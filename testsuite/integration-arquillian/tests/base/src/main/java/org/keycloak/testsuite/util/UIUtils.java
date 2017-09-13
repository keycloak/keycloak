package org.keycloak.testsuite.util;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.testsuite.util.DroneUtils.getCurrentDriver;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class UIUtils {

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
     * Navigates to a link directly instead of clicking on it.
     * Some browsers are sometimes having problems with clicking on links, so this should be used only in that cases,
     * i.e. only when clicking directly doesn't work
     *
     * @param element
     */
    public static void navigateToLink(WebElement element) {
        URLUtils.navigateToUri(element.getAttribute("href"), true);
    }
}
