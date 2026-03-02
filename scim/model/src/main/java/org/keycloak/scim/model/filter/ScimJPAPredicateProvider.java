package org.keycloak.scim.model.filter;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.keycloak.scim.filter.ScimFilterException;

/**
 * Creates JPA predicates for SCIM filter operators. Handles both direct root entity fields and custom attributes stored
 * in an associated "attributes" collection. Also handles necessary type conversions for temporal fields.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ScimJPAPredicateProvider {

    private final CriteriaBuilder cb;
    private final CriteriaQuery<?> query;
    private final Root<?> root;
    private final AttributeNameResolver nameResolver;

    // Cache joins to avoid creating duplicate joins for the same filter
    private Join<?, ?> attributeJoin;

    public ScimJPAPredicateProvider(AttributeNameResolver resolver, CriteriaBuilder cb, CriteriaQuery<?> query, Root<?> root) {
        this.cb = cb;
        this.query = query;
        this.root = root;
        this.nameResolver = resolver;
    }

    public JPAFilterResult createPresentPredicate(String scimAttrPath) {
        AttributeInfo attrInfo = nameResolver.resolve(scimAttrPath);
        if (attrInfo == null) {
            return JPAFilterResult.unsupported(cb.disjunction());
        }

        if (attrInfo.isPrimary()) {
            // Direct field: check not null
            return JPAFilterResult.valid(cb.isNotNull(root.get(attrInfo.getKeycloakName())));
        } else {
            // Custom attribute: must exist in attributes collection with non-null value
            Join<?, ?> join = getOrCreateAttributeJoin();
            return JPAFilterResult.valid(cb.and(
                cb.equal(join.get("name"), attrInfo.getKeycloakName()),
                cb.isNotNull(join.get("value"))
            ));
        }
    }

    public JPAFilterResult createComparisonPredicate(String scimAttrPath, String operator, String value) {
        AttributeInfo attrInfo = nameResolver.resolve(scimAttrPath);
        if (attrInfo == null) {
            return JPAFilterResult.unsupported(cb.disjunction());
        }

        // Determine if this is a temporal (timestamp) field that needs date conversion
        boolean isTimestampField = attrInfo.isTimestamp();
        boolean isBooleanField = attrInfo.isBoolean();

        switch (operator.toLowerCase()) {
            case "eq":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.equal(root.get(attrInfo.getKeycloakName()), timestamp));
                } else if (isBooleanField) {
                    Boolean boolValue = Boolean.parseBoolean(value);
                    return JPAFilterResult.valid(cb.equal(root.get(attrInfo.getKeycloakName()), boolValue));
                } else {
                    Expression<String> attrExpr = getAttributeExpression(attrInfo);
                    return JPAFilterResult.valid(cb.equal(attrExpr, value));
                }
            case "ne":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.notEqual(root.get(attrInfo.getKeycloakName()), timestamp));
                } else if (isBooleanField) {
                    Boolean boolValue = Boolean.parseBoolean(value);
                    return JPAFilterResult.valid(cb.notEqual(root.get(attrInfo.getKeycloakName()), boolValue));
                } else {
                    Expression<String> attrExpr = getAttributeExpression(attrInfo);
                    return JPAFilterResult.valid(cb.notEqual(attrExpr, value));
                }
            case "co":
                return JPAFilterResult.valid(cb.like(getAttributeExpression(attrInfo), "%" + escapeLike(value) + "%", '\\'));
            case "sw":
                return JPAFilterResult.valid(cb.like(getAttributeExpression(attrInfo), escapeLike(value) + "%", '\\'));
            case "ew":
                return JPAFilterResult.valid(cb.like(getAttributeExpression(attrInfo), "%" + escapeLike(value), '\\'));
            case "gt":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.greaterThan(root.get(attrInfo.getKeycloakName()), timestamp));
                } else {
                    return JPAFilterResult.valid(cb.greaterThan(getAttributeExpression(attrInfo), value));
                }
            case "ge":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.greaterThanOrEqualTo(root.get(attrInfo.getKeycloakName()), timestamp));
                } else {
                    return JPAFilterResult.valid(cb.greaterThanOrEqualTo(getAttributeExpression(attrInfo), value));
                }
            case "lt":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.lessThan(root.get(attrInfo.getKeycloakName()), timestamp));
                } else {
                    return JPAFilterResult.valid(cb.lessThan(getAttributeExpression(attrInfo), value));
                }
            case "le":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.lessThanOrEqualTo(root.get(attrInfo.getKeycloakName()), timestamp));
                } else {
                    return JPAFilterResult.valid(cb.lessThanOrEqualTo(getAttributeExpression(attrInfo), value));
                }
            default:
                throw new ScimFilterException("Unknown operator: " + operator);
        }
    }

    /**
     * Parse ISO 8601 date/time string to Long timestamp (milliseconds since epoch).
     * SCIM uses ISO 8601 format (e.g., "2011-05-13T04:42:34Z") while Keycloak stores
     * timestamps as Long (milliseconds).
     */
    private Long parseDateTime(String dateTimeString) {
        try {
            Instant instant = Instant.parse(dateTimeString);
            return instant.toEpochMilli();
        } catch (DateTimeParseException e) {
            // If not a valid ISO 8601 date, try parsing as number (might be timestamp already)
            try {
                return Long.parseLong(dateTimeString);
            } catch (NumberFormatException nfe) {
                throw new ScimFilterException(
                    "Invalid date/time format: " + dateTimeString +
                    ". Expected ISO 8601 format (e.g., 2011-05-13T04:42:34Z) or timestamp");
            }
        }
    }

    private Expression<String> getAttributeExpression(AttributeInfo attrInfo) {
        if (attrInfo.isPrimary()) {
            return root.get(attrInfo.getKeycloakName());
        } else {
            Join<?, ?> join = getOrCreateAttributeJoin();
            // Add name filter to join
            query.where(cb.equal(join.get("name"), attrInfo.getKeycloakName()));
            return join.get("value");
        }
    }

    private Join<?, ?> getOrCreateAttributeJoin() {
        if (attributeJoin == null) {
            attributeJoin = root.join("attributes", JoinType.LEFT);
        }
        return attributeJoin;
    }

    private String escapeLike(String value) {
        // Escape SQL LIKE special characters
        return value.replace("\\", "\\\\")
                   .replace("%", "\\%")
                   .replace("_", "\\_");
    }
}
