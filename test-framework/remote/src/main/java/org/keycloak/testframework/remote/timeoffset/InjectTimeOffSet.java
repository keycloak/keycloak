package org.keycloak.testframework.remote.timeoffset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;

/**
 * Injects a {@link TimeOffSet} to change the timeoffset on the Keycloak server
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectTimeOffSet {

    LifeCycle lifecycle() default LifeCycle.METHOD;

    /**
     * Specifies whether time-offset should be integrated with underlying caches (EG. infinispan)
     *
     * @return
     */
    boolean enableForCaches() default false;

    int offset() default 0;
}
