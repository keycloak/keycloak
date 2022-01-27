package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"client","realm"})
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
public class RolesSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("client")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client Roles")
    private java.util.Map<java.lang.String, java.util.List<ClientSpec>> client;

    public java.util.Map<java.lang.String, java.util.List<ClientSpec>> getClient() {
        return client;
    }

    public void setClient(java.util.Map<java.lang.String, java.util.List<ClientSpec>> client) {
        this.client = client;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("realm")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Realm Roles")
    private java.util.List<RealmSpec0> realm;

    public java.util.List<RealmSpec0> getRealm() {
        return realm;
    }

    public void setRealm(java.util.List<RealmSpec0> realm) {
        this.realm = realm;
    }
}
