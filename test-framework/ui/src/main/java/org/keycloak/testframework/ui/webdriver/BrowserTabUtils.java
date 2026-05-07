package org.keycloak.testframework.ui.webdriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Helper class for managing tabs in browser.
 * Tabs are indexed from 0. (f.e. first tab has index 0)
 *
 * <p>Note: For one particular WebDriver has to exist only one BrowserTabUtil instance. (Right order of tabs)</p>
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class BrowserTabUtils {

    private final ManagedWebDriver managedDriver;
    private WebDriver driver;
    private JavascriptExecutor jsExecutor;
    private List<String> tabs;

    BrowserTabUtils(ManagedWebDriver managedDriver) {
        this.managedDriver = managedDriver;
        driverValidation();
    }

    private void driverValidation() {
        this.driver = managedDriver.driver();
        this.jsExecutor = (JavascriptExecutor) driver;
        tabs = new ArrayList<>(driver.getWindowHandles());
    }


    public String getActualWindowHandle() {
        return driver.getWindowHandle();
    }

    public void switchToTab(String windowHandle) {
        driver.switchTo().window(windowHandle);
    }

    public void switchToTab(int index) {
        assertValidIndex(index);
        switchToTab(tabs.get(index));
    }

    public void newTab(String url) {
        jsExecutor.executeScript("window.open(arguments[0]);", url);

        final Set<String> handles = driver.getWindowHandles();
        final String tabHandle = handles.stream()
                .filter(tab -> !tabs.contains(tab))
                .findFirst()
                .orElse(null);

        if (handles.size() > tabs.size() + 1) {
            throw new RuntimeException("Too many window handles. You can only create a new one by this method.");
        }

        if (tabHandle == null) {
            throw new RuntimeException("Creating the new tab failed.");
        }

        tabs.add(tabHandle);
        switchToTab(tabHandle);
    }

    public void closeTab(int index) {
        assertValidIndex(index);

        if (index == 0 || getCountOfTabs() == 1)
            throw new RuntimeException("You must not close the original tab.");

        switchToTab(index);
        driver.close();

        tabs.remove(index);
        switchToTab(index - 1);
    }

    public int getCountOfTabs() {
        return tabs.size();
    }

    /**
     * Close all browser tabs with the exception of the single original tab (tab with index 0), which should be always kept opened
     */
    public void closeTabs() {
        for (int i = 1; i < getCountOfTabs(); i++) {
            closeTab(i);
        }
    }

    private boolean validIndex(int index) {
        return (index >= 0 && tabs != null && index < tabs.size());
    }

    private void assertValidIndex(int index) {
        if (!validIndex(index))
            throw new IndexOutOfBoundsException("Invalid index of tab.");
    }

}
