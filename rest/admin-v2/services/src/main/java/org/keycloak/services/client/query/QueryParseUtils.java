package org.keycloak.services.client.query;

import java.util.Set;

import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterException;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.services.client.scim.BaseClientModelSchema;

public class QueryParseUtils {

    private static final Set<String> SUPPORTED_OPERATORS = Set.of("eq", "ne", "co", "sw", "ew");

    public static ScimFilterParser.FilterContext parse(String query) {
        if (query == null || query.isBlank()) {
            throw new ClientQueryException("Query expression cannot be null or empty");
        }
        try {
            return FilterUtils.parseFilter(query);
        } catch (ScimFilterException e) {
            throw new ClientQueryException(e.getMessage());
        }
    }

    public static void validate(ScimFilterParser.FilterContext filterCtx) {
        validateExpression(filterCtx.expression());
    }

    private static void validateExpression(ScimFilterParser.ExpressionContext ctx) {
        if (ctx.OR() != null) {
            validateExpression(ctx.expression());
        }
        validateAndExpression(ctx.andExpression());
    }

    private static void validateAndExpression(ScimFilterParser.AndExpressionContext ctx) {
        if (ctx.AND() != null) {
            validateAndExpression(ctx.andExpression());
        }
        validateNotExpression(ctx.notExpression());
    }

    private static void validateNotExpression(ScimFilterParser.NotExpressionContext ctx) {
        if (ctx.NOT() != null) {
            validateNotExpression(ctx.notExpression());
        } else {
            validateAtom(ctx.atom());
        }
    }

    private static void validateAtom(ScimFilterParser.AtomContext ctx) {
        if (ctx.valuePath() != null) {
            throw new ClientQueryException("Value path expressions are not supported");
        }
        if (ctx.expression() != null) {
            validateExpression(ctx.expression());
        }
        if (ctx.attributeExpression() != null) {
            validateAttributeExpression(ctx.attributeExpression());
        }
    }

    private static void validateAttributeExpression(ScimFilterParser.AttributeExpressionContext ctx) {
        if (ctx instanceof ScimFilterParser.ComparisonExpressionContext comp) {
            String fieldPath = comp.ATTRPATH().getText();
            String operator = comp.compareOp().getText().toLowerCase();
            validateField(fieldPath, operator);
        } else if (ctx instanceof ScimFilterParser.PresentExpressionContext pr) {
            String fieldPath = pr.ATTRPATH().getText();
            validateField(fieldPath, null);
        }
    }
    
    /**
     * TODO: eventually this could be proper Attribute metadata, in which case it
     * should be handled in ScimJPAPredicateProvider
     * 
     * Alternatively the type provider class via a mechanism like ScimAttributeJpaExpressionResolver
     * could provide additional attribute checks so that the QueryParseUtils class could be
     * removed
     */
    public static void validateField(String fieldPath, String operator) {
        if (!FieldResolver.isKnownField(fieldPath)) {
            throw new ClientQueryException("Unknown query field: " + fieldPath);
        }
        if (operator != null && !SUPPORTED_OPERATORS.contains(operator)) {
            throw new ClientQueryException("Unsupported operator: " + operator
                    + ". Supported operators: " + SUPPORTED_OPERATORS);
        }
        if (!BaseClientModelSchema.QUERYABLE_FIELDS.contains(fieldPath)) {
            throw new ClientQueryException("Unsearchable query field: " + fieldPath);
        }
    }
}
