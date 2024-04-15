package org.keycloak.testsuite.webdriver;

import org.openqa.selenium.WebDriver;

public interface Browser {
    WebDriver getBrowser();

    void startBrowser();

    void stopBrowser();
}
