package org.keycloak.scim.resource.schema.path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.filter.ScimFilterParserBaseVisitor;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;

public final class Path {

    // operators a value selection filter may use on a sub-attribute of each type (RFC 7644 section 3.4.2.2)
    private static final Set<String> STRING_OPERATORS = Set.of("eq", "ne", "co", "sw", "ew");
    private static final Set<String> BOOLEAN_OPERATORS = Set.of("eq", "ne");

    private final String path;
    private final String filter;

    public <R extends ResourceTypeRepresentation> Path(ModelSchema<?, ?> schema, String rawPath) {
        if (rawPath == null) {
            this.path = schema.getId();
            this.filter = null;
        } else {
            int filterStartIdx = rawPath.indexOf("[");

            if (filterStartIdx > 0) {
                int filterEndIdx = rawPath.lastIndexOf("]");

                if (filterEndIdx == -1) {
                    throw new RuntimeException("Invalid path: " + rawPath);
                }

                // expects the attribute to filter in the beginning
                String path = rawPath.substring(0, filterStartIdx);

                if (rawPath.indexOf('.', filterEndIdx) != -1) {
                    // append any sub-attribute after the filter, e.g. "emails[type eq "work"].value"
                    path = path + rawPath.substring(filterEndIdx + 1);
                }

                this.path = path;
                this.filter = rawPath.substring(filterStartIdx + 1, filterEndIdx);
            } else {
                this.path = rawPath;
                this.filter = null;
            }
        }
    }

    public String getPath() {
        return path;
    }

    public boolean hasFilter() {
        return filter != null;
    }

    public JsonNode getValue(Attribute<?, ?> attribute) {
        if (filter == null) {
            return NullNode.getInstance();
        }

        ScimFilterParser.FilterContext filterContext = FilterUtils.parseFilter(filter);
        return new ScimFilterToJsonNodeConverter(attribute).visit(filterContext);
    }

    /**
     * Whether the filter compares only the {@code value} sub-attribute, e.g. {@code members[value eq "..."]}. Such a
     * filter is turned into the value to remove by {@link #getValue(Attribute)}; any other is resolved against the
     * entries the resource holds, by {@link #select(Attribute, JsonNode)}. A nested value filter, such as
     * {@code emails[value[value eq "..."]]}, is never one: the conversion would ignore the nesting, while
     * {@code select} refuses it.
     */
    public boolean filtersOnlyOnValue() {
        if (filter == null) {
            return true;
        }
        ScimFilterParser.FilterContext parsed = FilterUtils.parseFilter(filter);
        return !hasValuePath(parsed) && attributeNames(parsed).stream().allMatch("value"::equalsIgnoreCase);
    }

    private static boolean hasValuePath(ScimFilterParser.FilterContext parsed) {
        boolean[] found = {false};
        new ScimFilterParserBaseVisitor<Void>() {
            @Override
            public Void visitValuePath(ScimFilterParser.ValuePathContext ctx) {
                found[0] = true;
                return null;
            }
        }.visit(parsed);
        return found[0];
    }

    /**
     * The entries of a complex multi-valued {@code attribute}, from those the resource currently holds, that the filter
     * selects (RFC 7644 section 3.5.2), e.g. the email whose {@code type} is {@code work}. Each sub-attribute the filter
     * names must be one of the complex type's, so a typo gets a 400 rather than silently matching nothing.
     *
     * @param attribute the complex multi-valued attribute the path names
     * @param current the attribute's current entries as JSON, or {@code null} when it has none
     * @return the matching entries, possibly none
     */
    public ArrayNode select(Attribute<?, ?> attribute, JsonNode current) {
        if (filter == null || attribute.getComplexType() == null || !attribute.isMultivalued()) {
            throw new IllegalStateException("select needs a filter on a complex multi-valued attribute");
        }

        ScimFilterParser.FilterContext parsed = FilterUtils.parseFilter(filter);
        Map<String, String> names = validate(parsed, attribute);
        ArrayNode matched = JsonNodeFactory.instance.arrayNode();

        if (current == null || !current.isArray()) {
            return matched;
        }

        for (JsonNode entry : current) {
            if (entry.isObject() && Boolean.TRUE.equals(new ScimFilterEntryMatcher(entry, names).visit(parsed))) {
                matched.add(entry);
            }
        }

        return matched;
    }

    /**
     * Checks the whole filter before any entry is compared, so that the outcome does not depend on the current entries
     * or on which side of an {@code and} or {@code or} decides. Each sub-attribute must be one of the complex type's,
     * in any case (RFC 7643 section 2.1), each operator must apply to the sub-attribute's type, a boolean is compared
     * only with {@code true} or {@code false}, and nested value filters are refused.
     *
     * @return the complex type's spelling of each sub-attribute the filter names, keyed by the name as written
     */
    private static Map<String, String> validate(ScimFilterParser.FilterContext parsed, Attribute<?, ?> attribute) {
        JavaType type = JsonSerialization.mapper.getTypeFactory().constructType(attribute.getComplexType());
        Map<String, BeanPropertyDefinition> properties = new HashMap<>();

        for (BeanPropertyDefinition property : JsonSerialization.mapper.getSerializationConfig().introspect(type).findProperties()) {
            properties.put(property.getName().toLowerCase(Locale.ROOT), property);
        }

        Map<String, String> names = new HashMap<>();

        new ScimFilterParserBaseVisitor<Void>() {
            private BeanPropertyDefinition resolve(String name) {
                BeanPropertyDefinition property = properties.get(name.toLowerCase(Locale.ROOT));

                if (property == null) {
                    throw new ModelValidationException("Unknown attribute " + name + " in the filter for " + attribute.getName());
                }

                names.put(name, property.getName());
                return property;
            }

            @Override
            public Void visitPresentExpression(ScimFilterParser.PresentExpressionContext ctx) {
                resolve(ctx.ATTRPATH().getText());
                return null;
            }

            @Override
            public Void visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
                String name = ctx.ATTRPATH().getText();
                Class<?> valueType = resolve(name).getRawPrimaryType();
                boolean isBoolean = Boolean.class.equals(valueType) || boolean.class.equals(valueType);
                String operator = ctx.compareOp().getText().toLowerCase(Locale.ROOT);

                if (!(isBoolean ? BOOLEAN_OPERATORS : STRING_OPERATORS).contains(operator)) {
                    throw new ModelValidationException("Operator '" + operator + "' is not supported for " + name
                            + " when selecting values of a multi-valued attribute");
                }

                String operand = FilterUtils.extractCompValue(ctx.compValue());

                if (isBoolean && operand != null && !"true".equalsIgnoreCase(operand) && !"false".equalsIgnoreCase(operand)) {
                    // the operand is a parameter, not part of the message: the message is a MessageFormat pattern
                    throw new ModelValidationException("Invalid boolean value {0} for {1} in the filter for {2}", operand, name,
                            attribute.getName());
                }

                return null;
            }

            @Override
            public Void visitValuePath(ScimFilterParser.ValuePathContext ctx) {
                throw new ModelValidationException("Nested value filters are not supported when selecting values of a multi-valued attribute");
            }
        }.visit(parsed);

        return names;
    }

    private static List<String> attributeNames(ScimFilterParser.FilterContext parsed) {
        List<String> names = new ArrayList<>();
        new ScimFilterParserBaseVisitor<Void>() {
            @Override
            public Void visitPresentExpression(ScimFilterParser.PresentExpressionContext ctx) {
                names.add(ctx.ATTRPATH().getText());
                return null;
            }

            @Override
            public Void visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
                names.add(ctx.ATTRPATH().getText());
                return null;
            }

            @Override
            public Void visitValuePath(ScimFilterParser.ValuePathContext ctx) {
                names.add(ctx.ATTRPATH().getText());
                return super.visitValuePath(ctx);
            }
        }.visit(parsed);
        return names;
    }

    /**
     * Decides whether one entry of a complex multi-valued attribute, as it currently is on the resource, matches a value
     * selection filter, e.g. {@code type eq "work"} in {@code emails[type eq "work"].value}.
     *
     * <p>RFC 7644 section 3.5.2 lets a PATCH {@code remove} or {@code replace} select the values of a multi-valued
     * attribute with a filter on any of its sub-attributes. {@link ScimFilterToJsonNodeConverter} turns a filter into the
     * value to remove, which only works for {@code value}; a filter on another sub-attribute has to be compared with the
     * entries the resource already holds. Microsoft Entra ID sends exactly that when a user's email changes.
     *
     * <p>The filter was checked by {@code validate} before this runs, which also gives the complex type's spelling of
     * each sub-attribute name. String comparisons ignore case, as {@code type}, {@code display} and {@code value} of the
     * core multi-valued attributes are not case exact.
     */
    private static final class ScimFilterEntryMatcher extends ScimFilterParserBaseVisitor<Boolean> {

        private final JsonNode entry;
        private final Map<String, String> names;

        ScimFilterEntryMatcher(JsonNode entry, Map<String, String> names) {
            this.entry = entry;
            this.names = names;
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
            if (ctx.valuePath() != null) {
                throw new ModelValidationException("Nested value filters are not supported when selecting values of a multi-valued attribute");
            }
            if (ctx.attributeExpression() != null) {
                return visit(ctx.attributeExpression());
            }
            return visit(ctx.expression());
        }

        @Override
        public Boolean visitPresentExpression(ScimFilterParser.PresentExpressionContext ctx) {
            JsonNode actual = entry.get(names.get(ctx.ATTRPATH().getText()));
            return actual != null && !actual.isNull() && !(actual.isTextual() && actual.asText().isEmpty());
        }

        @Override
        public Boolean visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
            String operator = ctx.compareOp().getText().toLowerCase(Locale.ROOT);
            String expected = FilterUtils.extractCompValue(ctx.compValue());
            JsonNode node = entry.get(names.get(ctx.ATTRPATH().getText()));
            String actual = node == null || node.isNull() ? null : node.asText();

            if (expected == null || actual == null) {
                return switch (operator) {
                    case "eq" -> expected == actual;
                    case "ne" -> expected != actual;
                    default -> false;
                };
            }

            String a = actual.toLowerCase(Locale.ROOT);
            String e = expected.toLowerCase(Locale.ROOT);

            return switch (operator) {
                case "eq" -> a.equals(e);
                case "ne" -> !a.equals(e);
                case "co" -> a.contains(e);
                case "sw" -> a.startsWith(e);
                case "ew" -> a.endsWith(e);
                default -> throw new ModelValidationException(
                        "Operator '" + operator + "' is not supported when selecting values of a multi-valued attribute");
            };
        }
    }
}
