package org.keycloak.scim.resource.schema.path;

import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.filter.ScimFilterParserBaseVisitor;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

class ScimFilterToJsonNodeConverter extends ScimFilterParserBaseVisitor<JsonNode> {

    private final Attribute<?, ?> attribute;

    public ScimFilterToJsonNodeConverter(Attribute<?, ?> attribute) {
        this.attribute = attribute;
    }

    @Override
    public JsonNode visitFilter(ScimFilterParser.FilterContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public JsonNode visitExpression(ScimFilterParser.ExpressionContext ctx) {
        if (ctx.OR() != null) {
            if (attribute.getComplexType() == null && !attribute.isMultivalued()) {
                // OR is valid for multivalued attributes, so this is narrower than the AND guard.
                // For single-valued non-complex attributes visitComparisonExpression returns null,
                // which flattenIntoArray would dereference, surfacing as a 500 instead of a 400.
                throw new ModelValidationException(
                        "'or' operator is not supported for single-valued non-complex attributes");
            }
            JsonNode left = visit(ctx.expression());
            JsonNode right = visit(ctx.andExpression());
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            flattenIntoArray(array, left);
            flattenIntoArray(array, right);
            return array;
        }
        return visit(ctx.andExpression());
    }

    @Override
    public JsonNode visitAndExpression(ScimFilterParser.AndExpressionContext ctx) {
        if (ctx.AND() != null) {
            if (attribute.isMultivalued() || attribute.getComplexType() == null) {
                // AND is not supported for filtering multivalued or non-complex attributes because:
                // - For scalar multivalued: one value cannot satisfy two conditions simultaneously.
                // - For complex multivalued: even with multiple sub-attributes, filtering is
                //   restricted to 'value' only, so AND has no valid interpretation.
                // ModelValidationException so unsupported client input maps to a 400 rather
                // than surfacing as a 500 from Error.toResponse.
                throw new ModelValidationException(
                        "'and' operator is not supported for multivalued or non-complex attributes");
            }

            JsonNode left = visit(ctx.andExpression());
            JsonNode right = visit(ctx.notExpression());
            ObjectNode merged = JsonNodeFactory.instance.objectNode();
            mergeFields(merged, left);
            mergeFields(merged, right);
            return merged;
        }
        return visit(ctx.notExpression());
    }

    @Override
    public JsonNode visitNotExpression(ScimFilterParser.NotExpressionContext ctx) {
        if (ctx.NOT() != null) {
            throw new ModelValidationException("NOT operator is not supported when converting a SCIM filter to a JSON value");
        }
        return visit(ctx.atom());
    }

    @Override
    public JsonNode visitAtom(ScimFilterParser.AtomContext ctx) {
        if (ctx.valuePath() != null) {
            return visit(ctx.valuePath());
        }
        if (ctx.attributeExpression() != null) {
            return visit(ctx.attributeExpression());
        }
        return visit(ctx.expression());
    }

    @Override
    public JsonNode visitValuePath(ScimFilterParser.ValuePathContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public JsonNode visitPresentExpression(ScimFilterParser.PresentExpressionContext ctx) {
        throw new ModelValidationException("Present (pr) operator is not supported when converting a SCIM filter to a JSON value");
    }

    @Override
    public JsonNode visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
        String operator = ctx.compareOp().getText().toLowerCase();

        if (!"eq".equals(operator)) {
            throw new ModelValidationException("Only 'eq' operator is supported when converting a SCIM filter to a JSON value, got: " + operator);
        }

        Class<?> complexType = attribute.getComplexType();
        String attrName = ctx.ATTRPATH().getText();

        if (complexType == null) {
            if (!attribute.isMultivalued()) {
                return null;
            }

            // multivalued attributes without a complex type only expose the "value" sub-attribute.
            // SCIM attribute names are case-insensitive, so canonicalize it to the lower case name
            // that AttributeMapper unwraps.
            if (!"value".equalsIgnoreCase(attrName)) {
                throw new ModelValidationException("Unknown attribute " + attrName);
            }

            attrName = "value";
        } else {
            // For complex multivalued attributes, only filtering on the 'value' sub-attribute
            // (the unique identifier) is supported by the model layer. Other sub-attributes
            // like 'display', 'type', 'primary', etc. cannot be used for filtering.
            if (!"value".equalsIgnoreCase(attrName)) {
                throw new ModelValidationException("Only 'value' sub-attribute is supported for filtering complex multivalued attributes, got: " + attrName);
            }
            if (!isComplexTypeAttribute(complexType, attrName)) {
                throw new ModelValidationException("Unknown attribute " + attrName);
            }
            attrName = "value";
        }

        String value = extractValue(ctx.compValue());
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        if (value == null) {
            node.putNull(attrName);
        } else {
            node.put(attrName, value);
        }

        return node;
    }

    private static boolean isComplexTypeAttribute(Class<?> complexType, String attrName) {
        JavaType javaType = JsonSerialization.mapper.getTypeFactory().constructType(complexType);
        SerializationConfig serializationConfig = JsonSerialization.mapper.getSerializationConfig();
        return serializationConfig.introspect(javaType).findProperties().stream().anyMatch(p -> p.getName().equals(attrName));
    }

    private String extractValue(ScimFilterParser.CompValueContext ctx) {
        return FilterUtils.extractCompValue(ctx);
    }

    private void flattenIntoArray(ArrayNode array, JsonNode node) {
        if (node.isArray()) {
            node.forEach(array::add);
        } else {
            array.add(node);
        }
    }

    private void mergeFields(ObjectNode target, JsonNode source) {
        if (source.isObject()) {
            source.properties().forEach(e -> target.set(e.getKey(), e.getValue()));
        }
    }
}
