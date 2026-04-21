package org.keycloak.services.client.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryParseUtilsTest {

    @Test
    void parseSingleBareValue() {
        var ctx = QueryParseUtils.parse("clientId:my-app");
        assertEquals(1, ctx.expression().size());
        assertEquals("clientId", ctx.expression(0).fieldPath().getText());
    }

    @Test
    void parseQuotedValue() {
        var ctx = QueryParseUtils.parse("description:\"My OAuth Application\"");
        assertEquals(1, ctx.expression().size());
    }

    @Test
    void parseMultipleExpressions() {
        var ctx = QueryParseUtils.parse("publicClient:true protocol:openid-connect");
        assertEquals(2, ctx.expression().size());
    }

    @Test
    void parseDotNotation() {
        var ctx = QueryParseUtils.parse("auth.method:client-jwt");
        var fieldPath = ctx.expression(0).fieldPath();
        assertEquals(2, fieldPath.BAREWORD().size());
        assertEquals("auth", fieldPath.BAREWORD(0).getText());
        assertEquals("method", fieldPath.BAREWORD(1).getText());
    }

    @Test
    void parseListValue() {
        var ctx = QueryParseUtils.parse("roles:[admin,user]");
        assertEquals(1, ctx.expression().size());
        var value = ctx.expression(0).value();
        assertNotNull(value);
    }

    @Test
    void parseListWithSpaces() {
        var ctx = QueryParseUtils.parse("roles:[admin, user, viewer]");
        assertEquals(1, ctx.expression().size());
    }

    @Test
    void parseMapEntries() {
        var ctx = QueryParseUtils.parse("attributes:[owner:team-a,env:prod]");
        assertEquals(1, ctx.expression().size());
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
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("no-colon-here"));
    }

    @Test
    void parseColonOnly() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse(":"));
    }

    @Test
    void parseValueOnly() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse(":value"));
    }

    @Test
    void parseFieldOnly() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("field:"));
    }

    @Test
    void parseMultipleColons() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("field:value:extra"));
    }

    @Test
    void parseEmptyBrackets() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("field:[]"));
    }

    @Test
    void parseUnclosedBracket() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("field:[value"));
    }

    @Test
    void parseUnclosedQuote() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("field:\"unclosed"));
    }

    @Test
    void parseNestedBrackets() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("field:[[nested]]"));
    }

    @Test
    void parseTrailingColon() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("field:value:"));
    }

    @Test
    void parseCommaOutsideBrackets() {
        assertThrows(ClientQueryException.class, () -> QueryParseUtils.parse("field:a,b"));
    }

    @Test
    void parseEmptyQuotedString() {
        var ctx = QueryParseUtils.parse("field:\"\"");
        assertEquals(1, ctx.expression().size());
    }
}
