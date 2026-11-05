package org.keycloak.testframework.ui.webdriver;

import java.util.Arrays;

import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;

import org.openqa.selenium.WebDriver;

public abstract class AbstractWebDriverSupplier implements Supplier<ManagedWebDriver, InjectWebDriver> {

    @Override
    public ManagedWebDriver getValue(InstanceContext<ManagedWebDriver, InjectWebDriver> instanceContext) {
        return new ManagedWebDriver(getWebDriver(instanceContext.getAnnotation()));
    }

    @Override
    public boolean compatible(InstanceContext<ManagedWebDriver, InjectWebDriver> a, RequestedInstance<ManagedWebDriver, InjectWebDriver> b) {
        return a.getAnnotation().ref().equals(b.getAnnotation().ref())
                && Arrays.equals(a.getAnnotation().chromeArguments(), b.getAnnotation().chromeArguments());
    }

    @Override
    public void close(InstanceContext<ManagedWebDriver, InjectWebDriver> instanceContext) {
        instanceContext.getValue().driver().quit();
    }

    /** Creates the driver; browsers that support per-test arguments override this. */
    public WebDriver getWebDriver(InjectWebDriver annotation) {
        return getWebDriver();
    }

    public abstract WebDriver getWebDriver();

}
