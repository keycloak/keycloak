package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"authenticator","authenticatorConfig","authenticatorFlow","flowAlias","priority","requirement","userSetupAllowed"})
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
public class AuthenticationExecutionsSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("authenticator")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Authenticator")
    private String authenticator;

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("authenticatorConfig")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Authenticator Config")
    private String authenticatorConfig;

    public String getAuthenticatorConfig() {
        return authenticatorConfig;
    }

    public void setAuthenticatorConfig(String authenticatorConfig) {
        this.authenticatorConfig = authenticatorConfig;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("authenticatorFlow")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Authenticator flow")
    private Boolean authenticatorFlow;

    public Boolean getAuthenticatorFlow() {
        return authenticatorFlow;
    }

    public void setAuthenticatorFlow(Boolean authenticatorFlow) {
        this.authenticatorFlow = authenticatorFlow;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("flowAlias")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Flow Alias")
    private String flowAlias;

    public String getFlowAlias() {
        return flowAlias;
    }

    public void setFlowAlias(String flowAlias) {
        this.flowAlias = flowAlias;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("priority")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Priority")
    private Integer priority;

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("requirement")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Requirement [REQUIRED, OPTIONAL, ALTERNATIVE, DISABLED]")
    private String requirement;

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("userSetupAllowed")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("User setup allowed")
    private Boolean userSetupAllowed;

    public Boolean getUserSetupAllowed() {
        return userSetupAllowed;
    }

    public void setUserSetupAllowed(Boolean userSetupAllowed) {
        this.userSetupAllowed = userSetupAllowed;
    }
}
