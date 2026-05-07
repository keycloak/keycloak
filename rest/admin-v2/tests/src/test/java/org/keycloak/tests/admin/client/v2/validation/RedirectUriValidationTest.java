package org.keycloak.tests.admin.client.v2.validation;

import org.keycloak.representations.admin.v2.validators.ValidRedirectUrisValidator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ValidRedirectUrisValidator} covering all redirect URI validation scenarios.
 */
class RedirectUriValidationTest {

    @Nested
    @DisplayName("Scenario 1: Root URL is NOT set")
    class RootUrlNotSet {
        private final boolean hasRootUrl = false;

        @Nested
        @DisplayName("Invalid URIs")
        class InvalidUris {

            @Test
            @DisplayName("rejects relative path without scheme - foo/bar")
            void rejectsRelativePathWithoutScheme() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("foo/bar", hasRootUrl));
                assertNotNull(ValidRedirectUrisValidator.validateRedirectUri("foo/bar", hasRootUrl));
            }

            @Test
            @DisplayName("rejects relative path with wildcard - foo/*")
            void rejectsRelativePathWithWildcard() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("foo/*", hasRootUrl));
                assertNotNull(ValidRedirectUrisValidator.validateRedirectUri("foo/*", hasRootUrl));
            }

            @Test
            @DisplayName("rejects wildcard in middle of path segment - https://foo/bar/*xzxzxzxcxcx/baz")
            void rejectsWildcardInMiddleOfPathSegment() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*xzxzxzxcxcx/baz", hasRootUrl));
                assertNotNull(ValidRedirectUrisValidator.validateRedirectUri("https://foo/bar/*xzxzxzxcxcx/baz", hasRootUrl));
            }

            @Test
            @DisplayName("rejects wildcard not at end of path - https://foo/bar/*/baz")
            void rejectsWildcardNotAtEndOfPath() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*/baz", hasRootUrl));
                assertNotNull(ValidRedirectUrisValidator.validateRedirectUri("https://foo/bar/*/baz", hasRootUrl));
            }

            @Test
            @DisplayName("rejects wildcard with query parameters - https://foo/bar/*?query")
            void rejectsWildcardWithQueryParams() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*?query", hasRootUrl));
                assertNotNull(ValidRedirectUrisValidator.validateRedirectUri("https://foo/bar/*?query", hasRootUrl));
            }

            @Test
            @DisplayName("rejects wildcard with query parameters and value - https://foo/bar/*?query=value")
            void rejectsWildcardWithQueryParamsAndValue() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*?query=value", hasRootUrl));
            }

            @Test
            @DisplayName("rejects relative path starting with slash - /my/path")
            void rejectsRelativePathStartingWithSlash() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("/my/path", hasRootUrl));
            }

            @Test
            @DisplayName("rejects relative path with wildcard starting with slash - /my/path/*")
            void rejectsRelativePathWithWildcardStartingWithSlash() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("/my/path/*", hasRootUrl));
            }

            @Test
            @DisplayName("rejects empty string")
            void rejectsEmptyString() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("", hasRootUrl));
                assertNotNull(ValidRedirectUrisValidator.validateRedirectUri("", hasRootUrl));
            }

            @Test
            @DisplayName("rejects null")
            void rejectsNull() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri(null, hasRootUrl));
                assertNotNull(ValidRedirectUrisValidator.validateRedirectUri(null, hasRootUrl));
            }

            @Test
            @DisplayName("rejects blank string")
            void rejectsBlankString() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("   ", hasRootUrl));
            }

            @Test
            @DisplayName("rejects wildcard not preceded by slash - https://foo/bar*")
            void rejectsWildcardNotPrecededBySlash() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar*", hasRootUrl));
                assertNotNull(ValidRedirectUrisValidator.validateRedirectUri("https://foo/bar*", hasRootUrl));
            }

            @Test
            @DisplayName("rejects multiple wildcards - https://foo/*/bar/*")
            void rejectsMultipleWildcards() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/*/bar/*", hasRootUrl));
            }

            @Test
            @DisplayName("rejects wildcard with fragment - https://foo/bar/*#fragment")
            void rejectsWildcardWithFragment() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*#fragment", hasRootUrl));
            }
        }

        @Nested
        @DisplayName("Valid URIs")
        class ValidUris {

            @Test
            @DisplayName("accepts https URL with wildcard at end - https://foo/bar/*")
            void acceptsHttpsUrlWithWildcardAtEnd() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*", hasRootUrl));
                assertNull(ValidRedirectUrisValidator.validateRedirectUri("https://foo/bar/*", hasRootUrl));
            }

            @Test
            @DisplayName("accepts https URL without wildcard - https://foo/bar")
            void acceptsHttpsUrlWithoutWildcard() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar", hasRootUrl));
                assertNull(ValidRedirectUrisValidator.validateRedirectUri("https://foo/bar", hasRootUrl));
            }

            @Test
            @DisplayName("accepts https URL with port - https://foo:8443/bar")
            void acceptsHttpsUrlWithPort() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("https://foo:8443/bar", hasRootUrl));
                assertNull(ValidRedirectUrisValidator.validateRedirectUri("https://foo:8443/bar", hasRootUrl));
            }

            @Test
            @DisplayName("accepts http URL - http://example.com/callback")
            void acceptsHttpUrl() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("http://example.com/callback", hasRootUrl));
            }

            @Test
            @DisplayName("accepts full wildcard - *")
            void acceptsFullWildcard() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("*", hasRootUrl));
                assertNull(ValidRedirectUrisValidator.validateRedirectUri("*", hasRootUrl));
            }

            @Test
            @DisplayName("accepts plus sign for post-logout - +")
            void acceptsPlusSign() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("+", hasRootUrl));
                assertNull(ValidRedirectUrisValidator.validateRedirectUri("+", hasRootUrl));
            }

            @Test
            @DisplayName("accepts minus sign for post-logout - -")
            void acceptsMinusSign() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("-", hasRootUrl));
                assertNull(ValidRedirectUrisValidator.validateRedirectUri("-", hasRootUrl));
            }

            @Test
            @DisplayName("accepts URL with query parameters (no wildcard) - https://foo/bar?query=value")
            void acceptsUrlWithQueryParams() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar?query=value", hasRootUrl));
            }

            @Test
            @DisplayName("accepts URL with fragment (no wildcard) - https://foo/bar#fragment")
            void acceptsUrlWithFragment() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar#fragment", hasRootUrl));
            }

            @Test
            @DisplayName("accepts URL ending with /* - https://example.com/*")
            void acceptsUrlEndingWithWildcard() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("https://example.com/*", hasRootUrl));
            }

            @Test
            @DisplayName("accepts deep path with wildcard - https://foo/bar/baz/qux/*")
            void acceptsDeepPathWithWildcard() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/baz/qux/*", hasRootUrl));
            }

            @Test
            @DisplayName("accepts custom scheme URL - myapp://callback")
            void acceptsCustomSchemeUrl() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("myapp://callback", hasRootUrl));
            }

            @ParameterizedTest
            @ValueSource(strings = {
                "https://localhost:8080/callback",
                "http://127.0.0.1:3000/auth",
                "https://example.com/auth/callback",
                "https://sub.domain.example.com/path/*"
            })
            @DisplayName("accepts various valid absolute URLs")
            void acceptsVariousValidAbsoluteUrls(String uri) {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri(uri, hasRootUrl));
            }
        }
    }

    @Nested
    @DisplayName("Scenario 2: Root URL IS set")
    class RootUrlSet {
        private final boolean hasRootUrl = true;

        @Nested
        @DisplayName("Invalid URIs")
        class InvalidUris {

            @Test
            @DisplayName("rejects wildcard in middle of path segment - https://foo/bar/*xzxzxzxcxcx/baz")
            void rejectsWildcardInMiddleOfPathSegment() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*xzxzxzxcxcx/baz", hasRootUrl));
                assertNotNull(ValidRedirectUrisValidator.validateRedirectUri("https://foo/bar/*xzxzxzxcxcx/baz", hasRootUrl));
            }

            @Test
            @DisplayName("rejects wildcard not at end of path - https://foo/bar/*/baz")
            void rejectsWildcardNotAtEndOfPath() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*/baz", hasRootUrl));
            }

            @Test
            @DisplayName("rejects wildcard with query parameters - https://foo/bar/*?query")
            void rejectsWildcardWithQueryParams() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*?query", hasRootUrl));
            }

            @Test
            @DisplayName("rejects wildcard not preceded by slash - https://foo/bar*")
            void rejectsWildcardNotPrecededBySlash() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar*", hasRootUrl));
            }

            @Test
            @DisplayName("rejects multiple wildcards - https://foo/*/bar/*")
            void rejectsMultipleWildcards() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/*/bar/*", hasRootUrl));
            }

            @Test
            @DisplayName("rejects empty string")
            void rejectsEmptyString() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("", hasRootUrl));
            }

            @Test
            @DisplayName("rejects null")
            void rejectsNull() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri(null, hasRootUrl));
            }

            @Test
            @DisplayName("rejects wildcard with fragment - https://foo/bar/*#fragment")
            void rejectsWildcardWithFragment() {
                assertFalse(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*#fragment", hasRootUrl));
            }
        }

        @Nested
        @DisplayName("Valid URIs")
        class ValidUris {

            @Test
            @DisplayName("accepts https URL with wildcard at end - https://foo/bar/*")
            void acceptsHttpsUrlWithWildcardAtEnd() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar/*", hasRootUrl));
            }

            @Test
            @DisplayName("accepts https URL without wildcard - https://foo/bar")
            void acceptsHttpsUrlWithoutWildcard() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("https://foo/bar", hasRootUrl));
            }

            @Test
            @DisplayName("accepts https URL with port - https://foo:8443/bar")
            void acceptsHttpsUrlWithPort() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("https://foo:8443/bar", hasRootUrl));
            }

            @Test
            @DisplayName("accepts relative path - foo/bar (relative to root)")
            void acceptsRelativePath() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("foo/bar", hasRootUrl));
                assertNull(ValidRedirectUrisValidator.validateRedirectUri("foo/bar", hasRootUrl));
            }

            @Test
            @DisplayName("accepts relative path with wildcard - foo/*")
            void acceptsRelativePathWithWildcard() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("foo/*", hasRootUrl));
                assertNull(ValidRedirectUrisValidator.validateRedirectUri("foo/*", hasRootUrl));
            }

            @Test
            @DisplayName("accepts relative path starting with slash - /my/path")
            void acceptsRelativePathStartingWithSlash() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("/my/path", hasRootUrl));
            }

            @Test
            @DisplayName("accepts relative path with wildcard starting with slash - /my/path/*")
            void acceptsRelativePathWithWildcardStartingWithSlash() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("/my/path/*", hasRootUrl));
            }

            @Test
            @DisplayName("accepts full wildcard - *")
            void acceptsFullWildcard() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("*", hasRootUrl));
            }

            @Test
            @DisplayName("accepts plus sign for post-logout - +")
            void acceptsPlusSign() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("+", hasRootUrl));
            }

            @Test
            @DisplayName("accepts minus sign for post-logout - -")
            void acceptsMinusSign() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("-", hasRootUrl));
            }

            @Test
            @DisplayName("accepts deep relative path - app/auth/callback")
            void acceptsDeepRelativePath() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("app/auth/callback", hasRootUrl));
            }

            @Test
            @DisplayName("accepts deep relative path with wildcard - app/auth/*")
            void acceptsDeepRelativePathWithWildcard() {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri("app/auth/*", hasRootUrl));
            }

            @ParameterizedTest
            @ValueSource(strings = {
                "callback",
                "auth/callback",
                "/callback",
                "/auth/callback/*",
                "sub/path/to/resource"
            })
            @DisplayName("accepts various valid relative paths when root URL is set")
            void acceptsVariousValidRelativePaths(String uri) {
                assertTrue(ValidRedirectUrisValidator.isValidRedirectUri(uri, hasRootUrl));
            }
        }
    }

    @Nested
    @DisplayName("Error messages")
    class ErrorMessages {

        @Test
        @DisplayName("provides correct error for empty URI")
        void providesCorrectErrorForEmptyUri() {
            String error = ValidRedirectUrisValidator.validateRedirectUri("", false);
            assertEquals("Redirect URI cannot be empty", error);
        }

        @Test
        @DisplayName("provides correct error for relative URI without root URL")
        void providesCorrectErrorForRelativeUriWithoutRootUrl() {
            String error = ValidRedirectUrisValidator.validateRedirectUri("foo/bar", false);
            assertEquals("Redirect URI must be an absolute URI (include scheme like https://) when Root URL is not set", error);
        }

        @Test
        @DisplayName("provides correct error for wildcard not at end")
        void providesCorrectErrorForWildcardNotAtEnd() {
            String error = ValidRedirectUrisValidator.validateRedirectUri("https://foo/*/bar", false);
            assertEquals("Wildcard (*) must be at the end of the URI", error);
        }

        @Test
        @DisplayName("provides correct error for wildcard not preceded by slash")
        void providesCorrectErrorForWildcardNotPrecededBySlash() {
            String error = ValidRedirectUrisValidator.validateRedirectUri("https://foo/bar*", false);
            assertEquals("Wildcard (*) must be preceded by a slash (/)", error);
        }

        @Test
        @DisplayName("provides correct error for wildcard with query parameters")
        void providesCorrectErrorForWildcardWithQueryParams() {
            String error = ValidRedirectUrisValidator.validateRedirectUri("https://foo/bar/*?query", false);
            assertEquals("Wildcard (*) must be at the end of the URI", error);
        }

        @Test
        @DisplayName("provides correct error for wildcard with fragment")
        void providesCorrectErrorForWildcardWithFragment() {
            String error = ValidRedirectUrisValidator.validateRedirectUri("https://foo/bar/*#frag", false);
            assertEquals("Wildcard (*) must be at the end of the URI", error);
        }

        @Test
        @DisplayName("provides correct error for multiple wildcards")
        void providesCorrectErrorForMultipleWildcards() {
            String error = ValidRedirectUrisValidator.validateRedirectUri("https://foo/*/bar/*", false);
            assertEquals("Only one wildcard (*) is allowed at the end of the URI", error);
        }
    }
}
