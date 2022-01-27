package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"instanceSelector","realm","realmOverrides","unmanaged"})
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
public class KeycloakRealmSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("instanceSelector")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Selector for looking up Keycloak Custom Resources.")
    private InstanceSelectorSpec instanceSelector;

    public InstanceSelectorSpec getInstanceSelector() {
        return instanceSelector;
    }

    public void setInstanceSelector(InstanceSelectorSpec instanceSelector) {
        this.instanceSelector = instanceSelector;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("realm")
    @javax.validation.constraints.NotNull()
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Keycloak Realm REST object.")
    private RealmSpec realm;

    public RealmSpec getRealm() {
        return realm;
    }

    public void setRealm(RealmSpec realm) {
        this.realm = realm;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("realmOverrides")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A list of overrides to the default Realm behavior.")
    private java.util.List<RealmOverridesSpec> realmOverrides;

    public java.util.List<RealmOverridesSpec> getRealmOverrides() {
        return realmOverrides;
    }

    public void setRealmOverrides(java.util.List<RealmOverridesSpec> realmOverrides) {
        this.realmOverrides = realmOverrides;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("unmanaged")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("When set to true, this KeycloakRealm will be marked as unmanaged and not be managed by this operator. It can then be used for targeting purposes.")
    private Boolean unmanaged;

    public Boolean getUnmanaged() {
        return unmanaged;
    }

    public void setUnmanaged(Boolean unmanaged) {
        this.unmanaged = unmanaged;
    }
}
