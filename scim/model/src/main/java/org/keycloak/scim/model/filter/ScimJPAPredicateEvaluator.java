package org.keycloak.scim.model.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.filter.ScimFilterParserBaseVisitor;

/**
 * Visitor that converts an SCIM filter AST into a JPA Predicate.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ScimJPAPredicateEvaluator extends ScimFilterParserBaseVisitor<JPAFilterResult> {

    private final CriteriaBuilder cb;
    private final ScimJPAPredicateProvider predicateProvider;

    public ScimJPAPredicateEvaluator(AttributeNameResolver resolver, CriteriaBuilder cb, CriteriaQuery<?> query, Root<?> root) {
        this.cb = cb;
        this.predicateProvider = new ScimJPAPredicateProvider(resolver, cb, query, root);
    }

    @Override
    public JPAFilterResult visitFilter(ScimFilterParser.FilterContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public JPAFilterResult visitExpression(ScimFilterParser.ExpressionContext ctx) {
        if (ctx.OR() != null) {
            JPAFilterResult left = visit(ctx.expression());
            JPAFilterResult right = visit(ctx.andExpression());

            // Logical OR: If one side is unsupported (False), the result is just the other side
            if (left.unsupported()) return right;
            if (right.unsupported()) return left;

            return JPAFilterResult.valid(cb.or(left.predicate(), right.predicate()));
        }
        return visit(ctx.andExpression());
    }

    @Override
    public JPAFilterResult visitAndExpression(ScimFilterParser.AndExpressionContext ctx) {
        if (ctx.AND() != null) {
            JPAFilterResult left = visit(ctx.andExpression());
            JPAFilterResult right = visit(ctx.notExpression());

            // If either side is unsupported, the whole AND is unsupported (False)
            if (left.unsupported() || right.unsupported()) {
                return JPAFilterResult.unsupported(cb.disjunction());
            }
            return JPAFilterResult.valid(cb.and(left.predicate(), right.predicate()));
        }
        return visit(ctx.notExpression());
    }

    @Override
    public JPAFilterResult visitNotExpression(ScimFilterParser.NotExpressionContext ctx) {
        if (ctx.NOT() != null) {
            JPAFilterResult child = visit(ctx.notExpression());
            // If the child is a disjunction caused by an unsupported attribute, per RFC 7644, 'not (unknownAttr pr)' MUST still be an empty set.
            if (child.unsupported()) {
                return child;
            }
            return JPAFilterResult.valid(cb.not(child.predicate()));
        }
        return visit(ctx.atom());
    }

    @Override
    public JPAFilterResult visitAtom(ScimFilterParser.AtomContext ctx) {
        if (ctx.attributeExpression() != null) {
            return visit(ctx.attributeExpression());
        }
        return visit(ctx.expression());
    }

    @Override
    public JPAFilterResult visitPresentExpression(ScimFilterParser.PresentExpressionContext ctx) {
        String scimAttrPath = ctx.ATTRPATH().getText();
        return predicateProvider.createPresentPredicate(scimAttrPath);
    }

    @Override
    public JPAFilterResult visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
        String scimAttrPath = ctx.ATTRPATH().getText();
        String operator = ctx.compareOp().getText().toLowerCase();
        String value = extractValue(ctx.compValue());

        return predicateProvider.createComparisonPredicate(scimAttrPath, operator, value);
    }

    private String extractValue(ScimFilterParser.CompValueContext ctx) {
        if (ctx.STRING() != null) {
            // Remove quotes and unescape per JSON string rules
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
        // Note: Unicode escape sequences (backslash-u followed by 4 hex digits) are handled by ANTLR lexer
    }
}
