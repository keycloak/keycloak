package org.keycloak.operator.testsuite.apiserver;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface DisabledIfApiServerTest {

}
