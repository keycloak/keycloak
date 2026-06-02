package org.keycloak.services.client.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryParseUtilsTest {

    // parse() wrapper tests (grammar correctness is tested by SCIM FilterUtilsTest)

    @Test
    void parseValidQuery() {
        var ctx = QueryParseUtils.parse("clientId eq \"my-app\"");
        assertNotNull(ctx.expression());
    }

    @Test
    void parseBooleanValue() {
        var ctx = QueryParseUtils.parse("enabled eq true");
        assertNotNull(ctx.expression());
    }

    @Test
    void parseNullQueryThrows() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse(null));
    }

    @Test
    void parseEmptyQueryThrows() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse(""));
    }

    @Test
    void parseBlankQueryThrows() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("   "));
    }

    @Test
    void parseInvalidSyntaxThrows() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("not-a-valid-query"));
    }

    // validate() tests

    @Test
    void validateKnownField() {
        var ctx = QueryParseUtils.parse("clientId eq \"my-app\"");
        QueryParseUtils.validate(ctx);
    }

    @Test
    void validateUnknownFieldThrows() {
        var ctx = QueryParseUtils.parse("unknownField eq \"value\"");
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.validate(ctx));
    }

    @Test
    void validateDotNotationField() {
        var ctx = QueryParseUtils.parse("auth.method eq \"client-secret\"");
        QueryParseUtils.validate(ctx);
    }

    @Test
    void validateUnknownDotNotationFieldThrows() {
        var ctx = QueryParseUtils.parse("auth.unknown eq \"value\"");
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.validate(ctx));
    }

    @Test
    void validateUnsupportedOperatorThrows() {
        var ctx = QueryParseUtils.parse("clientId gt \"value\"");
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.validate(ctx));
    }

    @Test
    void validatePresenceOperatorAllowed() {
        var ctx = QueryParseUtils.parse("description pr");
        QueryParseUtils.validate(ctx);
    }

    @Test
    void validateValuePathThrows() {
        var ctx = QueryParseUtils.parse("emails[type eq \"work\"]");
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.validate(ctx));
    }

    @Test
    void validateMultipleExpressions() {
        var ctx = QueryParseUtils.parse("clientId eq \"my-app\" and enabled eq true");
        QueryParseUtils.validate(ctx);
    }

    @Test
    void validateMultipleExpressionsOneUnknownThrows() {
        var ctx = QueryParseUtils.parse("clientId eq \"my-app\" and unknownField eq \"value\"");
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.validate(ctx));
    }

    @Test
    void validateOrExpression() {
        var ctx = QueryParseUtils.parse("enabled eq true or protocol eq \"saml\"");
        QueryParseUtils.validate(ctx);
    }

    @Test
    void validateNotExpression() {
        var ctx = QueryParseUtils.parse("not enabled eq false");
        QueryParseUtils.validate(ctx);
    }

    @Test
    void validateUnknownFieldInPresenceThrows() {
        var ctx = QueryParseUtils.parse("unknownField pr");
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.validate(ctx));
    }
}
