package org.keycloak.testframework.ui.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.ui.webdriver.DisabledOnWebDriverCondition;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests annotated with {@code @DisabledOnWebDriver} will be skipped when running against any of the specified web drivers.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DisabledOnWebDriverCondition.class)
public @interface DisabledOnWebDriver {

    /**
     * One or more web-driver supplier aliases for which the test should be disabled.
     */
    String[] value() default {};
}
