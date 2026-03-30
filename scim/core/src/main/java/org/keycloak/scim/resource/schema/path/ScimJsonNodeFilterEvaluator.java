package org.keycloak.scim.resource.schema.path;

import java.util.function.Predicate;

import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.filter.ScimFilterParserBaseVisitor;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Visitor that converts a SCIM filter AST into a {@link Predicate} over {@link JsonNode} elements.
 * <p>
 * This is used by {@link Path} to evaluate filter expressions (e.g., {@code value eq "some-id"})
 * against JSON array elements in-memory, supporting all SCIM comparison operators and logical
 * operators ({@code and}, {@code or}, {@code not}).
 */
class ScimJsonNodeFilterEvaluator extends ScimFilterParserBaseVisitor<Predicate<JsonNode>> {

    @Override
    public Predicate<JsonNode> visitFilter(ScimFilterParser.FilterContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Predicate<JsonNode> visitExpression(ScimFilterParser.ExpressionContext ctx) {
        if (ctx.OR() != null) {
            Predicate<JsonNode> left = visit(ctx.expression());
            Predicate<JsonNode> right = visit(ctx.andExpression());
            return left.or(right);
        }
        return visit(ctx.andExpression());
    }

    @Override
    public Predicate<JsonNode> visitAndExpression(ScimFilterParser.AndExpressionContext ctx) {
        if (ctx.AND() != null) {
            Predicate<JsonNode> left = visit(ctx.andExpression());
            Predicate<JsonNode> right = visit(ctx.notExpression());
            return left.and(right);
        }
        return visit(ctx.notExpression());
    }

    @Override
    public Predicate<JsonNode> visitNotExpression(ScimFilterParser.NotExpressionContext ctx) {
        if (ctx.NOT() != null) {
            Predicate<JsonNode> child = visit(ctx.notExpression());
            return child.negate();
        }
        return visit(ctx.atom());
    }

    @Override
    public Predicate<JsonNode> visitAtom(ScimFilterParser.AtomContext ctx) {
        if (ctx.valuePath() != null) {
            return visit(ctx.valuePath());
        }
        if (ctx.attributeExpression() != null) {
            return visit(ctx.attributeExpression());
        }
        return visit(ctx.expression());
    }

    @Override
    public Predicate<JsonNode> visitValuePath(ScimFilterParser.ValuePathContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Predicate<JsonNode> visitPresentExpression(ScimFilterParser.PresentExpressionContext ctx) {
        String attrName = ctx.ATTRPATH().getText();
        return node -> {
            if (!node.isObject()) return false;
            JsonNode value = node.get(attrName);
            return value != null && !value.isNull() && !value.isMissingNode();
        };
    }

    @Override
    public Predicate<JsonNode> visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
        String attrName = ctx.ATTRPATH().getText();
        String operator = ctx.compareOp().getText().toLowerCase();
        String compValue = extractValue(ctx.compValue());

        return node -> {
            if (!node.isObject()) return false;
            JsonNode attrNode = node.get(attrName);
            if (attrNode == null || attrNode.isNull()) {
                return "eq".equals(operator) && compValue == null;
            }
            return compare(attrNode.asText(), operator, compValue);
        };
    }

    private boolean compare(String nodeValue, String operator, String compValue) {
        if (compValue == null || nodeValue == null) {
            return false;
        }

        return switch (operator) {
            case "eq" -> nodeValue.equals(compValue);
            case "ne" -> !nodeValue.equals(compValue);
            case "co" -> nodeValue.contains(compValue);
            case "sw" -> nodeValue.startsWith(compValue);
            case "ew" -> nodeValue.endsWith(compValue);
            case "gt" -> nodeValue.compareTo(compValue) > 0;
            case "ge" -> nodeValue.compareTo(compValue) >= 0;
            case "lt" -> nodeValue.compareTo(compValue) < 0;
            case "le" -> nodeValue.compareTo(compValue) <= 0;
            default -> false;
        };
    }

    private String extractValue(ScimFilterParser.CompValueContext ctx) {
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

    private String unescapeJsonString(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
