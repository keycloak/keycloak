package org.keycloak.scim.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for SCIM filter parsing.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FilterUtilsTest {

    @Test
    public void testSimpleEqualityFilter() {
        ScimFilterParser.FilterContext ctx = FilterUtils.parseFilter("userName eq \"john\"");
        assertNotNull(ctx);
        assertNotNull(ctx.expression());
    }

    @Test
    public void testCaseInsensitiveOperators() {
        // Test all case variations
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName EQ \"john\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName eq \"john\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName Eq \"john\""));

        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName NE \"john\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName CO \"john\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName SW \"john\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName EW \"john\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("age GT 30"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("age GE 30"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("age LT 30"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("age LE 30"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName PR"));
    }

    @Test
    public void testLogicalOperators() {
        // AND
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName eq \"john\" and active eq true"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName eq \"john\" AND active eq true"));

        // OR
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName eq \"john\" or userName eq \"jane\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName eq \"john\" OR userName eq \"jane\""));

        // NOT
        assertDoesNotThrow(() -> FilterUtils.parseFilter("not (userName eq \"john\")"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("NOT (userName eq \"john\")"));
    }

    @Test
    public void testComplexFilter() {
        String filter = "(userName eq \"john\" or userName eq \"jane\") and active eq true";
        ScimFilterParser.FilterContext ctx = FilterUtils.parseFilter(filter);
        assertNotNull(ctx);
    }

    @Test
    public void testPresentOperator() {
        ScimFilterParser.FilterContext ctx = FilterUtils.parseFilter("userName pr");
        assertNotNull(ctx);
    }

    @Test
    public void testStringComparison() {
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName co \"oh\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName sw \"j\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName ew \"n\""));
    }

    @Test
    public void testNumericComparison() {
        assertDoesNotThrow(() -> FilterUtils.parseFilter("age gt 30"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("age ge 30"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("age lt 50"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("age le 50"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("age eq 42"));
    }

    @Test
    public void testBooleanLiterals() {
        assertDoesNotThrow(() -> FilterUtils.parseFilter("active eq true"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("active eq false"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("active eq TRUE"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("active eq FALSE"));
    }

    @Test
    public void testNullLiteral() {
        assertDoesNotThrow(() -> FilterUtils.parseFilter("middleName eq null"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("middleName eq NULL"));
    }

    @Test
    public void testNestedAttributes() {
        assertDoesNotThrow(() -> FilterUtils.parseFilter("name.givenName eq \"John\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("name.familyName eq \"Doe\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("emails[0].value eq \"john@example.com\""));
    }

    @Test
    public void testSchemaPrefix() {
        // Full URN schema prefix support
        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "urn:ietf:params:scim:schemas:core:2.0:User:userName eq \"john\""));

        // HTTP schema prefix
        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "http://example.com/schemas/User:userName eq \"john\""));

        // HTTPS schema prefix
        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "https://example.com/schemas/User:userName eq \"john\""));
    }

    @Test
    public void testOperatorPrecedence() {
        // NOT has highest precedence
        assertDoesNotThrow(() -> FilterUtils.parseFilter("not userName eq \"john\" and active eq true"));

        // AND has higher precedence than OR
        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "userName eq \"john\" and active eq true or userName eq \"jane\""));
    }

    @Test
    public void testParenthesesGrouping() {
        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "(userName eq \"john\" or userName eq \"jane\") and active eq true"));

        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "userName eq \"john\" or (userName eq \"jane\" and active eq true)"));
    }

    @Test
    public void testEscapedStrings() {
        // JSON string escaping
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName eq \"john\\\"doe\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("path eq \"c:\\\\users\\\\john\""));
    }

    @Test
    public void testInvalidSyntax() {
        // Missing value
        ScimFilterException e1 = assertThrows(ScimFilterException.class,
            () -> FilterUtils.parseFilter("userName eq"));
        assertTrue(e1.getMessage().contains("Invalid filter syntax"));

        // Invalid operator
        ScimFilterException e2 = assertThrows(ScimFilterException.class,
            () -> FilterUtils.parseFilter("userName invalid \"john\""));
        assertTrue(e2.getMessage().contains("Invalid filter syntax"));

        // Mismatched parentheses
        ScimFilterException e3 = assertThrows(ScimFilterException.class,
            () -> FilterUtils.parseFilter("(userName eq \"john\""));
        assertTrue(e3.getMessage().contains("Invalid filter syntax"));
    }

    @Test
    public void testEmptyOrNullFilter() {
        assertThrows(ScimFilterException.class, () -> FilterUtils.parseFilter(null));
        assertThrows(ScimFilterException.class, () -> FilterUtils.parseFilter(""));
        assertThrows(ScimFilterException.class, () -> FilterUtils.parseFilter("   "));
    }

    @Test
    public void testComplexRealWorldFilters() {
        // Complex filter from SCIM spec
        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "userType eq \"Employee\" and (emails co \"example.com\" or emails.value co \"example.org\")"));

        // Multiple conditions
        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "userName sw \"J\" and active eq true and meta.created gt \"2024-01-01T00:00:00Z\""));

        // Negation with complex expression
        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "not (userName eq \"admin\" or userName eq \"root\")"));
    }

    @Test
    public void testWhitespaceHandling() {
        // Extra whitespace should be handled correctly
        assertDoesNotThrow(() -> FilterUtils.parseFilter("  userName   eq   \"john\"  "));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName eq \"john\"and active eq true"));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("userName eq \"john\" and active eq true"));
    }

    @Test
    public void testAttributePathWithArray() {
        assertDoesNotThrow(() -> FilterUtils.parseFilter("emails[0].value eq \"john@example.com\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter("phoneNumbers[0].value pr"));
    }

    @Test
    public void testDateTimeComparison() {
        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "meta.created gt \"2024-01-01T00:00:00Z\""));
        assertDoesNotThrow(() -> FilterUtils.parseFilter(
            "meta.lastModified le \"2024-12-31T23:59:59Z\""));
    }
}
