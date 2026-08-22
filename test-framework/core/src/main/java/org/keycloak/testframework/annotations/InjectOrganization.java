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
 * Organizations are automatically enabled on the referenced realm, and only on that realm. Realms that are attached to
 * an existing Keycloak instance through {@link InjectRealm#attachTo()} are not created by the framework and therefore
 * not configured by it, so those have to have organizations enabled already, otherwise the injection fails.
 * <p>
 * Note that enabling organizations changes the behaviour of the realm once an organization exists, most notably the
 * browser login becomes identity-first.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectOrganization {

    /**
     * Used to define a custom configuration for the organization
     */
    Class<? extends OrganizationConfig> config() default DefaultOrganizationConfig.class;

    /**
     * Controls the lifecycle of the resource.
     * <p>
     * Since the organization configures the realm it belongs to, the realm is destroyed together with the organization.
     * Using {@link LifeCycle#METHOD} therefore re-creates the realm for every test method as well.
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
