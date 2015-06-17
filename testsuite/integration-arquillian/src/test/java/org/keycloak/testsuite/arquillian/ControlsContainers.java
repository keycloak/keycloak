package org.keycloak.testsuite.arquillian;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 * @author tkyjovsk
 */
@Retention(RUNTIME)
public @interface ControlsContainers 
{
   String[] value();
}