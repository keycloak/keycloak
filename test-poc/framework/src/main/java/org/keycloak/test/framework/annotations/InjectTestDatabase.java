package org.keycloak.test.framework.annotations;

import org.keycloak.test.framework.database.DatabaseConfig;
import org.keycloak.test.framework.injection.LifeCycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectTestDatabase {

    Class<? extends DatabaseConfig> config() default DatabaseConfig.class;

    LifeCycle lifecycle() default LifeCycle.GLOBAL;

}
