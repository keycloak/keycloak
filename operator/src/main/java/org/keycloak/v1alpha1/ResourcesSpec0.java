package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"_id","attributes","displayName","icon_uri","name","ownerManagedAccess","scopes","type","uris"})
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
@lombok.ToString()
@lombok.EqualsAndHashCode()
@lombok.Setter()
@lombok.experimental.Accessors(prefix = {
    "_",
    ""
})
@io.sundr.builder.annotations.Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false, builderPackage = "io.fabric8.kubernetes.api.builder", refs = {
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.ObjectMeta.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.ObjectReference.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.LabelSelector.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.Container.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.EnvVar.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.ContainerPort.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.Volume.class),
    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.VolumeMount.class)
})
public class ResourcesSpec0 implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("_id")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("ID.")
    private String _id;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("attributes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("The attributes associated with the resource.")
    private java.util.Map<java.lang.String, String> attributes;

    public java.util.Map<java.lang.String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(java.util.Map<java.lang.String, String> attributes) {
        this.attributes = attributes;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("displayName")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A unique name for this resource. The name can be used to uniquely identify a resource, useful when querying for a specific resource.")
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("icon_uri")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("An URI pointing to an icon.")
    private String icon_uri;

    public String getIcon_uri() {
        return icon_uri;
    }

    public void setIcon_uri(String icon_uri) {
        this.icon_uri = icon_uri;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("name")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A unique name for this resource. The name can be used to uniquely identify a resource, useful when querying for a specific resource.")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("ownerManagedAccess")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if the access to this resource can be managed by the resource owner.")
    private Boolean ownerManagedAccess;

    public Boolean getOwnerManagedAccess() {
        return ownerManagedAccess;
    }

    public void setOwnerManagedAccess(Boolean ownerManagedAccess) {
        this.ownerManagedAccess = ownerManagedAccess;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("scopes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("The scopes associated with this resource.")
    private java.util.List<ScopesSpec3> scopes;

    public java.util.List<ScopesSpec3> getScopes() {
        return scopes;
    }

    public void setScopes(java.util.List<ScopesSpec3> scopes) {
        this.scopes = scopes;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("type")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("The type of this resource. It can be used to group different resource instances with the same type.")
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("uris")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Set of URIs which are protected by resource.")
    private java.util.List<String> uris;

    public java.util.List<String> getUris() {
        return uris;
    }

    public void setUris(java.util.List<String> uris) {
        this.uris = uris;
    }
}
