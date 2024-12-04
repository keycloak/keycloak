package org.keycloak.test.framework.remote.timeoffset;

import org.keycloak.test.framework.injection.LifeCycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectTimeOffSet {

    LifeCycle lifecycle() default LifeCycle.METHOD;

    int offset() default 0;
}
