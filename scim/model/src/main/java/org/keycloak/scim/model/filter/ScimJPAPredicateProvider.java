package org.keycloak.scim.model.filter;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.keycloak.models.KeycloakSession;
import org.keycloak.scim.filter.ScimFilterException;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.utils.KeycloakSessionUtil;

/**
 * Creates JPA predicates for SCIM filter operators. Handles both direct root entity fields and custom attributes stored
 * in an associated "attributes" collection. Also handles necessary type conversions for temporal fields.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ScimJPAPredicateProvider {

    private final KeycloakSession session;
    private final List<ModelSchema<?, ?>> schemas;
    private final CriteriaBuilder cb;
    private final CriteriaQuery<?> query;
    private final Root<?> root;

    // Cache joins to avoid creating duplicate joins for the same filter
    private Join<?, ?> attributeJoin;

    public ScimJPAPredicateProvider(KeycloakSession session, List<ModelSchema<?, ?>> schemas, CriteriaBuilder cb, CriteriaQuery<?> query, Root<?> root) {
        this.session = session;
        this.schemas = schemas;
        this.cb = cb;
        this.query = query;
        this.root = root;
    }

    public JPAFilterResult createPresentPredicate(String path) {
        Attribute attrInfo = resolve(path);
        if (attrInfo == null) {
            return JPAFilterResult.unsupported(cb.disjunction());
        }
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        String modelAttributeName = attrInfo.getModelAttributeName(session);
        if (attrInfo.isPrimary()) {
            // Direct field: check not null
            return JPAFilterResult.valid(cb.isNotNull(root.get(modelAttributeName)));
        } else {
            // Custom attribute: must exist in attributes collection with non-null value
            Join<?, ?> join = getOrCreateAttributeJoin();
            return JPAFilterResult.valid(cb.and(
                cb.equal(join.get("name"), modelAttributeName),
                cb.isNotNull(join.get("value"))
            ));
        }
    }

    public JPAFilterResult createComparisonPredicate(String path, String operator, String value) {
        Attribute attrInfo = resolve(path);
        if (attrInfo == null) {
            return JPAFilterResult.unsupported(cb.disjunction());
        }
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        // Determine if this is a temporal (timestamp) field that needs date conversion
        boolean isTimestampField = attrInfo.isTimestamp();
        boolean isBooleanField = attrInfo.isBoolean();
        String modelAttributeName = attrInfo.getModelAttributeName(session);

        switch (operator.toLowerCase()) {
            case "eq":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.equal(root.get(modelAttributeName), timestamp));
                } else if (isBooleanField) {
                    Boolean boolValue = Boolean.parseBoolean(value);
                    return JPAFilterResult.valid(cb.equal(root.get(modelAttributeName), boolValue));
                } else {
                    Expression<String> attrExpr = getAttributeExpression(session, modelAttributeName, attrInfo);
                    return JPAFilterResult.valid(cb.equal(attrExpr, value));
                }
            case "ne":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.notEqual(root.get(modelAttributeName), timestamp));
                } else if (isBooleanField) {
                    Boolean boolValue = Boolean.parseBoolean(value);
                    return JPAFilterResult.valid(cb.notEqual(root.get(modelAttributeName), boolValue));
                } else {
                    Expression<String> attrExpr = getAttributeExpression(session, modelAttributeName, attrInfo);
                    return JPAFilterResult.valid(cb.notEqual(attrExpr, value));
                }
            case "co":
                return JPAFilterResult.valid(cb.like(getAttributeExpression(session, modelAttributeName, attrInfo), "%" + escapeLike(value) + "%", '\\'));
            case "sw":
                return JPAFilterResult.valid(cb.like(getAttributeExpression(session, modelAttributeName, attrInfo), escapeLike(value) + "%", '\\'));
            case "ew":
                return JPAFilterResult.valid(cb.like(getAttributeExpression(session, modelAttributeName, attrInfo), "%" + escapeLike(value), '\\'));
            case "gt":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.greaterThan(root.get(modelAttributeName), timestamp));
                } else {
                    return JPAFilterResult.valid(cb.greaterThan(getAttributeExpression(session, modelAttributeName, attrInfo), value));
                }
            case "ge":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.greaterThanOrEqualTo(root.get(modelAttributeName), timestamp));
                } else {
                    return JPAFilterResult.valid(cb.greaterThanOrEqualTo(getAttributeExpression(session, modelAttributeName, attrInfo), value));
                }
            case "lt":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.lessThan(root.get(modelAttributeName), timestamp));
                } else {
                    return JPAFilterResult.valid(cb.lessThan(getAttributeExpression(session, modelAttributeName, attrInfo), value));
                }
            case "le":
                if (isTimestampField) {
                    Long timestamp = parseDateTime(value);
                    return JPAFilterResult.valid(cb.lessThanOrEqualTo(root.get(modelAttributeName), timestamp));
                } else {
                    return JPAFilterResult.valid(cb.lessThanOrEqualTo(getAttributeExpression(session, modelAttributeName, attrInfo), value));
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

    private Expression<String> getAttributeExpression(KeycloakSession session, String modelAttributeName, Attribute attrInfo) {
        if (attrInfo.isPrimary()) {
            return root.get(modelAttributeName);
        } else {
            Join<?, ?> join = getOrCreateAttributeJoin();
            // Add name filter to join
            query.where(cb.equal(join.get("name"), modelAttributeName));
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

    public Attribute<?, ?> resolve(String path) {
        Attribute<?, ?> metadata = null;

        for (ModelSchema<?, ?> schema : schemas) {
            metadata = schema.resolveAttribute(path);

            if (metadata != null    ) {
                break;
            }

        }
        if (metadata == null) {
            return null;
        }

        String modelAttributeName = metadata.getModelAttributeName(session);

        if (modelAttributeName != null) {
            return metadata;
        }

        // haven't found the attribute in the user profile, so return null to indicate that this is an unknown attribute.
        return null;
    }
}
