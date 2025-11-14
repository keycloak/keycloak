package org.keycloak.testframework.ui;

import java.util.List;
import java.util.Map;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.ui.page.PageSupplier;
import org.keycloak.testframework.ui.webdriver.ChromeHeadlessWebDriverSupplier;
import org.keycloak.testframework.ui.webdriver.ChromeWebDriverSupplier;
import org.keycloak.testframework.ui.webdriver.FirefoxHeadlessWebDriverSupplier;
import org.keycloak.testframework.ui.webdriver.FirefoxWebDriverSupplier;
import org.keycloak.testframework.ui.webdriver.HtmlUnitWebDriverSupplier;

import org.openqa.selenium.WebDriver;

public class UITestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(
                new HtmlUnitWebDriverSupplier(),
                new ChromeHeadlessWebDriverSupplier(),
                new ChromeWebDriverSupplier(),
                new FirefoxHeadlessWebDriverSupplier(),
                new FirefoxWebDriverSupplier(),
                new PageSupplier()
        );
    }

    @Override
    public Map<Class<?>, String> valueTypeAliases() {
        return Map.of(
                WebDriver.class, "browser"
        );
    }

}
