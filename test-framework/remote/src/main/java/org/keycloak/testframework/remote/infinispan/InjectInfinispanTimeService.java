package org.keycloak.testframework.remote.infinispan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;

/**
 * Injects a {@link InfinispanTimeService} to change the infinispan runtime environment to use Keycloak {@link org.keycloak.common.util.Time} utility
 *
 * This is often needed with together with {@link TimeOffSet} in the tests, which need to update time on Keycloak server
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectInfinispanTimeService {

    LifeCycle lifecycle() default LifeCycle.METHOD;
}
