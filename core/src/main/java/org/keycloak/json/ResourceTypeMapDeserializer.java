package org.keycloak.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.authorization.ResourceType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ResourceTypeMapDeserializer extends JsonDeserializer<Map<String, ResourceType>> {

    @Override
    public Map<String, ResourceType> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.isExpectedStartArrayToken()) {
            List<ResourceType> resourceTypeList = parser.readValueAs(new TypeReference<List<ResourceType>>() {});
            return resourceTypeList.stream()
                    .collect(Collectors.toMap(ResourceType::getType, Function.identity()));
        } else if (parser.isExpectedStartObjectToken()) {
            return parser.readValueAs(new TypeReference<Map<String, ResourceType>>() {});
        } else {
            throw JsonMappingException.from(parser, "Expected an array or object for resourceTypes");
        }
    }
}
