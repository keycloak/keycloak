package org.keycloak.client.admin.cli.v2;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import org.keycloak.client.cli.util.OutputUtil;

public final class CliJsonOutputHighlighter {

    public static final String ESC = "\033[";
    public static final String RESET = ESC + "0m";
    public static final String COLOR_KEY     = ESC + "36m"; // Standard Cyan
    public static final String COLOR_STRING  = ESC + "32m"; // Standard Green
    public static final String COLOR_NUMBER  = ESC + "35m"; // Standard Magenta
    public static final String COLOR_BOOLEAN = ESC + "31m"; // Standard Red
    public static final String COLOR_NULL    = ESC + "91m"; // Bright Red

    private static final String[] OBJECT_COLORS = {
            ESC + "1;37m",  // Level 1: Bold White
            ESC + "37m",  // Level 2: Light Gray / Standard White
            ESC + "94m",  // Level 3: Bright Blue
            ESC + "96m",  // Level 4: Bright Cyan
            ESC + "92m",  // Level 5: Bright Green
            ESC + "95m"   // Level 6: Bright Magenta
    };
    private static final String[] ARRAY_COLORS = OBJECT_COLORS;
    private static final int CONTEXT_OBJECT = 1;
    private static final int CONTEXT_ARRAY = 2;
    private static final char COLON = ':';
    private static final char COMMA = ',';
    private static final char DOUBLE_QUOTES = '"';
    private static final int TRUE_LENGTH = "true".length();
    private static final int FALSE_LENGTH = "false".length();
    private static final int NULL_LENGTH = "null".length();

    private CliJsonOutputHighlighter() {
    }

    public static String objectColorAtDepth(int depth) {
        return OBJECT_COLORS[Math.floorMod(depth, getObjectDepthCycle())];
    }

    public static String arrayColorAtDepth(int depth) {
        return ARRAY_COLORS[Math.floorMod(depth, getArrayDepthCycle())];
    }

    public static int getObjectDepthCycle() {
        return OBJECT_COLORS.length;
    }

    public static int getArrayDepthCycle() {
        return ARRAY_COLORS.length;
    }

    public static String highlight(String json) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final Deque<Integer> contextStack = new ArrayDeque<>();
        int objectDepth = -1;
        int arrayDepth = -1;
        int lastEnd = 0;

        try (final var parser = OutputUtil.MAPPER.getFactory().createParser(json)) {
            while (parser.nextToken() != null) {
                final var token = parser.currentToken();
                final int tokenStart = (int) parser.currentTokenLocation().getCharOffset();

                if (tokenStart > lastEnd) {
                    copyGapWithColoredSeparators(sb, json, lastEnd, tokenStart, contextStack, objectDepth, arrayDepth);
                }

                final String color;
                final int tokenEnd;

                switch (token) {
                    case START_OBJECT -> {
                        objectDepth++;
                        contextStack.push(CONTEXT_OBJECT);
                        color = objectColorAtDepth(objectDepth);
                        tokenEnd = tokenStart + 1;
                    }
                    case END_OBJECT -> {
                        color = objectColorAtDepth(objectDepth);
                        objectDepth--;
                        contextStack.pop();
                        tokenEnd = tokenStart + 1;
                    }
                    case START_ARRAY -> {
                        arrayDepth++;
                        contextStack.push(CONTEXT_ARRAY);
                        color = arrayColorAtDepth(arrayDepth);
                        tokenEnd = tokenStart + 1;
                    }
                    case END_ARRAY -> {
                        color = arrayColorAtDepth(arrayDepth);
                        arrayDepth--;
                        contextStack.pop();
                        tokenEnd = tokenStart + 1;
                    }
                    case FIELD_NAME -> {
                        color = COLOR_KEY;
                        tokenEnd = findClosingQuote(json, tokenStart) + 1;
                    }
                    case VALUE_STRING -> {
                        color = COLOR_STRING;
                        tokenEnd = findClosingQuote(json, tokenStart) + 1;
                    }
                    case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> {
                        color = COLOR_NUMBER;
                        tokenEnd = tokenStart + parser.getText().length();
                    }
                    case VALUE_TRUE -> {
                        color = COLOR_BOOLEAN;
                        tokenEnd = tokenStart + TRUE_LENGTH;
                    }
                    case VALUE_FALSE -> {
                        color = COLOR_BOOLEAN;
                        tokenEnd = tokenStart + FALSE_LENGTH;
                    }
                    case VALUE_NULL -> {
                        color = COLOR_NULL;
                        tokenEnd = tokenStart + NULL_LENGTH;
                    }
                    default -> {
                        color = null;
                        tokenEnd = tokenStart;
                    }
                }

                if (color != null) {
                    sb.append(color);
                    sb.append(json, tokenStart, tokenEnd);
                    sb.append(RESET);
                }

                lastEnd = tokenEnd;
            }
        }

        if (lastEnd < json.length()) {
            copyGapWithColoredSeparators(sb, json, lastEnd, json.length(), contextStack, objectDepth, arrayDepth);
        }

        return sb.toString();
    }

    // Copies the gap between tokens (whitespace, separators) verbatim, except : and , which get colored
    private static void copyGapWithColoredSeparators(StringBuilder sb, String json, int from, int to,
                                                      Deque<Integer> contextStack, int objectDepth, int arrayDepth) {
        for (int i = from; i < to; i++) {
            char c = json.charAt(i);
            if (c == COLON || c == COMMA) {
                boolean inObject = Integer.valueOf(CONTEXT_OBJECT).equals(contextStack.peek());
                String sepColor = inObject ? objectColorAtDepth(objectDepth) : arrayColorAtDepth(arrayDepth);
                sb.append(sepColor).append(c).append(RESET);
            } else {
                sb.append(c);
            }
        }
    }

    private static int findClosingQuote(String json, int openQuotePos) {
        int i = openQuotePos + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == DOUBLE_QUOTES) {
                return i;
            } else {
                i++;
            }
        }
        return json.length() - 1;
    }
}
