package org.keycloak.test.framework.page;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.test.framework.TestAdminClient;
import org.keycloak.test.framework.TestPage;
import org.keycloak.test.framework.TestRealm;
import org.keycloak.test.framework.injection.InstanceWrapper;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.Registry;
import org.keycloak.test.framework.injection.Supplier;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PageSupplier implements Supplier<Object, TestPage> {

    @Override
    public Class<TestPage> getAnnotationClass() {
        return TestPage.class;
    }

    @Override
    public Class<Object> getValueType() {
        return Object.class;
    }

    @Override
    public InstanceWrapper<Object, TestPage> getValue(Registry registry, TestPage annotation) {
        return null;
    }

    @Override
    public InstanceWrapper<Object, TestPage> getValue(Registry registry, InstanceWrapper<Object, TestPage> wrapper) {
        WebDriver driver = registry.getDependency(WebDriver.class, wrapper);
        Field field = wrapper.getField();
        Class<?> type = field.getType();
        try {
            // create a new instance of the page type
            Object instance = type.getDeclaredConstructor().newInstance();

            for (Field declaredField : type.getDeclaredFields()) {
                if (WebDriver.class.equals(declaredField.getType())) {
                    // look for a webdriver field in the page and set the driver instance
                    try {
                        declaredField.setAccessible(true);
                        declaredField.set(instance, driver);
                    } finally {
                        declaredField.setAccessible(false);
                    }
                }
            }

            // initialize the fields
            PageFactory.initElements(driver, instance);
            return new InstanceWrapper<>(this, wrapper.getAnnotation(), instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LifeCycle getLifeCycle() {
        return LifeCycle.CLASS;
    }

    @Override
    public boolean compatible(InstanceWrapper<Object, TestPage> a, InstanceWrapper<Object, TestPage> b) {
        return true;
    }
}
