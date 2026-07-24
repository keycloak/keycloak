package org.keycloak.tests.suites;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

import static org.keycloak.tests.suites.DatabaseTest.TAG;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Tag(TAG)
public @interface DatabaseTest {

    String TAG = "database";

}
