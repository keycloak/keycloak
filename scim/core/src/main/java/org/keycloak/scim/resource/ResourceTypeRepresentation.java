package org.keycloak.scim.resource;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.keycloak.scim.resource.common.Meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.keycloak.scim.resource.Scim.getCoreSchema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"type"})
public abstract class ResourceTypeRepresentation {

    @JsonProperty("schemas")
    private Set<String> schemas;
    @JsonProperty("id")
    private String id;

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("meta")
    private Meta meta;

    @JsonIgnore
    private long createdTimestamp;

    public Set<String> getSchemas() {
        if (schemas == null) {
            schemas = new HashSet<>();
            schemas.add(getCoreSchema(this.getClass()));
        }
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public boolean hasSchema(String schema) {
        return Optional.ofNullable(getSchemas()).orElse(Set.of()).contains(schema);
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void addSchema(String schema) {
        if (schemas == null) {
            schemas = new HashSet<>();
        }
        schemas.add(schema);
    }
}
