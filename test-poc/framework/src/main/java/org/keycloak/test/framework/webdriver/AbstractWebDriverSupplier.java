package org.keycloak.test.framework.webdriver;

import org.keycloak.test.framework.annotations.InjectWebDriver;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.AbstractDriverOptions;

import java.time.Duration;

public abstract class AbstractWebDriverSupplier implements Supplier<WebDriver, InjectWebDriver> {

    @Override
    public Class<InjectWebDriver> getAnnotationClass() {
        return InjectWebDriver.class;
    }

    @Override
    public Class<WebDriver> getValueType() {
        return WebDriver.class;
    }

    @Override
    public WebDriver getValue(InstanceContext<WebDriver, InjectWebDriver> instanceContext) {
        return getWebDriver();
    }

    @Override
    public boolean compatible(InstanceContext<WebDriver, InjectWebDriver> a, RequestedInstance<WebDriver, InjectWebDriver> b) {
        return true;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public void close(InstanceContext<WebDriver, InjectWebDriver> instanceContext) {
        instanceContext.getValue().quit();
    }

    public abstract WebDriver getWebDriver();

    public void setGlobalOptions(AbstractDriverOptions<?> options) {
        options.setImplicitWaitTimeout(Duration.ofSeconds(5));
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
    }

}
