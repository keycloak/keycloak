package org.keycloak.scim.resource.schema.path;

import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;

import com.fasterxml.jackson.databind.JsonNode;
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

    public JsonNode getValue(Attribute<?, ?> attribute) {
        if (filter == null) {
            return NullNode.getInstance();
        }

        ScimFilterParser.FilterContext filterContext = FilterUtils.parseFilter(filter);
        return new ScimFilterToJsonNodeConverter(attribute).visit(filterContext);
    }
}
