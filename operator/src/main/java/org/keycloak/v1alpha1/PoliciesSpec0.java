package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"config","decisionStrategy","id","logic","name","owner","policies","resources","resourcesData","scopes","scopesData","type"})
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
public class PoliciesSpec0 implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("config")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Config.")
    private java.util.Map<java.lang.String, String> config;

    public java.util.Map<java.lang.String, String> getConfig() {
        return config;
    }

    public void setConfig(java.util.Map<java.lang.String, String> config) {
        this.config = config;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("decisionStrategy")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("The decision strategy dictates how the policies associated with a given permission are evaluated and how a final decision is obtained. 'Affirmative' means that at least one policy must evaluate to a positive decision in order for the final decision to be also positive. 'Unanimous' means that all policies must evaluate to a positive decision in order for the final decision to be also positive. 'Consensus' means that the number of positive decisions must be greater than the number of negative decisions. If the number of positive and negative is the same, the final decision will be negative.")
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

    @com.fasterxml.jackson.annotation.JsonProperty("logic")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("The logic dictates how the policy decision should be made. If 'Positive', the resulting effect (permit or deny) obtained during the evaluation of this policy will be used to perform a decision. If 'Negative', the resulting effect will be negated, in other words, a permit becomes a deny and vice-versa.")
    private String logic;

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("name")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("The name of this policy.")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("owner")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Owner.")
    private String owner;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("policies")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Policies.")
    private java.util.List<String> policies;

    public java.util.List<String> getPolicies() {
        return policies;
    }

    public void setPolicies(java.util.List<String> policies) {
        this.policies = policies;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("resources")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Resources.")
    private java.util.List<String> resources;

    public java.util.List<String> getResources() {
        return resources;
    }

    public void setResources(java.util.List<String> resources) {
        this.resources = resources;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("resourcesData")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Resources Data.")
    private java.util.List<ResourcesDataSpec0> resourcesData;

    public java.util.List<ResourcesDataSpec0> getResourcesData() {
        return resourcesData;
    }

    public void setResourcesData(java.util.List<ResourcesDataSpec0> resourcesData) {
        this.resourcesData = resourcesData;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("scopes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Scopes.")
    private java.util.List<String> scopes;

    public java.util.List<String> getScopes() {
        return scopes;
    }

    public void setScopes(java.util.List<String> scopes) {
        this.scopes = scopes;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("scopesData")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Scopes Data.")
    private java.util.List<ScopesDataSpec0> scopesData;

    public java.util.List<ScopesDataSpec0> getScopesData() {
        return scopesData;
    }

    public void setScopesData(java.util.List<ScopesDataSpec0> scopesData) {
        this.scopesData = scopesData;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("type")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Type.")
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
