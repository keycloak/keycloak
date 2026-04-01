package org.keycloak.scim.resource.schema.path;

import java.util.function.Predicate;

import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.ModelSchema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;

public final class Path {

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

    public JsonNode getValue(JsonNode rawValue) {
        if (filter == null) {
            return rawValue;
        }

        ScimFilterParser.FilterContext filterContext = FilterUtils.parseFilter(filter);
        Predicate<JsonNode> predicate = new ScimJsonNodeFilterEvaluator().visit(filterContext);

        if (rawValue.isArray()) {
            ArrayNode matches = JsonNodeFactory.instance.arrayNode();
            for (JsonNode node : rawValue) {
                if (node.isObject() && predicate.test(node)) {
                    matches.add(node);
                }
            }
            if (!matches.isEmpty()) {
                return matches.size() == 1 ? matches.get(0) : matches;
            }
        }

        return NullNode.getInstance();
    }

    public boolean hasFilter() {
        return filter != null;
    }

}
