package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"allowRemoteResourceManagement","clientId","decisionStrategy","id","name","policies","policyEnforcementMode","resources","scopes"})
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
public class AuthorizationSettingsSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("allowRemoteResourceManagement")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if resources should be managed remotely by the resource server.")
    private Boolean allowRemoteResourceManagement;

    public Boolean getAllowRemoteResourceManagement() {
        return allowRemoteResourceManagement;
    }

    public void setAllowRemoteResourceManagement(Boolean allowRemoteResourceManagement) {
        this.allowRemoteResourceManagement = allowRemoteResourceManagement;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("clientId")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client ID.")
    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("decisionStrategy")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("The decision strategy dictates how permissions are evaluated and how a final decision is obtained. 'Affirmative' means that at least one permission must evaluate to a positive decision in order to grant access to a resource and its scopes. 'Unanimous' means that all permissions must evaluate to a positive decision in order for the final decision to be also positive.")
    private String decisionStrategy;

    public String getDecisionStrategy() {
        return decisionStrategy;
    }

    public void setDecisionStrategy(String decisionStrategy) {
        this.decisionStrategy = decisionStrategy;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("id")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("ID.")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("name")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Name.")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("policies")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Policies.")
    private java.util.List<PoliciesSpec> policies;

    public java.util.List<PoliciesSpec> getPolicies() {
        return policies;
    }

    public void setPolicies(java.util.List<PoliciesSpec> policies) {
        this.policies = policies;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("policyEnforcementMode")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("The policy enforcement mode dictates how policies are enforced when evaluating authorization requests. 'Enforcing' means requests are denied by default even when there is no policy associated with a given resource. 'Permissive' means requests are allowed even when there is no policy associated with a given resource. 'Disabled' completely disables the evaluation of policies and allows access to any resource.")
    private String policyEnforcementMode;

    public String getPolicyEnforcementMode() {
        return policyEnforcementMode;
    }

    public void setPolicyEnforcementMode(String policyEnforcementMode) {
        this.policyEnforcementMode = policyEnforcementMode;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("resources")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Resources.")
    private java.util.List<ResourcesSpec> resources;

    public java.util.List<ResourcesSpec> getResources() {
        return resources;
    }

    public void setResources(java.util.List<ResourcesSpec> resources) {
        this.resources = resources;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("scopes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Authorization Scopes.")
    private java.util.List<ScopesSpec1> scopes;

    public java.util.List<ScopesSpec1> getScopes() {
        return scopes;
    }

    public void setScopes(java.util.List<ScopesSpec1> scopes) {
        this.scopes = scopes;
    }
}
