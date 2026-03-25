package org.keycloak.scim.resource.schema.path;

import java.util.function.Function;

import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;

import com.fasterxml.jackson.databind.JsonNode;

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

    public JsonNode getValue(Attribute<?, ?> attribute, JsonNode rawValue) {
        if (filter == null) {
            return rawValue;
        }

        return parseFilter(attribute).apply(rawValue);
    }

    private Function<JsonNode, JsonNode> parseFilter(Attribute<?, ?> attribute) {
        String[] parts = filter.trim().split(" ");

        if (parts.length == 3) {
            String leftOperand = parts[0];
            String operator = parts[1];
            String rightOperand = parts[2];

            if ("eq".equals(operator)) {
                return new EqualExpression(attribute, leftOperand, rightOperand);
            }

            // for now, we only support equality filter in the path, and we assume the filter is always in the format "attribute eq "value""
            throw new ModelValidationException("Unsupported filter operator: " + operator);
        }

        throw new ModelValidationException("Unsupported filter format: " + filter);
    }

    public boolean hasFilter() {
        return filter != null;
    }

}
