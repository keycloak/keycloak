package org.keycloak.testsuite.arquillian.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.openqa.selenium.WebDriver;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(IgnoreBrowserDrivers.class)
public @interface IgnoreBrowserDriver {

    Class<? extends WebDriver>[] value();

    boolean negate() default false;
}
