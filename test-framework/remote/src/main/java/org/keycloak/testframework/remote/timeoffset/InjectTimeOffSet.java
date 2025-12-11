package org.keycloak.testframework.remote.timeoffset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectTimeOffSet {

    LifeCycle lifecycle() default LifeCycle.METHOD;

    int offset() default 0;
}
