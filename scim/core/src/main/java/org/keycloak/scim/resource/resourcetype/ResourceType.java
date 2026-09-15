package org.keycloak.scim.resource.resourcetype;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResourceType extends org.keycloak.scim.resource.ResourceTypeRepresentation {

    public static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:ResourceType";

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("schema")
    private String schema;

    @JsonProperty("schemaExtensions")
    private List<SchemaExtension> schemaExtensions;

    @Override
    public Set<String> getSchemas() {
        return Set.of(SCHEMA);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public List<SchemaExtension> getSchemaExtensions() {
        return schemaExtensions;
    }

    public void setSchemaExtensions(List<SchemaExtension> schemaExtensions) {
        this.schemaExtensions = schemaExtensions;
    }

    /**
     * Represents a schema extension for a resource type
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SchemaExtension {
        @JsonProperty("schema")
        private String schema;

        @JsonProperty("required")
        private Boolean required = false;

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }
    }
}
