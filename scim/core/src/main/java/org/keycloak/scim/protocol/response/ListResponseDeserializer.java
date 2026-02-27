package org.keycloak.scim.protocol.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.resourcetype.ResourceType;
import org.keycloak.scim.resource.user.User;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.keycloak.scim.resource.Scim.getCoreSchema;

public class ListResponseDeserializer extends JsonDeserializer<List<ResourceTypeRepresentation>> {

    @Override
    public List<ResourceTypeRepresentation> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode nodes = mapper.readTree(parser);
        List<ResourceTypeRepresentation> resources = new ArrayList<>();

        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                ResourceTypeRepresentation resource = parseNode(mapper, node);

                if (resource != null) {
                    resources.add(resource);
                }
            }
        }

        return resources;
    }

    private ResourceTypeRepresentation parseNode(ObjectMapper mapper, JsonNode node) throws IOException {
        Class<? extends ResourceTypeRepresentation> resourceType = getResourceType(node);

        return mapper.treeToValue(node, resourceType);
    }

    private Class<? extends ResourceTypeRepresentation> getResourceType(JsonNode node) {
        Set<String> schemas = getSchemas(node);

        if (schemas.contains(getCoreSchema(User.class))) {
            return User.class;
        } else if (schemas.contains(getCoreSchema(Group.class))) {
            return Group.class;
        } else if (schemas.contains(getCoreSchema(ResourceType.class))) {
            return  ResourceType.class;
        }

        throw new IllegalArgumentException("Could not map resource type from any of the schemas: " + schemas);
    }

    private Set<String> getSchemas(JsonNode node) {
        if (node.has("schemas")) {
            JsonNode schemasNode = node.get("schemas");

            if (schemasNode.isArray()) {
                return schemasNode.valueStream().map(JsonNode::asText).collect(Collectors.toSet());
            }

            return Set.of(schemasNode.asText());
        }

        throw new IllegalArgumentException("No schema set to JSON node");
    }
}
