package org.keycloak.services.client.query;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class QueryParseUtils {

    public static String extractFieldPath(ClientQueryParser.ExpressionContext expr) {
        return expr.fieldPath().BAREWORD().stream()
                .map(Object::toString)
                .collect(Collectors.joining("."));
    }

    public static void validate(ClientQueryParser.QueryContext queryCtx) {
        for (var expr : queryCtx.expression()) {
            String fieldPath = extractFieldPath(expr);
            if (!FieldResolver.isKnownField(fieldPath)) {
                throw new ClientQueryException("Unknown query field: " + fieldPath);
            }

            if (expr.value() instanceof ClientQueryParser.ListValueContext listValue) {
                validateListConsistency(listValue.list());
            }
        }
    }

    private static void validateListConsistency(ClientQueryParser.ListContext listCtx) {
        List<String> entries = listCtx.listEntry().stream()
                .map(e -> e.LIST_ENTRY().getText())
                .toList();

        boolean allHaveColon = entries.stream().allMatch(e -> e.contains(":"));
        boolean noneHaveColon = entries.stream().noneMatch(e -> e.contains(":"));
        if (!allHaveColon && !noneHaveColon) {
            throw new ClientQueryException("Cannot mix list items and map entries in a single list expression");
        }
    }

    public static ClientQueryParser.QueryContext parse(String query) {
        if (query == null || query.isBlank()) {
            throw new ClientQueryException("Query expression cannot be null or empty");
        }

        CharStream charStream = CharStreams.fromString(query);
        ClientQueryLexer lexer = new ClientQueryLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ClientQueryParser parser = new ClientQueryParser(tokens);

        QueryErrorListener errorListener = new QueryErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        ClientQueryParser.QueryContext context = parser.query();

        if (errorListener.hasErrors()) {
            String errors = String.join(", ", errorListener.getErrorMessages());
            throw new ClientQueryException("Invalid query syntax: " + errors);
        }

        return context;
    }
}
