package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"access","adminUrl","attributes","authenticationFlowBindingOverrides","authorizationServicesEnabled","authorizationSettings","baseUrl","bearerOnly","clientAuthenticatorType","clientId","consentRequired","defaultClientScopes","defaultRoles","directAccessGrantsEnabled","enabled","frontchannelLogout","fullScopeAllowed","id","implicitFlowEnabled","name","nodeReRegistrationTimeout","notBefore","optionalClientScopes","protocol","protocolMappers","publicClient","redirectUris","rootUrl","secret","serviceAccountsEnabled","standardFlowEnabled","surrogateAuthRequired","useTemplateConfig","useTemplateMappers","useTemplateScope","webOrigins"})
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
public class ClientsSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("access")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Access options.")
    private java.util.Map<java.lang.String, Boolean> access;

    public java.util.Map<java.lang.String, Boolean> getAccess() {
        return access;
    }

    public void setAccess(java.util.Map<java.lang.String, Boolean> access) {
        this.access = access;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("adminUrl")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Application Admin URL.")
    private String adminUrl;

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("attributes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client Attributes.")
    private java.util.Map<java.lang.String, String> attributes;

    public java.util.Map<java.lang.String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(java.util.Map<java.lang.String, String> attributes) {
        this.attributes = attributes;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("authenticationFlowBindingOverrides")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Authentication Flow Binding Overrides.")
    private java.util.Map<java.lang.String, String> authenticationFlowBindingOverrides;

    public java.util.Map<java.lang.String, String> getAuthenticationFlowBindingOverrides() {
        return authenticationFlowBindingOverrides;
    }

    public void setAuthenticationFlowBindingOverrides(java.util.Map<java.lang.String, String> authenticationFlowBindingOverrides) {
        this.authenticationFlowBindingOverrides = authenticationFlowBindingOverrides;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("authorizationServicesEnabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if fine-grained authorization support is enabled for this client.")
    private Boolean authorizationServicesEnabled;

    public Boolean getAuthorizationServicesEnabled() {
        return authorizationServicesEnabled;
    }

    public void setAuthorizationServicesEnabled(Boolean authorizationServicesEnabled) {
        this.authorizationServicesEnabled = authorizationServicesEnabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("authorizationSettings")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Authorization settings for this resource server.")
    private AuthorizationSettingsSpec authorizationSettings;

    public AuthorizationSettingsSpec getAuthorizationSettings() {
        return authorizationSettings;
    }

    public void setAuthorizationSettings(AuthorizationSettingsSpec authorizationSettings) {
        this.authorizationSettings = authorizationSettings;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("baseUrl")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Application base URL.")
    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("bearerOnly")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if a client supports only Bearer Tokens.")
    private Boolean bearerOnly;

    public Boolean getBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(Boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("clientAuthenticatorType")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("What Client authentication type to use.")
    private String clientAuthenticatorType;

    public String getClientAuthenticatorType() {
        return clientAuthenticatorType;
    }

    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        this.clientAuthenticatorType = clientAuthenticatorType;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("clientId")
    @javax.validation.constraints.NotNull()
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client ID.")
    private String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("consentRequired")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if Consent Screen is required.")
    private Boolean consentRequired;

    public Boolean getConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(Boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("defaultClientScopes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A list of default client scopes. Default client scopes are always applied when issuing OpenID Connect tokens or SAML assertions for this client.")
    private java.util.List<String> defaultClientScopes;

    public java.util.List<String> getDefaultClientScopes() {
        return defaultClientScopes;
    }

    public void setDefaultClientScopes(java.util.List<String> defaultClientScopes) {
        this.defaultClientScopes = defaultClientScopes;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("defaultRoles")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Default Client roles.")
    private java.util.List<String> defaultRoles;

    public java.util.List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(java.util.List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("directAccessGrantsEnabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if Direct Grant is enabled.")
    private Boolean directAccessGrantsEnabled;

    public Boolean getDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public void setDirectAccessGrantsEnabled(Boolean directAccessGrantsEnabled) {
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("enabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client enabled flag.")
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("frontchannelLogout")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if this client supports Front Channel logout.")
    private Boolean frontchannelLogout;

    public Boolean getFrontchannelLogout() {
        return frontchannelLogout;
    }

    public void setFrontchannelLogout(Boolean frontchannelLogout) {
        this.frontchannelLogout = frontchannelLogout;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("fullScopeAllowed")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if Full Scope is allowed.")
    private Boolean fullScopeAllowed;

    public Boolean getFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(Boolean fullScopeAllowed) {
        this.fullScopeAllowed = fullScopeAllowed;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("id")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client ID. If not specified, automatically generated.")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("implicitFlowEnabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if Implicit flow is enabled.")
    private Boolean implicitFlowEnabled;

    public Boolean getImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public void setImplicitFlowEnabled(Boolean implicitFlowEnabled) {
        this.implicitFlowEnabled = implicitFlowEnabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("name")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client name.")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("nodeReRegistrationTimeout")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Node registration timeout.")
    private Long nodeReRegistrationTimeout;

    public Long getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public void setNodeReRegistrationTimeout(Long nodeReRegistrationTimeout) {
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("notBefore")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Not Before setting.")
    private Long notBefore;

    public Long getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Long notBefore) {
        this.notBefore = notBefore;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("optionalClientScopes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A list of optional client scopes. Optional client scopes are applied when issuing tokens for this client, but only when they are requested by the scope parameter in the OpenID Connect authorization request.")
    private java.util.List<String> optionalClientScopes;

    public java.util.List<String> getOptionalClientScopes() {
        return optionalClientScopes;
    }

    public void setOptionalClientScopes(java.util.List<String> optionalClientScopes) {
        this.optionalClientScopes = optionalClientScopes;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("protocol")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Protocol used for this Client.")
    private String protocol;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("protocolMappers")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Protocol Mappers.")
    private java.util.List<ProtocolMappersSpec0> protocolMappers;

    public java.util.List<ProtocolMappersSpec0> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(java.util.List<ProtocolMappersSpec0> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("publicClient")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if this is a public Client.")
    private Boolean publicClient;

    public Boolean getPublicClient() {
        return publicClient;
    }

    public void setPublicClient(Boolean publicClient) {
        this.publicClient = publicClient;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("redirectUris")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A list of valid Redirection URLs.")
    private java.util.List<String> redirectUris;

    public java.util.List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(java.util.List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("rootUrl")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Application root URL.")
    private String rootUrl;

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("secret")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client Secret. The Operator will automatically create a Secret based on this value.")
    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("serviceAccountsEnabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if Service Accounts are enabled.")
    private Boolean serviceAccountsEnabled;

    public Boolean getServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public void setServiceAccountsEnabled(Boolean serviceAccountsEnabled) {
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("standardFlowEnabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True if Standard flow is enabled.")
    private Boolean standardFlowEnabled;

    public Boolean getStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public void setStandardFlowEnabled(Boolean standardFlowEnabled) {
        this.standardFlowEnabled = standardFlowEnabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("surrogateAuthRequired")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Surrogate Authentication Required option.")
    private Boolean surrogateAuthRequired;

    public Boolean getSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(Boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("useTemplateConfig")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True to use a Template Config.")
    private Boolean useTemplateConfig;

    public Boolean getUseTemplateConfig() {
        return useTemplateConfig;
    }

    public void setUseTemplateConfig(Boolean useTemplateConfig) {
        this.useTemplateConfig = useTemplateConfig;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("useTemplateMappers")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True to use Template Mappers.")
    private Boolean useTemplateMappers;

    public Boolean getUseTemplateMappers() {
        return useTemplateMappers;
    }

    public void setUseTemplateMappers(Boolean useTemplateMappers) {
        this.useTemplateMappers = useTemplateMappers;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("useTemplateScope")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("True to use Template Scope.")
    private Boolean useTemplateScope;

    public Boolean getUseTemplateScope() {
        return useTemplateScope;
    }

    public void setUseTemplateScope(Boolean useTemplateScope) {
        this.useTemplateScope = useTemplateScope;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("webOrigins")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A list of valid Web Origins.")
    private java.util.List<String> webOrigins;

    public java.util.List<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(java.util.List<String> webOrigins) {
        this.webOrigins = webOrigins;
    }
}
