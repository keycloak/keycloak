package org.keycloak.client.admin.cli.commands.v2;

import java.io.IOException;
import java.util.regex.Pattern;

import org.keycloak.client.admin.cli.v2.CliJsonOutputHighlighter;
import org.keycloak.client.cli.util.OutputUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.keycloak.client.admin.cli.v2.CliJsonOutputHighlighter.COLOR_BOOLEAN;
import static org.keycloak.client.admin.cli.v2.CliJsonOutputHighlighter.COLOR_KEY;
import static org.keycloak.client.admin.cli.v2.CliJsonOutputHighlighter.COLOR_NULL;
import static org.keycloak.client.admin.cli.v2.CliJsonOutputHighlighter.COLOR_NUMBER;
import static org.keycloak.client.admin.cli.v2.CliJsonOutputHighlighter.COLOR_STRING;
import static org.keycloak.client.admin.cli.v2.CliJsonOutputHighlighter.ESC;
import static org.keycloak.client.admin.cli.v2.CliJsonOutputHighlighter.RESET;
import static org.keycloak.client.admin.cli.v2.CliJsonOutputHighlighter.arrayColorAtDepth;
import static org.keycloak.client.admin.cli.v2.CliJsonOutputHighlighter.objectColorAtDepth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CliJsonOutputHighlighterTest {

    private static final String ANSI_CODE_PATTERN = Pattern.quote(ESC) + "[0-9;]*m";

    @Test
    public void testStringValues() throws IOException {
        String json = "{\"a\":\"hello\",\"b\":\"world\"}";
        String result = highlight(json, false);

        assertHasColor(result, "\"hello\"", COLOR_STRING);
        assertHasColor(result, "\"world\"", COLOR_STRING);
    }

    @Test
    public void testNumberValues() throws IOException {
        String json = "{\"x\":42,\"y\":3.14}";
        String result = highlight(json, false);

        assertHasColor(result, "42", COLOR_NUMBER);
        assertHasColor(result, "3.14", COLOR_NUMBER);
    }

    @Test
    public void testBooleanValues() throws IOException {
        String json = "{\"a\":true,\"b\":false}";
        String result = highlight(json, false);

        assertHasColor(result, "true", COLOR_BOOLEAN);
        assertHasColor(result, "false", COLOR_BOOLEAN);
    }

    @Test
    public void testNullValues() throws IOException {
        String json = "{\"a\":null,\"b\":null}";
        String result = highlight(json, false);

        assertHasColor(result, "null", COLOR_NULL);
    }

    @Test
    public void testKeys() throws IOException {
        String json = "{\"firstName\":\"Alice\",\"lastName\":\"Bob\"}";
        String result = highlight(json, false);

        assertHasColor(result, "\"firstName\"", COLOR_KEY);
        assertHasColor(result, "\"lastName\"", COLOR_KEY);
    }

    @Test
    public void testObjectPunctuation() throws IOException {
        String json = "{\"a\":1,\"b\":2}";
        String result = highlight(json, false);

        String expected = objectColorAtDepth(0);
        assertHasColor(result, "{", expected);
        assertHasColor(result, "}", expected);
        assertHasColor(result, ":", expected);
        assertHasColor(result, ",", expected);
    }

    @Test
    public void testArrayPunctuation() throws IOException {
        String json = "[1,2,3]";
        String result = highlight(json, false);

        String expected = arrayColorAtDepth(0);
        assertHasColor(result, "[", expected);
        assertHasColor(result, "]", expected);
        assertHasColor(result, ",", expected);
    }

    @Test
    public void testNestedObjectsHaveDifferentColors() throws IOException {
        int depth = CliJsonOutputHighlighter.getObjectDepthCycle() + 1;
        String json = buildNestedObjectJson(depth);
        String result = highlight(json, false);

        for (int i = 0; i < depth; i++) {
            assertNthTokenHasColor(result, "{", i + 1, objectColorAtDepth(i));
            // each level has 2 colons: {"key":"val","nested":{...}}
            assertNthTokenHasColor(result, ":", 2 * i + 1, objectColorAtDepth(i));
            assertNthTokenHasColor(result, ":", 2 * i + 2, objectColorAtDepth(i));
            assertNthTokenHasColor(result, ",", i + 1, objectColorAtDepth(i));
        }
    }

    @Test
    public void testNestedArraysHaveDifferentColors() throws IOException {
        int depth = CliJsonOutputHighlighter.getArrayDepthCycle() + 1;
        String json = buildNestedArrayJson(depth);
        String result = highlight(json, false);

        for (int i = 0; i < depth; i++) {
            assertNthTokenHasColor(result, "[", i + 1, arrayColorAtDepth(i));
            assertNthTokenHasColor(result, ",", i + 1, arrayColorAtDepth(i));
        }
    }

    @Test
    public void testPrettyPrintedOutput() throws IOException {
        String result = highlight("{\"name\":\"Alice\",\"age\":30}", false);

        assertTrue("Pretty output should contain newlines", result.contains(System.lineSeparator()));
        assertTrue("Pretty output should contain indentation", result.contains("  "));
    }

    @Test
    public void testCompressedOutput() throws IOException {
        String result = highlight("{\"name\":\"Alice\",\"age\":30}", true);

        assertFalse("Compressed output should not contain newlines", result.contains(System.lineSeparator()));
        assertFalse("Compressed output should not contain indentation", result.contains("  "));
    }

    @Test
    public void testCompressedPreservesJsonStructure() throws IOException {
        String json = "{\"name\":\"Alice\",\"age\":30,\"active\":true}";
        String result = highlight(json, true);

        assertEquals("Stripped output should be valid compressed JSON", json, stripAnsiCodes(result));
    }

    @Test
    public void testPrettyPrintPreservesJsonContent() throws IOException {
        String json = "{\"name\":\"Alice\",\"age\":30}";
        String result = highlight(json, false);

        String stripped = stripAnsiCodes(result);
        assertTrue("Should contain all keys and values",
                stripped.contains("\"name\"") && stripped.contains("\"Alice\"") && stripped.contains("30"));
    }

    @Test
    public void testStringWithEscapedQuotes() throws IOException {
        String json = "{\"msg\":\"hello \\\"world\\\"\"}";
        String result = highlight(json, false);

        assertHasColor(result, "\"hello \\\"world\\\"\"", COLOR_STRING);
    }

    @Test
    public void testStringWithJsonEscapeSequences() throws IOException {
        String json = "{\"a\":\"line1\\nline2\",\"b\":\"col1\\tcol2\",\"c\":\"back\\\\slash\"}";
        String result = highlight(json, true);

        String stripped = stripAnsiCodes(result);
        assertEquals("Escape sequences should be preserved in output", json, stripped);
    }

    @Test
    public void testPrettyFormatMatchesJacksonForSimpleObject() throws IOException {
        String json = "{\"name\":\"Alice\",\"age\":30}";
        assertFormattingMatchesJackson(json);
    }

    @Test
    public void testPrettyFormatMatchesJacksonForArrayOfObjects() throws IOException {
        String json = "[{\"a\":1},{\"b\":2}]";
        assertFormattingMatchesJackson(json);
    }

    @Test
    public void testPrettyFormatMatchesJacksonForNestedObject() throws IOException {
        String json = "{\"user\":{\"name\":\"Bob\",\"active\":true}}";
        assertFormattingMatchesJackson(json);
    }

    @Test
    public void testPrettyFormatMatchesJacksonForArrayOfPrimitives() throws IOException {
        String json = "{\"items\":[1,2,3]}";
        assertFormattingMatchesJackson(json);
    }

    @Test
    public void testOutputEndsWithReset() throws IOException {
        String result = highlight("{\"a\":1}", false);

        assertTrue("Output should end with ANSI reset to avoid color bleeding",
                result.stripTrailing().endsWith(RESET));
    }

    private String highlight(String json, boolean compressed) throws IOException {
        JsonNode tree = OutputUtil.MAPPER.readTree(json);
        String formatted = compressed ? tree.toString() : OutputUtil.MAPPER.writeValueAsString(tree);
        return CliJsonOutputHighlighter.highlight(formatted);
    }

    private void assertHasColor(String text, String token, String expectedColor) {
        assertNthTokenHasColor(text, token, 1, expectedColor);
    }

    private void assertNthTokenHasColor(String text, String token, int n, String expectedColor) {
        String tokenWithReset = token + RESET;
        int idx = -1;
        for (int i = 0; i < n; i++) {
            idx = text.indexOf(tokenWithReset, idx + 1);
            assertTrue("Occurrence " + n + " of '" + token + "' not found in: " + text, idx >= 0);
        }
        assertTrue("Occurrence " + n + " of '" + token + "' should have color " + expectedColor + " in: " + text,
                text.startsWith(expectedColor + token, idx - expectedColor.length()));
    }

    private void assertFormattingMatchesJackson(String json) throws IOException {
        JsonNode tree = OutputUtil.MAPPER.readTree(json);
        String jacksonOutput = OutputUtil.MAPPER.writeValueAsString(tree);
        String highlightedOutput = stripAnsiCodes(CliJsonOutputHighlighter.highlight(jacksonOutput));
        assertEquals("Highlighted formatting should match Jackson's DefaultPrettyPrinter:", jacksonOutput, highlightedOutput);
    }

    private String stripAnsiCodes(String text) {
        return text.replaceAll(ANSI_CODE_PATTERN, "");
    }

    private String buildNestedObjectJson(int depth) {
        StringBuilder open = new StringBuilder();
        StringBuilder close = new StringBuilder();
        for (int i = 0; i < depth - 1; i++) {
            open.append("{\"k").append(i).append("\":\"v").append(i).append("\",\"d").append(i).append("\":");
            close.insert(0, "}");
        }
        open.append("{\"a\":1,\"b\":2}");
        return open + close.toString();
    }

    private String buildNestedArrayJson(int depth) {
        StringBuilder open = new StringBuilder();
        StringBuilder close = new StringBuilder();
        for (int i = 0; i < depth - 1; i++) {
            open.append("[0,");
            close.insert(0, "]");
        }
        open.append("[1,2]");
        return open + close.toString();
    }
}
