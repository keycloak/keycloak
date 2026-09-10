package org.keycloak.scim.model.filter;

import java.util.List;
import java.util.function.BiPredicate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;

import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.filter.ScimFilterParserBaseVisitor;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;

/**
 * Visitor that converts an SCIM filter AST into a JPA Predicate.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ScimJPAPredicateEvaluator extends ScimFilterParserBaseVisitor<JPAFilterResult> {

    private final CriteriaBuilder cb;
    private final ScimJPAPredicateProvider predicateProvider;
    private String parentPath;

    @SuppressWarnings("unchecked,rawtypes")
    public ScimJPAPredicateEvaluator(ScimResourceTypeProvider resourceTypeProvider, List schemas, CriteriaBuilder cb, Root<?> root) {
        this(resourceTypeProvider, schemas, cb, root, null);
    }

    @SuppressWarnings("unchecked,rawtypes")
    public ScimJPAPredicateEvaluator(ScimResourceTypeProvider resourceTypeProvider, List schemas, CriteriaBuilder cb, Root<?> root,
                                      BiPredicate<String, String> filterAuthorizationCheck) {
        this.cb = cb;
        this.predicateProvider = new ScimJPAPredicateProvider(resourceTypeProvider, schemas, cb, root, filterAuthorizationCheck);
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

            boolean prot = left.authzProtected() || right.authzProtected();
            return JPAFilterResult.valid(cb.or(left.predicate(), right.predicate()), prot);
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
            boolean prot = left.authzProtected() || right.authzProtected();
            return JPAFilterResult.valid(cb.and(left.predicate(), right.predicate()), prot);
        }
        return visit(ctx.notExpression());
    }

    @Override
    public JPAFilterResult visitNotExpression(ScimFilterParser.NotExpressionContext ctx) {
        if (ctx.NOT() != null) {
            JPAFilterResult child = visit(ctx.notExpression());
            if (child.unsupported()) {
                return child;
            }
            // negating an authz-protected predicate would invert it into a membership oracle
            if (child.authzProtected()) {
                return JPAFilterResult.unsupported(cb.disjunction());
            }
            return JPAFilterResult.valid(cb.not(child.predicate()));
        }
        return visit(ctx.atom());
    }

    @Override
    public JPAFilterResult visitAtom(ScimFilterParser.AtomContext ctx) {
        if (ctx.valuePath() != null) {
            return visit(ctx.valuePath());
        }
        if (ctx.attributeExpression() != null) {
            return visit(ctx.attributeExpression());
        }
        return visit(ctx.expression());
    }

    @Override
    public JPAFilterResult visitValuePath(ScimFilterParser.ValuePathContext ctx) {
        String previousPath = parentPath;
        parentPath = resolveAttrPath(ctx.ATTRPATH().getText());
        try {
            return visit(ctx.expression());
        } finally {
            parentPath = previousPath;
        }
    }

    @Override
    public JPAFilterResult visitPresentExpression(ScimFilterParser.PresentExpressionContext ctx) {
        String scimAttrPath = resolveAttrPath(ctx.ATTRPATH().getText());
        String operator = ctx.PR().getText().toLowerCase();
        return predicateProvider.createPredicate(scimAttrPath, operator, null);
    }

    @Override
    public JPAFilterResult visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
        String scimAttrPath = resolveAttrPath(ctx.ATTRPATH().getText());
        String operator = ctx.compareOp().getText().toLowerCase();
        String value = FilterUtils.extractCompValue(ctx.compValue());

        return predicateProvider.createPredicate(scimAttrPath, operator, value);
    }

    private String resolveAttrPath(String attrPath) {
        return parentPath != null ? parentPath + "." + attrPath : attrPath;
    }
}
