package org.keycloak.tests.admin.client.v2.validation;

import org.keycloak.representations.admin.v2.validators.ValidWebOriginValidator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ValidWebOriginValidator} covering web origin format validation
 * per RFC 6454 (scheme://host[:port]) plus special values {@code *} and {@code +}.
 */
class WebOriginValidationTest {

    private final ValidWebOriginValidator validator = new ValidWebOriginValidator();

    @Nested
    @DisplayName("Special values")
    class SpecialValues {

        @Test
        @DisplayName("accepts wildcard - *")
        void acceptsWildcard() {
            assertTrue(validator.isValid("*", null));
        }

        @Test
        @DisplayName("accepts plus (derive from redirects) - +")
        void acceptsPlus() {
            assertTrue(validator.isValid("+", null));
        }
    }

    @Nested
    @DisplayName("Valid origins")
    class ValidOrigins {

        @ParameterizedTest
        @ValueSource(strings = {
                "https://example.com",
                "http://example.com",
                "https://example.com:8443",
                "http://localhost:3000",
                "http://127.0.0.1:8080",
                "https://sub.domain.example.com",
                "http://my-server.example.com",
                "http://[::1]",
                "http://[::1]:8080",
                "http://[2001:db8::1]:443",
                "https://example.com:443",
                "myapp://auth"
        })
        @DisplayName("accepts valid scheme://host[:port] origins")
        void acceptsValidOrigins(String origin) {
            assertTrue(validator.isValid(origin, null));
        }
    }

    @Nested
    @DisplayName("Invalid origins")
    class InvalidOrigins {

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertFalse(validator.isValid(null, null));
        }

        @Test
        @DisplayName("rejects plain string without scheme - not-an-origin")
        void rejectsPlainString() {
            assertFalse(validator.isValid("not-an-origin", null));
        }

        @Test
        @DisplayName("rejects origin with path - https://example.com/path")
        void rejectsOriginWithPath() {
            assertFalse(validator.isValid("https://example.com/path", null));
        }

        @Test
        @DisplayName("rejects origin with trailing slash - https://example.com/")
        void rejectsOriginWithTrailingSlash() {
            assertFalse(validator.isValid("https://example.com/", null));
        }

        @Test
        @DisplayName("rejects origin with query - https://example.com?foo=bar")
        void rejectsOriginWithQuery() {
            assertFalse(validator.isValid("https://example.com?foo=bar", null));
        }

        @Test
        @DisplayName("rejects origin with fragment - https://example.com#section")
        void rejectsOriginWithFragment() {
            assertFalse(validator.isValid("https://example.com#section", null));
        }

        @Test
        @DisplayName("rejects empty string")
        void rejectsEmptyString() {
            assertFalse(validator.isValid("", null));
        }

        @Test
        @DisplayName("rejects blank string")
        void rejectsBlankString() {
            assertFalse(validator.isValid("   ", null));
        }

        @Test
        @DisplayName("rejects scheme only - https://")
        void rejectsSchemeOnly() {
            assertFalse(validator.isValid("https://", null));
        }

        @Test
        @DisplayName("rejects missing scheme - example.com")
        void rejectsMissingScheme() {
            assertFalse(validator.isValid("example.com", null));
        }

        @Test
        @DisplayName("rejects scheme starting with digit - 1http://example.com")
        void rejectsSchemeStartingWithDigit() {
            assertFalse(validator.isValid("1http://example.com", null));
        }

        @Test
        @DisplayName("rejects origin with path and wildcard - https://example.com/*")
        void rejectsOriginWithPathAndWildcard() {
            assertFalse(validator.isValid("https://example.com/*", null));
        }

        @Test
        @DisplayName("rejects origin with userinfo - https://user:pass@example.com")
        void rejectsOriginWithUserinfo() {
            assertFalse(validator.isValid("https://user:pass@example.com", null));
        }

        @Test
        @DisplayName("rejects origin with matrix parameter - https://example.com;param=value")
        void rejectsOriginWithMatrixParam() {
            assertFalse(validator.isValid("https://example.com;param=value", null));
        }

        @Test
        @DisplayName("rejects brackets mid-hostname - http://a[b]c")
        void rejectsBracketsMidHostname() {
            assertFalse(validator.isValid("http://a[b]c", null));
        }

        @Test
        @DisplayName("rejects space in hostname - http://exam ple.com")
        void rejectsSpaceInHostname() {
            assertFalse(validator.isValid("http://exam ple.com", null));
        }

        @Test
        @DisplayName("rejects non-numeric port - http://example.com:abc")
        void rejectsNonNumericPort() {
            assertFalse(validator.isValid("http://example.com:abc", null));
        }

        @Test
        @DisplayName("rejects underscore in hostname - http://my_server.com")
        void rejectsUnderscoreInHostname() {
            assertFalse(validator.isValid("http://my_server.com", null));
        }

        @Test
        @DisplayName("rejects percent in hostname - http://example%20.com")
        void rejectsPercentInHostname() {
            assertFalse(validator.isValid("http://example%20.com", null));
        }
    }
}
