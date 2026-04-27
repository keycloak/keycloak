package org.keycloak.scim.resource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.scim.resource.common.Meta;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.keycloak.scim.resource.Scim.getCoreSchema;

@JsonInclude(Include.NON_NULL)
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
    private Long createdTimestamp;

    @JsonIgnore
    private Long lastModifiedTimestamp;

    private Map<String, Object> extensions;

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

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setLastModifiedTimestamp(Long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    public Long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void addSchema(String schema) {
        if (schemas == null) {
            schemas = new HashSet<>();
        }
        schemas.add(schema);
    }

    @JsonAnyGetter
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @JsonAnySetter
    public void setExtensions(String name, Object value) {
        if (extensions == null) {
            extensions = new HashMap<>();
        }
        this.extensions.put(name, value);
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }
}
