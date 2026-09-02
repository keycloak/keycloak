package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link org.keycloak.testframework.events.SysLogServer} that can be used to register listeners to obtain
 * logging events from Keycloak over syslog.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectSysLogServer {
}
