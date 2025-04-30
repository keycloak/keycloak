package org.keycloak.testframework.ui.page;

import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Constructor;

public class PageSupplier  implements Supplier<AbstractPage, InjectPage> {

    @Override
    public AbstractPage getValue(InstanceContext<AbstractPage, InjectPage> instanceContext) {
        WebDriver webDriver = instanceContext.getDependency(WebDriver.class);
        return createPage(webDriver, instanceContext.getRequestedValueType());
    }

    @Override
    public boolean compatible(InstanceContext<AbstractPage, InjectPage> a, RequestedInstance<AbstractPage, InjectPage> b) {
        return true;
    }

    private <S extends AbstractPage> S createPage(WebDriver webDriver, Class<S> valueType) {
        try {
            Constructor<S> constructor = valueType.getDeclaredConstructor(WebDriver.class);
            return constructor.newInstance(webDriver);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
