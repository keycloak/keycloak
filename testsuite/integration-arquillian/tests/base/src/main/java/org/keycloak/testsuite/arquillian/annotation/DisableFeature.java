package org.keycloak.testsuite.arquillian.annotation;

import org.keycloak.common.Profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author mhajas
 */
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(DisableFeatures.class)
@Inherited
public @interface DisableFeature {
    Profile.Feature value();
    boolean skipRestart() default false;
    boolean onlyForProduct() default false;
}
