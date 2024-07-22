package org.keycloak.test.framework.page;

import org.keycloak.test.framework.annotations.TestPage;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Constructor;

public class PageSupplier  implements Supplier<AbstractPage, TestPage> {

    @Override
    public Class<TestPage> getAnnotationClass() {
        return TestPage.class;
    }

    @Override
    public Class<AbstractPage> getValueType() {
        return AbstractPage.class;
    }

    @Override
    public AbstractPage getValue(InstanceContext<AbstractPage, TestPage> instanceContext) {
        WebDriver webDriver = instanceContext.getDependency(WebDriver.class);
        return createPage(webDriver, instanceContext.getRequestedValueType());
    }

    @Override
    public boolean compatible(InstanceContext<AbstractPage, TestPage> a, RequestedInstance<AbstractPage, TestPage> b) {
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
