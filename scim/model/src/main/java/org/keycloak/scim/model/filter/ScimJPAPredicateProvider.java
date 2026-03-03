package org.keycloak.scim.model.filter;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.common.util.TriFunction;
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
    private final Root<?> root;

    @SuppressWarnings("rawtypes,unchecked")
    private final Map<String, TriFunction<CriteriaBuilder, Expression, Object, Predicate>> operatorMap = Map.of(
            "eq", CriteriaBuilder::equal,
            "ne", CriteriaBuilder::notEqual,
            "gt", (cb, exp, val) -> cb.greaterThan(exp, asComparable(val)),
            "ge", (cb, exp, val) -> cb.greaterThanOrEqualTo(exp, asComparable(val)),
            "lt", (cb, exp, val) -> cb.lessThan(exp, asComparable(val)),
            "le", (cb, exp, val) -> cb.lessThanOrEqualTo(exp, asComparable(val)),
            "co", (cb, exp, val) -> cb.like(exp.as(String.class), "%" + escapeLike(val.toString()) + "%", '\\'),
            "sw", (cb, exp, val) -> cb.like(exp.as(String.class), escapeLike(val.toString()) + "%", '\\'),
            "ew", (cb, exp, val) -> cb.like(exp.as(String.class), "%" + escapeLike(val.toString()), '\\')
    );

    // cache joins to avoid creating duplicate joins for the same filter
    private Join<?, ?> attributeJoin;

    public ScimJPAPredicateProvider(KeycloakSession session, List<ModelSchema<?, ?>> schemas, CriteriaBuilder cb, Root<?> root) {
        this.session = session;
        this.schemas = schemas;
        this.cb = cb;
        this.root = root;
    }

    /**
     * Create a predicate for "presence" operator (pr). For direct fields, this checks that the field is not null. For custom attributes,
     * this checks that there is an entry in the attributes collection with the given name and a non-null value.
     *
     * @param path the SCIM attribute path to check for presence
     * @return a {@link JPAFilterResult} containing the presence predicate if the attribute is known and mapped, or an unsupported
     * result if the attribute is unknown
     */
    public JPAFilterResult createPresentPredicate(String path) {
        Attribute<?,?> attrInfo = resolve(path);
        if (attrInfo == null) {
            return JPAFilterResult.unsupported(cb.disjunction());
        }
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        String modelAttributeName = attrInfo.getModelAttributeName(session);
        if (attrInfo.isPrimary()) {
            // direct field: check not null
            return JPAFilterResult.valid(cb.isNotNull(root.get(modelAttributeName)));
        } else {
            // custom attribute: must exist in attributes collection with non-null value
            Join<?, ?> join = getOrCreateAttributeJoin();
            return JPAFilterResult.valid(cb.and(
                cb.equal(join.get("name"), modelAttributeName),
                cb.isNotNull(join.get("value"))
            ));
        }
    }

    /**
     * Create a predicate for comparison operators (eq, ne, gt, ge, lt, le, co, sw, ew). This method first resolves the SCIM
     * attribute path to get the corresponding metadata, then validates that the operator is supported for the attribute type,
     * normalizes the value to the correct type, and finally builds the appropriate predicate based on whether the attribute
     * is a direct field or a custom attribute.
     *
     * @param path the SCIM attribute path to compare
     * @param operator the comparison operator (eq, ne, gt, ge, lt, le, co, sw, ew)
     * @param value the value to compare against, as a string (will be normalized to the correct type based on the attribute metadata)
     * @return a {@link JPAFilterResult} containing the comparison predicate if the attribute is known and mapped, or an unsupported
     * result if the attribute is unknown
     */
    public JPAFilterResult createComparisonPredicate(String path, String operator, String value) {
        Attribute<?,?> attrInfo = resolve(path);
        if (attrInfo == null) return JPAFilterResult.unsupported(cb.disjunction());

        String op = operator.toLowerCase();
        // validate operator before normalization or predicate building
        validateOperator(attrInfo, path, op);

        // normalize the value (String -> Long, Boolean, etc.)
        Object normalizedValue = normalizeValue(attrInfo, value);

        // build the predicate
        return JPAFilterResult.valid(getAttributePredicate(attrInfo, op, normalizedValue));
    }

    /**
     * Normalize the string value from the filter expression to the correct type based on the attribute metadata. For timestamp attributes,
     * this converts the ISO 8601 date/time string to a Long timestamp. For boolean attributes, this converts "true"/"false" strings to Boolean.
     * For other types, it returns the original string value (no normalization needed).
     *
     * @param attrInfo the attribute metadata to determine the type for normalization
     * @param value the original string value from the filter expression
     * @return the normalized value, converted to the appropriate type based on the attribute metadata, or the original string if no normalization is needed
     */
    private Object normalizeValue(Attribute<?,?> attrInfo, String value) {
        if (value == null) return null;
        if (attrInfo.isTimestamp()) return parseDateTime(value);
        if (attrInfo.isBoolean()) return parseBoolean(value);
        return value;
    }

    /**
     * Build a JPA predicate for the given attribute, operator, and value. This method handles both direct fields (primary attributes) and custom attributes
     * stored in the "attributes" collection. For direct fields, it applies the operator directly to the root entity field. For custom attributes, it creates a join
     * to the "attributes" collection, adds a condition to match the attribute name, and then applies the operator to the "value" field of the joined entity.
     *
     * @param attrInfo the attribute metadata to determine how to build the predicate
     * @param operation the comparison operator (eq, ne, gt, ge, lt, le, co, sw, ew)
     * @param value the value to compare against, already normalized to the correct type
     * @return the JPA {@link Predicate} representing the comparison for the given attribute, operator, and value
     */
    private Predicate getAttributePredicate(Attribute<?,?> attrInfo, String operation, Object value) {
        Expression<?> path;
        Predicate basePredicate = null;
        String modelAttributeName = attrInfo.getModelAttributeName(session);

        if (attrInfo.isPrimary()) {
            path = root.get(modelAttributeName);
        } else {
            Join<?, ?> join = getOrCreateAttributeJoin();
            path = join.get("value");
            basePredicate = cb.equal(join.get("name"), modelAttributeName);
        }

        Predicate comparison = operatorMap.get(operation).apply(cb, path, value);
        return (basePredicate != null) ? cb.and(basePredicate, comparison) : comparison;
    }

    /**
     * Helper method to get or create a join to the "attributes" collection. This method checks if the join has already been created
     * and cached in the {@code attributeJoin} field.
     *
     * @return the existing or newly created join to the "attributes" collection
     */
    private Join<?, ?> getOrCreateAttributeJoin() {
        if (attributeJoin == null) {
            attributeJoin = root.join("attributes", JoinType.LEFT);
        }
        return attributeJoin;
    }

    /**
     * Validate that the operator is supported for the attribute type. For example, boolean attributes only support "eq" and "ne",
     * while timestamp attributes do not support string-specific operators like "co", "sw", or "ew". If the operator is not valid for the attribute type,
     * this method throws a {@link ScimFilterException} with a descriptive error message.
     *
     * @param attrInfo the attribute metadata to validate against
     * @param scimAttribute the original SCIM attribute path (used for error messages)
     * @param operator the operator to validate
     * @throws ScimFilterException if the operator is not supported for the attribute type
     */
    private void validateOperator(Attribute<?,?> attrInfo, String scimAttribute, String operator) {
        String op = operator.toLowerCase();

        // boolean validation: only allows equality
        if (attrInfo.isBoolean()) {
            if (!op.equals("eq") && !op.equals("ne")) {
                throw new ScimFilterException(
                        "Operator '" + operator + "' is not supported for boolean attribute: " + scimAttribute);
            }
        }

        // timestamp/numeric validation: block string-specific operators
        if (attrInfo.isTimestamp()) {
            if (op.equals("co") || op.equals("sw") || op.equals("ew")) {
                throw new ScimFilterException(
                        "String operators (co, sw, ew) are not supported for timestamp attribute: " + scimAttribute);
            }
        }
    }

    /**
     * Parse ISO 8601 date/time string to {@link Long} timestamp (milliseconds since epoch). SCIM uses ISO 8601 format
     * (e.g., "2011-05-13T04:42:34Z") while Keycloak stores timestamps as Long (milliseconds).
     *
     * @param dateTimeString the date/time string to parse
     * @return the parsed timestamp as {@link Long}
     * @throws ScimFilterException if the input string is not a valid ISO 8601 date/time format or a valid numeric timestamp
     */
    private Long parseDateTime(String dateTimeString) {
        try {
            Instant instant = Instant.parse(dateTimeString);
            return instant.toEpochMilli();
        } catch (DateTimeParseException e) {
            // If not a valid ISO 8601 date, try parsing as number (might be a timestamp already)
            try {
                return Long.parseLong(dateTimeString);
            } catch (NumberFormatException nfe) {
                throw new ScimFilterException(
                        "Invalid date/time format: " + dateTimeString +
                                ". Expected ISO 8601 format (e.g., 2011-05-13T04:42:34Z) or timestamp");
            }
        }
    }

    /**
     * Parse boolean string ("true"/"false") to {@link Boolean}. This method also validates that the value is a valid boolean string.
     *
     * @param value the string to parse as boolean
     * @return the parsed {@link Boolean} value
     * @throws ScimFilterException if the value is not a valid boolean string
     */
    private Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        throw new ScimFilterException("Invalid boolean value found in boolean expression: " + value);
    }

    /**
     * Helper method to cast an object to {@link Comparable}. This is used for comparison operators (gt, ge, lt, le) which
     * require the value to be comparable.
     *
      * @param val the object to cast
     * @return the object cast to {@link Comparable}
     * @throws ScimFilterException if the object is not an instance of {@link Comparable}
     */
    @SuppressWarnings("unchecked")
    private Comparable<Object> asComparable(Object val) {
        if (val instanceof Comparable) {
            return (Comparable<Object>) val;
        }
        throw new ScimFilterException("Value is not comparable: " + val);
    }

    /**
     * Resolve the SCIM attribute path to the corresponding {@link Attribute} metadata. This method checks all registered schemas
     * to find the attribute. If the attribute is found but does not have a model attribute name (i.e., it is not mapped to a model field),
     * it returns {@code null} to indicate that this is an unknown attribute for filtering purposes. If the attribute is not found in any schema,
     * it also returns {@code null}.
     *
     * @param path the SCIM attribute path to resolve
     * @return the corresponding {@link Attribute} metadata if found and mapped to a model field, or {@code null} if not found or not mapped
     */
    public Attribute<?, ?> resolve(String path) {
        Attribute<?, ?> metadata = null;

        for (ModelSchema<?, ?> schema : schemas) {
            metadata = schema.resolveAttribute(path);
            if (metadata != null    ) {
                break;
            }
        }
        if (metadata != null) {
            String modelAttributeName = metadata.getModelAttributeName(session);
            if (modelAttributeName != null) {
                return metadata;
            }
        }
        // haven't found the attribute - return null to indicate that this is an unknown attribute.
        return null;
    }

    /**
     * Escape special characters in a string for use in SQL LIKE expressions. This method escapes the backslash, percent,
     * and underscore characters, which are special in SQL LIKE patterns.
     *
     * @param value the string value to escape
     * @return the escaped string, safe for use in SQL LIKE expressions
     */
    private String escapeLike(String value) {
        // Escape SQL LIKE special characters
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
