package org.keycloak.testframework.remote.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

@Test
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestOnServer {
}
