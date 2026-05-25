package org.keycloak.services.client.query;

import java.util.Collection;
import java.util.Objects;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.filter.ScimFilterParserBaseVisitor;

public class ClientQueryEvaluator extends ScimFilterParserBaseVisitor<Boolean> {

    private final BaseClientRepresentation client;

    private ClientQueryEvaluator(BaseClientRepresentation client) {
        this.client = client;
    }

    public static boolean matches(ScimFilterParser.FilterContext filter, BaseClientRepresentation client) {
        return new ClientQueryEvaluator(client).visit(filter);
    }

    @Override
    public Boolean visitFilter(ScimFilterParser.FilterContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Boolean visitExpression(ScimFilterParser.ExpressionContext ctx) {
        if (ctx.OR() != null) {
            return visit(ctx.expression()) || visit(ctx.andExpression());
        }
        return visit(ctx.andExpression());
    }

    @Override
    public Boolean visitAndExpression(ScimFilterParser.AndExpressionContext ctx) {
        if (ctx.AND() != null) {
            return visit(ctx.andExpression()) && visit(ctx.notExpression());
        }
        return visit(ctx.notExpression());
    }

    @Override
    public Boolean visitNotExpression(ScimFilterParser.NotExpressionContext ctx) {
        if (ctx.NOT() != null) {
            return !visit(ctx.notExpression());
        }
        return visit(ctx.atom());
    }

    @Override
    public Boolean visitAtom(ScimFilterParser.AtomContext ctx) {
        if (ctx.attributeExpression() != null) {
            return visit(ctx.attributeExpression());
        }
        return visit(ctx.expression());
    }

    @Override
    public Boolean visitPresentExpression(ScimFilterParser.PresentExpressionContext ctx) {
        String fieldPath = ctx.ATTRPATH().getText();
        return FieldResolver.resolve(fieldPath, client) != null;
    }

    @Override
    public Boolean visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
        String fieldPath = ctx.ATTRPATH().getText();
        Object fieldValue = FieldResolver.resolve(fieldPath, client);
        String operator = ctx.compareOp().getText().toLowerCase();
        String queryValue = FilterUtils.extractCompValue(ctx.compValue());

        return switch (operator) {
            case "eq" -> fieldValue == null ? queryValue == null : matchScalar(fieldValue, queryValue, Objects::equals);
            case "ne" -> fieldValue == null ? queryValue != null : !matchScalar(fieldValue, queryValue, Objects::equals);
            case "co" -> fieldValue != null && matchScalar(fieldValue, queryValue, (fv, qv) -> fv != null && fv.contains(qv));
            case "sw" -> fieldValue != null && matchScalar(fieldValue, queryValue, (fv, qv) -> fv != null && fv.startsWith(qv));
            case "ew" -> fieldValue != null && matchScalar(fieldValue, queryValue, (fv, qv) -> fv != null && fv.endsWith(qv));
            default -> false;
        };
    }

    private boolean matchScalar(Object fieldValue, String queryValue,
                                java.util.function.BiPredicate<String, String> predicate) {
        if (fieldValue instanceof Collection<?> collection) {
            return collection.stream()
                    .anyMatch(item -> predicate.test(item.toString(), queryValue));
        }
        return predicate.test(fieldValue.toString(), queryValue);
    }
}
