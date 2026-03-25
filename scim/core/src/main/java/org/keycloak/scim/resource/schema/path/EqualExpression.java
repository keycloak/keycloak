package org.keycloak.scim.resource.schema.path;

import java.util.function.Function;

import org.keycloak.scim.resource.common.MultiValuedAttribute;
import org.keycloak.scim.resource.schema.attribute.Attribute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

record EqualExpression(Attribute<?, ?> attribute, String attributeName,
                       String value) implements Function<JsonNode, JsonNode> {

    @Override
    public JsonNode apply(JsonNode rawValue) {
        Class<?> complexType = attribute.getComplexType();

        if (complexType != null) {
            if (MultiValuedAttribute.class.isAssignableFrom(complexType)) {
                if (rawValue.isArray()) {
                    for (JsonNode node : rawValue) {
                        if (node.isObject()) {
                            if ("value".equals(attributeName)) {
                                JsonNode value = node.get(attributeName);

                                if (value != null && value.asText().equals(this.value.replaceAll("\"", ""))) {
                                    return node;

                                }
                            }
                        }
                    }
                }
            }
        }

        return NullNode.getInstance();
    }
}
