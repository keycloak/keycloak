package org.keycloak.services.client.query;

import java.util.LinkedHashSet;
import java.util.Set;

import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.filter.ScimFilterParserBaseVisitor;

public final class QueryFieldExtractor extends ScimFilterParserBaseVisitor<Void> {

    private final Set<String> fields = new LinkedHashSet<>();

    private QueryFieldExtractor() {
    }

    public static Set<String> extractFields(ScimFilterParser.FilterContext filterContext) {
        QueryFieldExtractor extractor = new QueryFieldExtractor();
        extractor.visit(filterContext);
        return Set.copyOf(extractor.fields);
    }

    @Override
    public Void visitFilter(ScimFilterParser.FilterContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Void visitExpression(ScimFilterParser.ExpressionContext ctx) {
        if (ctx.OR() != null) {
            visit(ctx.expression());
        }
        return visit(ctx.andExpression());
    }

    @Override
    public Void visitAndExpression(ScimFilterParser.AndExpressionContext ctx) {
        if (ctx.AND() != null) {
            visit(ctx.andExpression());
        }
        return visit(ctx.notExpression());
    }

    @Override
    public Void visitNotExpression(ScimFilterParser.NotExpressionContext ctx) {
        if (ctx.NOT() != null) {
            return visit(ctx.notExpression());
        }
        return visit(ctx.atom());
    }

    @Override
    public Void visitAtom(ScimFilterParser.AtomContext ctx) {
        if (ctx.attributeExpression() != null) {
            return visit(ctx.attributeExpression());
        }
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        return null;
    }

    @Override
    public Void visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
        fields.add(ctx.ATTRPATH().getText());
        return null;
    }

    @Override
    public Void visitPresentExpression(ScimFilterParser.PresentExpressionContext ctx) {
        fields.add(ctx.ATTRPATH().getText());
        return null;
    }
}
