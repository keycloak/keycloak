package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"attributes","clientRole","composite","composites","containerId","id","name"})
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
public class RealmSpec0 implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("attributes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Role Attributes")
    private java.util.Map<java.lang.String, java.util.List<String>> attributes;

    public java.util.Map<java.lang.String, java.util.List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(java.util.Map<java.lang.String, java.util.List<String>> attributes) {
        this.attributes = attributes;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("clientRole")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client Role")
    private Boolean clientRole;

    public Boolean getClientRole() {
        return clientRole;
    }

    public void setClientRole(Boolean clientRole) {
        this.clientRole = clientRole;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("composite")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Composite")
    private Boolean composite;

    public Boolean getComposite() {
        return composite;
    }

    public void setComposite(Boolean composite) {
        this.composite = composite;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("composites")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Composites")
    private CompositesSpec1 composites;

    public CompositesSpec1 getComposites() {
        return composites;
    }

    public void setComposites(CompositesSpec1 composites) {
        this.composites = composites;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("containerId")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Container Id")
    private String containerId;

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("id")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("name")
    @javax.validation.constraints.NotNull()
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
