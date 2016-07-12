package org.keycloak.testsuite.util;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

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

    public static boolean currentTitleEquals(WebDriver driver, String url) {
        try {
            (new WebDriverWait(driver, 5)).until(ExpectedConditions.titleIs(url));
        }
        catch (TimeoutException e) {
            return false;
        }
        return true;
    }
}
