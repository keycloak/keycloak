package org.keycloak.test.framework.ui;

import org.keycloak.test.framework.TestFrameworkExtension;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.ui.page.PageSupplier;
import org.keycloak.test.framework.ui.webdriver.ChromeHeadlessWebDriverSupplier;
import org.keycloak.test.framework.ui.webdriver.ChromeWebDriverSupplier;
import org.keycloak.test.framework.ui.webdriver.FirefoxHeadlessWebDriverSupplier;
import org.keycloak.test.framework.ui.webdriver.FirefoxWebDriverSupplier;
import org.keycloak.test.framework.ui.webdriver.HtmlUnitWebDriverSupplier;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;

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
