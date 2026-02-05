package org.keycloak.scim.resource;

import java.util.Set;

import org.keycloak.scim.resource.common.Meta;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.user.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"type", "path"})
public abstract class ScimResource {

    public enum Type {
        User,
        Group,
        ServiceProviderConfig,
        ResourceType,
        Schema;

        public String getPath() {
            return switch (this) {
                case User -> "/Users";
                case Group -> "/Groups";
                case ServiceProviderConfig -> "/ServiceProviderConfig";
                case ResourceType -> "/ResourceTypes";
                case Schema -> "/Schemas";
                default -> throw new IllegalStateException("Unknown resource type: " + this);
            };
        }

        public Class<? extends ScimResource> getType() {
            return switch (this) {
                case User -> User.class;
                case Group -> Group.class;
                default -> throw new IllegalStateException("Unknown resource type: " + this);
            };
        }
    }

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

    public abstract String getPath();

    public abstract Type getType();

    public Set<String> getSchemas() {
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
        if (schemas == null) {
            return false;
        }
        return schemas.contains(schema);
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
}
