package org.keycloak.scim.model.resourcetype.definition;

import java.util.List;

import org.keycloak.scim.resource.schema.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Admin-facing representation of a custom SCIM resource type definition managed per realm.
 * <p>
 * A definition describes a SCIM resource type (its name, endpoint, schema URN and attributes) but does not
 * carry any resource instance data. Built-in resource types (e.g. {@code Users}, {@code Groups}) are also
 * rendered using this representation with {@link #builtIn} set to {@code true} so that they can be listed
 * alongside custom ones as read-only entries.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScimResourceTypeRepresentation {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("schema")
    private String schema;

    @JsonProperty("builtIn")
    private boolean builtIn;

    @JsonProperty("attributes")
    private List<Schema.Attribute> attributes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public List<Schema.Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Schema.Attribute> attributes) {
        this.attributes = attributes;
    }
}
