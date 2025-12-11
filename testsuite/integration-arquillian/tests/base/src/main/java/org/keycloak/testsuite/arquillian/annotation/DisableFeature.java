package org.keycloak.testsuite.arquillian.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.keycloak.common.Profile;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author mhajas
 */
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(DisableFeatures.class)
public @interface DisableFeature {

    /**
     * Feature, which should be disabled.
     */
    Profile.Feature value();

    /**
     * The feature will be disabled without restarting of a server.
     */
    boolean skipRestart() default false;

    /**
     * Feature disable should be the last action in @Before context.
     * If the test halted, the feature is returned to the previous state.
     * If it's false, feature will be disabled before @Before method.
     */
    boolean executeAsLast() default true;
}
