package org.keycloak.testframework.ui.webdriver;


import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;

import org.openqa.selenium.WebDriver;

public abstract class AbstractWebDriverSupplier implements Supplier<ManagedWebDriver, InjectWebDriver> {

    @Override
    public ManagedWebDriver getValue(InstanceContext<ManagedWebDriver, InjectWebDriver> instanceContext) {
        return new ManagedWebDriver(getWebDriver());
    }

    @Override
    public boolean compatible(InstanceContext<ManagedWebDriver, InjectWebDriver> a, RequestedInstance<ManagedWebDriver, InjectWebDriver> b) {
        return true;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public void close(InstanceContext<ManagedWebDriver, InjectWebDriver> instanceContext) {
        instanceContext.getValue().driver().quit();
    }

    public abstract WebDriver getWebDriver();

}
