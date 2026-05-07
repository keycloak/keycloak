package org.keycloak.it.junit5.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skips realm bootstrap (DB lock, realm creation, admin user setup) after provider initialization.
 * Liquibase schema migration and provider {@code postInit()} still run.
 * Useful for tests that only assert on log messages from server startup and don't need realm data.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipRealmBootstrap {

}
