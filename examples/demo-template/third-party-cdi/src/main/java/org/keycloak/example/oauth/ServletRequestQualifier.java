package org.keycloak.example.oauth;

import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is needed to have same code working in AS7 and Wildfly. In Wildfly is HttpServletRequest injected automatically, in AS7 it's not
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
public @interface ServletRequestQualifier {
}
