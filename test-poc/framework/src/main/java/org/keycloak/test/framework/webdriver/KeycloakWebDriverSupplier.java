package org.keycloak.test.framework.webdriver;

import org.keycloak.test.framework.injection.InstanceWrapper;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.Registry;
import org.keycloak.test.framework.injection.Supplier;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class KeycloakWebDriverSupplier implements Supplier<WebDriver, KeycloakWebDriver> {
    @Override
    public Class<KeycloakWebDriver> getAnnotationClass() { return KeycloakWebDriver.class; }

    @Override
    public Class<WebDriver> getValueType() { return WebDriver.class; }

    @Override
    public InstanceWrapper<WebDriver, KeycloakWebDriver> getValue(Registry registry, KeycloakWebDriver annotation) {
        final var wrapper = new InstanceWrapper<>(this, annotation);
        final var driver = new FirefoxDriver();

        wrapper.setValue(driver);

        return wrapper;
    }

    @Override
    public LifeCycle getLifeCycle() { return LifeCycle.CLASS; }

    @Override
    public boolean compatible(InstanceWrapper<WebDriver, KeycloakWebDriver> a, InstanceWrapper<WebDriver, KeycloakWebDriver> b) {
        return true;
    }

    @Override
    public void close(WebDriver instance) {
        instance.close();
    }
}
