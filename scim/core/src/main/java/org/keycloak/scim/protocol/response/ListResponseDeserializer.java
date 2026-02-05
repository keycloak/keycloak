package org.keycloak.scim.protocol.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.scim.resource.ScimResource;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.user.User;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ListResponseDeserializer extends JsonDeserializer<List<ScimResource>> {

    private static final String USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
    private static final String GROUP_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";

    @Override
    public List<ScimResource> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode nodes = mapper.readTree(parser);
        List<ScimResource> resources = new ArrayList<>();

        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                ScimResource resource = parseNode(mapper, node);

                if (resource != null) {
                    resources.add(resource);
                }
            }
        }

        return resources;
    }

    private ScimResource parseNode(ObjectMapper mapper, JsonNode node) throws IOException {
        Class<? extends ScimResource> resourceType = getResourceType(node);

        return mapper.treeToValue(node, resourceType);
    }

    private Class<? extends ScimResource> getResourceType(JsonNode node) {
        Set<String> schemas = getSchemas(node);

        if (schemas.contains(USER_SCHEMA)) {
            return User.class;
        } else if (schemas.contains(GROUP_SCHEMA)) {
            return Group.class;
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
