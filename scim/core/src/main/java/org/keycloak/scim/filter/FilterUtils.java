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

        return context;
    }
}
