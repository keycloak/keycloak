package org.keycloak.testframework.mail.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link org.keycloak.testframework.mail.MailServer} to receive emails sent by the Keycloak server
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectMailServer { }
