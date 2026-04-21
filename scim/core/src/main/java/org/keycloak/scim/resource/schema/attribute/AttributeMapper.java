package org.keycloak.scim.resource.schema.attribute;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.keycloak.common.util.TriConsumer;
import org.keycloak.models.Model;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>An attribute mapper defines how to set an attribute to a {@link Model} and its corresponding {@link ResourceTypeRepresentation}.
 *
 * @see Attribute
 */
public class AttributeMapper<M extends Model, R extends ResourceTypeRepresentation> {

    private Attribute<M, R> attribute;
    private final TriConsumer<M, String, ?> modelSetter;
    private TriConsumer<M, String, ?> modelRemover;
    private TriConsumer<M, String, ?> modelAdder;
    private final BiConsumer<R, ?> representationSetter;

    AttributeMapper(TriConsumer<M, String, ?> modelSetter, BiConsumer<R, ?> representationSetter) {
        this(modelSetter, representationSetter, null, null);
    }

    AttributeMapper(TriConsumer<M, String, ?> modelSetter, BiConsumer<R, ?> representationSetter, TriConsumer<M, String, ?> modelRemover, TriConsumer<M, String, ?> modelAdder) {
        this.modelSetter = modelSetter;
        this.representationSetter = representationSetter;
        this.modelRemover = modelRemover;
        this.modelAdder = modelAdder;
    }

    public void setValue(R representation, Object value) {
        if (representationSetter != null) {
            ((BiConsumer<R, Object>) representationSetter).accept(representation, value);
        }
    }

    public void setValue(M model, JsonNode value) {
        setValue(model, value, (TriConsumer<M, String, Object>) modelSetter);
    }

    public void addValue(M model, JsonNode value) {
        if (modelAdder == null) {
            setValue(model, value);
        } else {
            setValue(model, value, (TriConsumer<M, String, Object>) modelAdder);
        }
    }

    public void removeValue(M model, JsonNode value) {
        if  (modelRemover == null) {
            setValue(model, null);
        } else {
            setValue(model, value, (TriConsumer<M, String, Object>) modelRemover);
        }
    }

    private void setValue(M model, JsonNode value, TriConsumer<M, String, Object> modelSetter) {
        if (modelSetter == null) {
            return;
        }

        String name = attribute.getModelAttributeName();

        if (name == null) {
            return;
        }

        if (attribute != null && attribute.isMultivalued()) {
            Class<?> complexType = attribute.getComplexType();

            if (complexType == null) {
                Set<String> values;

                if (value.isArray()) {
                    values =  value.valueStream().map(JsonNode::asText).collect(Collectors.toSet());
                } else {
                    values = Set.of(value.asText());
                }

                modelSetter.accept(model, name, values);
            } else if (value != null) {
                Set<Object> values = new HashSet<>();

                if (value.isArray()) {
                    for (JsonNode v : value) {
                        if (v.isValueNode()) {
                            values.add(v.textValue());
                        } else {
                            try {
                                values.add(JsonSerialization.readValue(v.toString(), complexType));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                } else if (value.isTextual()) {
                    values.add(value.textValue());
                } else if (!value.isNull()) {
                    try {
                        values.add(JsonSerialization.readValue(value.toString(), complexType));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                modelSetter.accept(model, name, values);
            }
        } else if (value != null && !value.isNull()) {
            modelSetter.accept(model, name, value.asText());
        } else {
            modelSetter.accept(model, name, null);
        }
    }

    void setAttribute(Attribute<M, R> attribute) {
        this.attribute = attribute;
    }
}
