package org.keycloak.testframework.ui.webdriver;

import org.keycloak.testframework.ui.page.AbstractPage;

public class NavigateUtils {

    private final ManagedWebDriver driver;

    NavigateUtils(ManagedWebDriver driver) {
        this.driver = driver;
    }

    public void refresh() {
        driver.driver().navigate().refresh();
    }

    public void backWithRefresh(AbstractPage expectedPage) {
        driver.driver().navigate().back();

        String currentPageId = driver.page().getCurrentPageId();
        if (!expectedPage.getExpectedPageId().equals(currentPageId) && driver.getBrowserType().equals(BrowserType.CHROME)) {
            driver.driver().navigate().refresh();
        }

        expectedPage.assertCurrent();
    }

}
