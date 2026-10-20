package org.keycloak.cookie;

import org.junit.Assert;
import org.junit.Test;

public class DefaultCookieProviderFactoryTest {

    @Test
    public void testValidCookiePrefixes() {
        // Should not throw for valid prefixes
        DefaultCookieProviderFactory.validateCookiePrefix("KC26_");
        DefaultCookieProviderFactory.validateCookiePrefix("MY-PREFIX-");
        DefaultCookieProviderFactory.validateCookiePrefix("app1.");
        DefaultCookieProviderFactory.validateCookiePrefix("A");
        DefaultCookieProviderFactory.validateCookiePrefix("prefix_with_underscores");
    }

    @Test
    public void testNullAndEmptyPrefixAreValid() {
        DefaultCookieProviderFactory.validateCookiePrefix(null);
        DefaultCookieProviderFactory.validateCookiePrefix("");
    }

    @Test
    public void testPrefixWithSpaceIsInvalid() {
        assertInvalidPrefix("KC 26", ' ');
    }

    @Test
    public void testPrefixWithSemicolonIsInvalid() {
        assertInvalidPrefix("KC;26", ';');
    }

    @Test
    public void testPrefixWithCommaIsInvalid() {
        assertInvalidPrefix("KC,26", ',');
    }

    @Test
    public void testPrefixWithEqualsIsInvalid() {
        assertInvalidPrefix("KC=26", '=');
    }

    @Test
    public void testPrefixWithDoubleQuoteIsInvalid() {
        assertInvalidPrefix("KC\"26", '"');
    }

    @Test
    public void testPrefixWithBackslashIsInvalid() {
        assertInvalidPrefix("KC\\26", '\\');
    }

    @Test
    public void testPrefixWithControlCharIsInvalid() {
        assertInvalidPrefix("KC\t26", '\t');
    }

    private void assertInvalidPrefix(String prefix, char expectedBadChar) {
        try {
            DefaultCookieProviderFactory.validateCookiePrefix(prefix);
            Assert.fail("Expected IllegalArgumentException for prefix: " + prefix);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Error message should mention the invalid character",
                    e.getMessage().contains("Invalid cookie prefix"));
        }
    }
}
