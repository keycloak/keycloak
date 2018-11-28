package org.keycloak.adapters.springsecurity;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.keycloak.adapters.springsecurity.management.HttpSessionManager;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.context.annotation.FilterType;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Add this annotation to a class that extends {@code KeycloakWebSecurityConfigurerAdapter} to provide
 * a keycloak based Spring security configuration.
 *
 * @author Hendrik Ebbers
 */
@Retention(value = RUNTIME)
@Target(value = { TYPE })
@Configuration
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class, excludeFilters={
  @ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE, value=HttpSessionManager.class)})
@EnableWebSecurity
public @interface KeycloakConfiguration {
}
