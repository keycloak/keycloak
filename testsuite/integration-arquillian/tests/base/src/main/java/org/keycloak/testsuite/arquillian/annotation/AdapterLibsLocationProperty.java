package org.keycloak.testsuite.arquillian.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 *
 * @author tkyjovsk
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE})
public @interface AdapterLibsLocationProperty 
{
   String value();
}