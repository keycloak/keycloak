package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"addReadTokenRoleOnCreate","alias","config","displayName","enabled","firstBrokerLoginFlowAlias","internalId","linkOnly","postBrokerLoginFlowAlias","providerId","storeToken","trustEmail"})
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
public class IdentityProvidersSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("addReadTokenRoleOnCreate")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Adds Read Token role when creating this Identity Provider.")
    private Boolean addReadTokenRoleOnCreate;

    public Boolean getAddReadTokenRoleOnCreate() {
        return addReadTokenRoleOnCreate;
    }

    public void setAddReadTokenRoleOnCreate(Boolean addReadTokenRoleOnCreate) {
        this.addReadTokenRoleOnCreate = addReadTokenRoleOnCreate;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("alias")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider Alias.")
    private String alias;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("config")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider config.")
    private java.util.Map<java.lang.String, String> config;

    public java.util.Map<java.lang.String, String> getConfig() {
        return config;
    }

    public void setConfig(java.util.Map<java.lang.String, String> config) {
        this.config = config;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("displayName")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider Display Name.")
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("enabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider enabled flag.")
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("firstBrokerLoginFlowAlias")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider First Broker Login Flow Alias.")
    private String firstBrokerLoginFlowAlias;

    public String getFirstBrokerLoginFlowAlias() {
        return firstBrokerLoginFlowAlias;
    }

    public void setFirstBrokerLoginFlowAlias(String firstBrokerLoginFlowAlias) {
        this.firstBrokerLoginFlowAlias = firstBrokerLoginFlowAlias;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("internalId")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider Internal ID.")
    private String internalId;

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("linkOnly")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider Link Only setting.")
    private Boolean linkOnly;

    public Boolean getLinkOnly() {
        return linkOnly;
    }

    public void setLinkOnly(Boolean linkOnly) {
        this.linkOnly = linkOnly;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("postBrokerLoginFlowAlias")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider Post Broker Login Flow Alias.")
    private String postBrokerLoginFlowAlias;

    public String getPostBrokerLoginFlowAlias() {
        return postBrokerLoginFlowAlias;
    }

    public void setPostBrokerLoginFlowAlias(String postBrokerLoginFlowAlias) {
        this.postBrokerLoginFlowAlias = postBrokerLoginFlowAlias;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("providerId")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider ID.")
    private String providerId;

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("storeToken")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider Store to Token.")
    private Boolean storeToken;

    public Boolean getStoreToken() {
        return storeToken;
    }

    public void setStoreToken(Boolean storeToken) {
        this.storeToken = storeToken;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("trustEmail")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Identity Provider Trust Email.")
    private Boolean trustEmail;

    public Boolean getTrustEmail() {
        return trustEmail;
    }

    public void setTrustEmail(Boolean trustEmail) {
        this.trustEmail = trustEmail;
    }
}
