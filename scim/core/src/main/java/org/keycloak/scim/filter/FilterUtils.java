package org.keycloak.scim.filter;

import org.keycloak.utils.StringUtil;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;


/**
 * Utility class for parsing SCIM filter expressions using ANTLR.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FilterUtils {

    public static final int MAX_FILTER_LENGTH = 2048;
    public static final int MAX_FILTER_DEPTH = 10;

    /**
     * Parses a SCIM filter expression string into an abstract syntax tree.
     *
     * @param filterExpression the filter expression to parse (RFC 7644 section 3.4.2.2)
     * @return the parsed filter context (AST root)
     * @throws ScimFilterException if the filter expression has syntax errors
     */
    public static ScimFilterParser.FilterContext parseFilter(String filterExpression) {
        if (StringUtil.isBlank(filterExpression)) {
            throw new ScimFilterException("Filter expression cannot be null or empty");
        }
        if (filterExpression.length() > MAX_FILTER_LENGTH) {
            throw new ScimFilterException(
                    "Filter expression exceeds maximum allowed length of %d characters".formatted(MAX_FILTER_LENGTH));
        }
        validateFilterDepth(filterExpression);

        CharStream charStream = CharStreams.fromString(filterExpression);
        ScimFilterLexer lexer = new ScimFilterLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ScimFilterParser parser = new ScimFilterParser(tokens);

        // Custom error listener
        ErrorListener errorListener = new ErrorListener();
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        ScimFilterParser.FilterContext context = parser.filter();

        if (errorListener.hasErrors()) {
            String errors = String.join(", ", errorListener.getErrorMessages());
            throw new ScimFilterException("Invalid filter syntax: " + errors);
        }

        validateNullCompValues(context);
        return context;
    }

    /**
     * Extracts the comparison value from a parsed {@code compValue} node as a string.
     *
     * @param ctx the comparison value context from the parse tree
     * @return the extracted value, or {@code null} for NULL literals
     */
    public static String extractCompValue(ScimFilterParser.CompValueContext ctx) {
        if (ctx.STRING() != null) {
            String raw = ctx.STRING().getText();
            return unescapeJsonString(raw.substring(1, raw.length() - 1));
        }
        if (ctx.TRUE() != null) return "true";
        if (ctx.FALSE() != null) return "false";
        if (ctx.NULL() != null) return null;
        if (ctx.NUMBER() != null) return ctx.NUMBER().getText();
        return null;
    }

    /**
     * Validates that the nesting depth of parenthesized groups and value path brackets does not exceed {@link #MAX_FILTER_DEPTH}.
     * Runs before ANTLR parsing to avoid building a deep parse tree that could cause a {@code StackOverflowError}.
     */
    private static void validateFilterDepth(String filterExpression) {
        int depth = 0;
        int maxDepth = 0;
        boolean inString = false;
        for (int i = 0; i < filterExpression.length(); i++) {
            char c = filterExpression.charAt(i);
            // skip characters inside quoted string literals — they are values, not structural
            if (inString) {
                if (c == '\\' && i + 1 < filterExpression.length()) {
                    i++; // skip escaped character
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                continue;
            }
            switch (c) {
                case '"' -> inString = true;
                // parenthesized groups and value path brackets both produce recursive grammar rules
                case '(', '[' -> maxDepth = Math.max(maxDepth, ++depth);
                case ')', ']' -> depth = Math.max(0, depth - 1);
            }
        }
        if (maxDepth > MAX_FILTER_DEPTH) {
            throw new ScimFilterException(
                    "Filter expression exceeds maximum allowed nesting depth of %d".formatted(MAX_FILTER_DEPTH));
        }
    }

    private static void validateNullCompValues(ScimFilterParser.FilterContext filterCtx) {
        new ScimFilterParserBaseVisitor<Void>() {
            @Override
            public Void visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
                if (ctx.compValue().NULL() != null) {
                    String operator = ctx.compareOp().getText().toLowerCase();
                    if (!operator.equals("eq") && !operator.equals("ne")) {
                        throw new ScimFilterException(
                                "Operator '%s' does not accept null values".formatted(operator));
                    }
                }
                return null;
            }
        }.visit(filterCtx);
    }

    /**
     * Unescapes a JSON string value (without surrounding quotes) per RFC 8259.
     * Unicode escape sequences are handled by the ANTLR lexer.
     */
    public static String unescapeJsonString(String s) {
        if (s.indexOf('\\') == -1) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/'  -> sb.append('/');
                    case 'b'  -> sb.append('\b');
                    case 'f'  -> sb.append('\f');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> sb.append('\\').append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
