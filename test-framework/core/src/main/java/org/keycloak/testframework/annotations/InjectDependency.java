package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects dependencies into configuration classes; for example if a {@link org.keycloak.testframework.realm.ClientConfig}
 * needs to access the {@link org.keycloak.testframework.realm.ManagedRealm} to set the correct configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectDependency {

}
