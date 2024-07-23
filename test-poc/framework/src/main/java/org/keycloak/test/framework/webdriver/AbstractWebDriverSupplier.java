package org.keycloak.test.framework.webdriver;

import org.keycloak.test.framework.annotations.WebDriver;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.remote.AbstractDriverOptions;

import java.time.Duration;

public abstract class AbstractWebDriverSupplier implements Supplier<org.openqa.selenium.WebDriver, WebDriver> {

    @Override
    public Class<WebDriver> getAnnotationClass() {
        return WebDriver.class;
    }

    @Override
    public Class<org.openqa.selenium.WebDriver> getValueType() {
        return org.openqa.selenium.WebDriver.class;
    }

    @Override
    public org.openqa.selenium.WebDriver getValue(InstanceContext<org.openqa.selenium.WebDriver, WebDriver> instanceContext) {
        return getWebDriver();
    }

    @Override
    public boolean compatible(InstanceContext<org.openqa.selenium.WebDriver, WebDriver> a, RequestedInstance<org.openqa.selenium.WebDriver, WebDriver> b) {
        return true;
    }

    @Override
    public LifeCycle getLifeCycle(WebDriver annotation) {
        return LifeCycle.GLOBAL;
    }

    @Override
    public void close(InstanceContext<org.openqa.selenium.WebDriver, WebDriver> instanceContext) {
        instanceContext.getValue().quit();
    }

    public abstract org.openqa.selenium.WebDriver getWebDriver();

    public void setGlobalOptions(AbstractDriverOptions<?> options) {
        options.setImplicitWaitTimeout(Duration.ofSeconds(5));
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
    }

}
