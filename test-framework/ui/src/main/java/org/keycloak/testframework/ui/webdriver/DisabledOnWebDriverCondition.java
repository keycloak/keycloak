package org.keycloak.testframework.ui.webdriver;

import java.lang.annotation.Annotation;

import org.keycloak.testframework.conditions.AbstractDisabledForSupplierCondition;
import org.keycloak.testframework.ui.annotations.DisabledOnWebDriver;

/**
 * JUnit 5 condition that disables a test when the active web driver matches one of the aliases
 * specified
 */
public class DisabledOnWebDriverCondition extends AbstractDisabledForSupplierCondition {

    @Override
    protected Class<?> valueType() {
        return ManagedWebDriver.class;
    }

    @Override
    protected Class<? extends Annotation> annotation() {
        return DisabledOnWebDriver.class;
    }
}
