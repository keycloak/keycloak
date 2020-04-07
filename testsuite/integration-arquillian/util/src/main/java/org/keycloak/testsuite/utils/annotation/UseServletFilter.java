package org.keycloak.testsuite.utils.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author mhajas
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface UseServletFilter {

    String filterDependency();
    String filterName();
    String filterClass();
    String filterPattern() default "/*";
    String dispatcherType() default "";
    String skipPattern() default "";
    String idMapper() default "";
}
