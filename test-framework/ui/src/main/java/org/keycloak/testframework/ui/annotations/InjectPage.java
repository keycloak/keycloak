package org.keycloak.testframework.ui.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects an implementation of {@link org.keycloak.testframework.ui.page.AbstractPage} to interact with HTML pages
 * published by the Keycloak server
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectPage {
}
