package org.keycloak.test.framework.page;

import org.keycloak.test.framework.injection.InstanceWrapper;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.Registry;
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

    public InstanceWrapper<AbstractPage, TestPage> getValue(Registry registry, TestPage annotation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstanceWrapper<AbstractPage, TestPage> getValue(Registry registry, TestPage annotation, Class<? extends AbstractPage> valueType) {
        InstanceWrapper<AbstractPage, TestPage> instanceWrapper = new InstanceWrapper<>(this, annotation);
        WebDriver webDriver = registry.getDependency(WebDriver.class, instanceWrapper);
        AbstractPage page = createPage(webDriver, valueType);
        instanceWrapper.setValue(page, LifeCycle.CLASS);
        return instanceWrapper;
    }

    @Override
    public boolean compatible(InstanceWrapper<AbstractPage, TestPage> a, RequestedInstance<AbstractPage, TestPage> b) {
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
