package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"experimental","podlabels","resources"})
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
public class KeycloakDeploymentSpecSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("experimental")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Experimental section NOTE: This section might change or get removed without any notice. It may also cause the deployment to behave in an unpredictable fashion. Please use with care.")
    private ExperimentalSpec experimental;

    public ExperimentalSpec getExperimental() {
        return experimental;
    }

    public void setExperimental(ExperimentalSpec experimental) {
        this.experimental = experimental;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("podlabels")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("List of labels to set in the keycloak pods")
    private java.util.Map<java.lang.String, String> podlabels;

    public java.util.Map<java.lang.String, String> getPodlabels() {
        return podlabels;
    }

    public void setPodlabels(java.util.Map<java.lang.String, String> podlabels) {
        this.podlabels = podlabels;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("resources")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Resources (Requests and Limits) for the Pods.")
    private ResourcesSpec resources;

    public ResourcesSpec getResources() {
        return resources;
    }

    public void setResources(ResourcesSpec resources) {
        this.resources = resources;
    }
}
