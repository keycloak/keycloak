package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link org.keycloak.testframework.server.Logs} instance for asserting Keycloak server log output.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectLogs {

    /**
     * Zero-based index of the cluster node whose logs to capture. Only relevant for {@code ClusteredKeycloakServer}.
     */
    int node() default 0;

}
