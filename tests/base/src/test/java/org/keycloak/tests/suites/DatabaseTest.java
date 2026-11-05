package org.keycloak.tests.suites;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

import static org.keycloak.tests.suites.DatabaseTest.TAG;

/**
 * Add to JUnit tests to be run against different databases.
 * The suite is a curated subset for cross-database coverage, not all tests that use a database.
 * Include tests whose persistence behavior warrants coverage across database vendors, such as queries,
 * transactions, schema handling, or regressions caused by database-specific behavior.
 * <p>
 * Annotate a class when all its tests warrant this coverage; otherwise annotate individual test methods.
 * Database configuration is supplied separately by the test framework or CI.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Tag(TAG)
public @interface DatabaseTest {

    String TAG = "database";

}
