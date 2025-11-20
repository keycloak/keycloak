package org.keycloak.testframework.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectAdminClientFactory {

    String ref() default "";

    LifeCycle lifecycle() default LifeCycle.CLASS;
}
