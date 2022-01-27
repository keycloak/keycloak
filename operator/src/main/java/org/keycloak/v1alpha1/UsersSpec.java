package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"attributes","clientRoles","credentials","email","emailVerified","enabled","federatedIdentities","firstName","groups","id","lastName","realmRoles","requiredActions","username"})
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
public class UsersSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("attributes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Attributes.")
    private java.util.Map<java.lang.String, java.util.List<String>> attributes;

    public java.util.Map<java.lang.String, java.util.List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(java.util.Map<java.lang.String, java.util.List<String>> attributes) {
        this.attributes = attributes;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("clientRoles")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Client Roles.")
    private java.util.Map<java.lang.String, java.util.List<String>> clientRoles;

    public java.util.Map<java.lang.String, java.util.List<String>> getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(java.util.Map<java.lang.String, java.util.List<String>> clientRoles) {
        this.clientRoles = clientRoles;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("credentials")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Credentials.")
    private java.util.List<CredentialsSpec> credentials;

    public java.util.List<CredentialsSpec> getCredentials() {
        return credentials;
    }

    public void setCredentials(java.util.List<CredentialsSpec> credentials) {
        this.credentials = credentials;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("email")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Email.")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("emailVerified")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if email has already been verified.")
    private Boolean emailVerified;

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("enabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("User enabled flag.")
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("federatedIdentities")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Federated Identities.")
    private java.util.List<FederatedIdentitiesSpec> federatedIdentities;

    public java.util.List<FederatedIdentitiesSpec> getFederatedIdentities() {
        return federatedIdentities;
    }

    public void setFederatedIdentities(java.util.List<FederatedIdentitiesSpec> federatedIdentities) {
        this.federatedIdentities = federatedIdentities;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("firstName")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("First Name.")
    private String firstName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("groups")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Groups.")
    private java.util.List<String> groups;

    public java.util.List<String> getGroups() {
        return groups;
    }

    public void setGroups(java.util.List<String> groups) {
        this.groups = groups;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("id")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("User ID.")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("lastName")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Last Name.")
    private String lastName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("realmRoles")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Realm Roles.")
    private java.util.List<String> realmRoles;

    public java.util.List<String> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(java.util.List<String> realmRoles) {
        this.realmRoles = realmRoles;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("requiredActions")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Required Actions.")
    private java.util.List<String> requiredActions;

    public java.util.List<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(java.util.List<String> requiredActions) {
        this.requiredActions = requiredActions;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("username")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("User Name.")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
