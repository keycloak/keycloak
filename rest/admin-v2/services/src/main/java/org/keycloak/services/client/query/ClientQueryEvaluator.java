package org.keycloak.services.client.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;

public class ClientQueryEvaluator extends ClientQueryParserBaseVisitor<Boolean> {

    private final BaseClientRepresentation client;

    private ClientQueryEvaluator(BaseClientRepresentation client) {
        this.client = client;
    }

    public static boolean matches(ClientQueryParser.QueryContext query, BaseClientRepresentation client) {
        return new ClientQueryEvaluator(client).visit(query);
    }

    @Override
    public Boolean visitQuery(ClientQueryParser.QueryContext ctx) {
        return ctx.expression().stream().allMatch(this::visitExpression);
    }

    @Override
    public Boolean visitExpression(ClientQueryParser.ExpressionContext ctx) {
        String fieldPath = QueryParseUtils.extractFieldPath(ctx);

        Object fieldValue = FieldResolver.resolve(fieldPath, client);
        if (fieldValue == null) {
            return false;
        }

        return matchValue(fieldValue, ctx.value());
    }

    private boolean matchValue(Object fieldValue, ClientQueryParser.ValueContext valueCtx) {
        if (valueCtx instanceof ClientQueryParser.BareValueContext bare) {
            return matchScalar(fieldValue, bare.BAREWORD().getText());
        } else if (valueCtx instanceof ClientQueryParser.QuotedValueContext quoted) {
            String text = quoted.QUOTED_STRING().getText();
            return matchScalar(fieldValue, text.substring(1, text.length() - 1));
        } else if (valueCtx instanceof ClientQueryParser.ListValueContext listCtx) {
            return matchList(fieldValue, listCtx.list());
        }
        return false;
    }

    private boolean matchScalar(Object fieldValue, String queryValue) {
        if (fieldValue instanceof Collection<?>) {
            return ((Collection<?>) fieldValue).stream()
                    .anyMatch(item -> Objects.equals(item.toString(), queryValue));
        }
        return Objects.equals(fieldValue.toString(), queryValue);
    }

    private boolean matchList(Object fieldValue, ClientQueryParser.ListContext listCtx) {
        List<String> entries = listCtx.listEntry().stream()
                .map(e -> e.LIST_ENTRY().getText())
                .toList();

        boolean mapMode = entries.get(0).contains(":");

        if (fieldValue instanceof Map<?, ?> map) {
            if (mapMode) {
                return entries.stream().allMatch(entry -> {
                    int colonIdx = entry.indexOf(':');
                    String key = entry.substring(0, colonIdx);
                    String value = entry.substring(colonIdx + 1);
                    return Objects.equals(Objects.toString(map.get(key), null), value);
                });
            } else {
                return entries.stream().allMatch(entry -> map.containsKey(entry));
            }
        }

        if (fieldValue instanceof Collection<?> collection) {
            var stringValues = collection.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
            return entries.stream().allMatch(stringValues::contains);
        }

        return false;
    }
}
