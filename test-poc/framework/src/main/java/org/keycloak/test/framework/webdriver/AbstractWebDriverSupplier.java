package org.keycloak.test.framework.webdriver;

import org.keycloak.test.framework.annotations.InjectWebDriver;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;

import java.time.Duration;
import java.util.Map;

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

    public void setCommonCapabilities(MutableCapabilities capabilities) {
        capabilities.setCapability(CapabilityType.PAGE_LOAD_STRATEGY, PageLoadStrategy.NORMAL.toString());
        capabilities.setCapability("timeouts", Map.of("implicit", Duration.ofSeconds(5).toMillis()));
    }

}
