package org.keycloak.testframework.ui.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inject a {@link org.keycloak.testframework.ui.webdriver.ManagedWebDriver} to interact directly with the web driver.
 * When possible it is recommended to use pages instead of directly accessing the web driver.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectWebDriver { }
