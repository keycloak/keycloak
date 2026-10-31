package org.keycloak.quarkus.runtime.integration.jaxrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.POJONode;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Keeps public Jackson 2 tree properties usable by the REST mapper, including token-buffer
 * conversions. Raw JSON would bypass tokens and turn nested values into opaque raw values.
 */
final class LegacyJsonNodeAdapter extends SimpleModule {

    LegacyJsonNodeAdapter(JsonNodeFactory factory) {
        addSerializer(JsonNode.class, new Serializer());
        addDeserializer(JsonNode.class, new ValueDeserializer<>() {
            @Override
            public JsonNode deserialize(JsonParser parser, DeserializationContext context) {
                return toLegacy(context.readTree(parser), factory);
            }

            @Override
            public JsonNode getNullValue(DeserializationContext context) {
                return factory.nullNode();
            }
        });
    }

    static JsonNode toLegacy(tools.jackson.databind.JsonNode node, JsonNodeFactory factory) {
        return switch (node.getNodeType()) {
            case OBJECT -> {
                var object = factory.objectNode();
                node.properties().forEach(property -> object.set(property.getKey(), toLegacy(property.getValue(), factory)));
                yield object;
            }
            case ARRAY -> {
                var array = factory.arrayNode();
                node.forEach(element -> array.add(toLegacy(element, factory)));
                yield array;
            }
            case STRING -> factory.textNode(node.stringValue());
            case BOOLEAN -> factory.booleanNode(node.booleanValue());
            case NULL -> factory.nullNode();
            case MISSING -> factory.missingNode();
            case BINARY -> factory.binaryNode(node.binaryValue());
            case POJO -> factory.pojoNode(((tools.jackson.databind.node.POJONode) node).getPojo());
            case NUMBER -> switch (((tools.jackson.databind.node.NumericNode) node).numberType()) {
                case INT -> factory.numberNode(node.intValue());
                case LONG -> factory.numberNode(node.longValue());
                case BIG_INTEGER -> factory.numberNode(node.bigIntegerValue());
                case FLOAT -> factory.numberNode(node.floatValue());
                case DOUBLE -> factory.numberNode(node.doubleValue());
                case BIG_DECIMAL -> factory.numberNode(node.decimalValue());
            };
        };
    }

    private static final class Serializer extends ValueSerializer<JsonNode> {

        @Override
        public void serialize(JsonNode node, JsonGenerator generator, SerializationContext context) {
            switch (node.getNodeType()) {
                case OBJECT -> {
                    generator.writeStartObject(node);
                    node.properties().forEach(property -> {
                        generator.writeName(property.getKey());
                        serialize(property.getValue(), generator, context);
                    });
                    generator.writeEndObject();
                }
                case ARRAY -> {
                    generator.writeStartArray(node, node.size());
                    node.forEach(element -> serialize(element, generator, context));
                    generator.writeEndArray();
                }
                case STRING -> generator.writeString(node.textValue());
                case BOOLEAN -> generator.writeBoolean(node.booleanValue());
                case NULL, MISSING -> generator.writeNull();
                case BINARY -> generator.writeBinary(((com.fasterxml.jackson.databind.node.BinaryNode) node).binaryValue());
                case POJO -> context.writeValue(generator, ((POJONode) node).getPojo());
                case NUMBER -> {
                    // Emit numbers as numbers, including non-finite floating-point values. The
                    // JSON generator retains the legacy quoting policy; token buffers retain types.
                    switch (node.numberType()) {
                        case INT -> generator.writeNumber(node.intValue());
                        case LONG -> generator.writeNumber(node.longValue());
                        case BIG_INTEGER -> generator.writeNumber(node.bigIntegerValue());
                        case FLOAT -> generator.writeNumber(node.floatValue());
                        case DOUBLE -> generator.writeNumber(node.doubleValue());
                        case BIG_DECIMAL -> generator.writeNumber(node.decimalValue());
                    }
                }
            }
        }
    }
}
