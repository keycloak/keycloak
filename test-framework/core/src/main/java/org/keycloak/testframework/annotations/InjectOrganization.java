package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.DefaultOrganizationConfig;
import org.keycloak.testframework.realm.OrganizationConfig;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectOrganization {

    LifeCycle lifecycle() default LifeCycle.METHOD;

    String realmRef() default "";

    String attachTo() default "";

    Class<? extends OrganizationConfig> config() default DefaultOrganizationConfig.class;
}
