package org.keycloak.scim.filter;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;


/**
 * Utility class for parsing SCIM filter expressions using ANTLR.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class FilterUtils {

    /**
     * Parses a SCIM filter expression string into an abstract syntax tree.
     *
     * @param filterExpression the filter expression to parse (RFC 7644 section 3.4.2.2)
     * @return the parsed filter context (AST root)
     * @throws ScimFilterException if the filter expression has syntax errors
     */
    public static ScimFilterParser.FilterContext parseFilter(String filterExpression) {
        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            throw new ScimFilterException("Filter expression cannot be null or empty");
        }

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
