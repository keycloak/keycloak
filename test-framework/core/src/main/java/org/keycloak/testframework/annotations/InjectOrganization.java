package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.DefaultOrganizationConfig;
import org.keycloak.testframework.realm.ManagedOrganization;
import org.keycloak.testframework.realm.OrganizationConfig;

/**
 * Injects a {@link ManagedOrganization} used to create an organization within the realm.
 * <p>
 * The realm the organization is created in must have organizations enabled, otherwise the injection fails. Configure
 * the realm with a {@link org.keycloak.testframework.realm.RealmConfig} calling
 * {@link org.keycloak.testframework.realm.RealmBuilder#organizationsEnabled(boolean)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectOrganization {

    /**
     * Used to define a custom configuration for the organization
     */
    Class<? extends OrganizationConfig> config() default DefaultOrganizationConfig.class;

    /**
     * Controls the lifecycle of the resource
     */
    LifeCycle lifecycle() default LifeCycle.CLASS;

    /**
     * A ref must be set if a test requires multiple instances
     */
    String ref() default "";

    /**
     * Set to attach to the non-default realm
     */
    String realmRef() default "";
}
