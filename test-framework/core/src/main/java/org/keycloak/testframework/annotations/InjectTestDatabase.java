package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.database.DatabaseConfig;
import org.keycloak.testframework.database.DefaultDatabaseConfig;
import org.keycloak.testframework.injection.LifeCycle;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectTestDatabase {

    LifeCycle lifecycle() default LifeCycle.GLOBAL;

    Class<? extends DatabaseConfig> config() default DefaultDatabaseConfig.class;
}
