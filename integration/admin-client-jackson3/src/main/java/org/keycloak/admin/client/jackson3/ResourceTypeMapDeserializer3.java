package org.keycloak.admin.client.jackson3;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.authorization.ResourceType;

import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

class ResourceTypeMapDeserializer3 extends ValueDeserializer<Map<String, ResourceType>> {

    @Override
    public Map<String, ResourceType> deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.isExpectedStartArrayToken()) {
            List<ResourceType> resourceTypeList = ctxt.readValue(p, new TypeReference<List<ResourceType>>() {});
            return resourceTypeList.stream()
                    .collect(Collectors.toMap(ResourceType::getType, Function.identity()));
        } else if (p.isExpectedStartObjectToken()) {
            return ctxt.readValue(p, new TypeReference<Map<String, ResourceType>>() {});
        } else {
            ctxt.reportInputMismatch(this, "Expected an array or object for resourceTypes");
            return null;
        }
    }
}
