package org.keycloak.testframework.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link org.keycloak.admin.client.Keycloak} instance to access Keycloak Admin APIs
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectAdminClient {

    /**
     * A ref must be set if a test requires multiple instances
     */
    String ref() default "";

    /**
     * Set to attach to the non-default realm
     */
    String realmRef() default "";

    /**
     * <code>BOOTSTRAP</code> attaches to the master realm and global test client, while <code>MANAGED_REALM</code>
     * attaches to a managed realm using the specified client or user. When using <code>MANAGED_REALM</code> either
     * client or user has to be set
     */
    Mode mode() default Mode.BOOTSTRAP;

    /**
     * The client to authenticate as
     */
    String client() default "";

    /**
     * The user to authenticate as
     */
    String user() default "";

    enum Mode {

        BOOTSTRAP,
        MANAGED_REALM

    }

}
