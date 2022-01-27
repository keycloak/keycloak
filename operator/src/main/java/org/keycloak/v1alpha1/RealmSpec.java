package org.keycloak.v1alpha1;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"accessTokenLifespan","accessTokenLifespanForImplicitFlow","accountTheme","adminEventsDetailsEnabled","adminEventsEnabled","adminTheme","authenticationFlows","authenticatorConfig","bruteForceProtected","clientScopeMappings","clientScopes","clients","defaultLocale","defaultRole","displayName","displayNameHtml","duplicateEmailsAllowed","editUsernameAllowed","emailTheme","enabled","enabledEventTypes","eventsEnabled","eventsListeners","failureFactor","id","identityProviders","internationalizationEnabled","loginTheme","loginWithEmailAllowed","maxDeltaTimeSeconds","maxFailureWaitSeconds","minimumQuickLoginWaitSeconds","passwordPolicy","permanentLockout","quickLoginCheckMilliSeconds","realm","registrationAllowed","registrationEmailAsUsername","rememberMe","resetPasswordAllowed","roles","scopeMappings","smtpServer","sslRequired","supportedLocales","userFederationMappers","userFederationProviders","userManagedAccessAllowed","users","verifyEmail","waitIncrementSeconds"})
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
public class RealmSpec implements io.fabric8.kubernetes.api.model.KubernetesResource {

    @com.fasterxml.jackson.annotation.JsonProperty("accessTokenLifespan")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Access Token Lifespan")
    private Integer accessTokenLifespan;

    public Integer getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public void setAccessTokenLifespan(Integer accessTokenLifespan) {
        this.accessTokenLifespan = accessTokenLifespan;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("accessTokenLifespanForImplicitFlow")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Access Token Lifespan For Implicit Flow")
    private Integer accessTokenLifespanForImplicitFlow;

    public Integer getAccessTokenLifespanForImplicitFlow() {
        return accessTokenLifespanForImplicitFlow;
    }

    public void setAccessTokenLifespanForImplicitFlow(Integer accessTokenLifespanForImplicitFlow) {
        this.accessTokenLifespanForImplicitFlow = accessTokenLifespanForImplicitFlow;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("accountTheme")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Account Theme")
    private String accountTheme;

    public String getAccountTheme() {
        return accountTheme;
    }

    public void setAccountTheme(String accountTheme) {
        this.accountTheme = accountTheme;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("adminEventsDetailsEnabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Enable admin events details TODO: change to values and use kubebuilder default annotation once supported")
    private Boolean adminEventsDetailsEnabled;

    public Boolean getAdminEventsDetailsEnabled() {
        return adminEventsDetailsEnabled;
    }

    public void setAdminEventsDetailsEnabled(Boolean adminEventsDetailsEnabled) {
        this.adminEventsDetailsEnabled = adminEventsDetailsEnabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("adminEventsEnabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Enable events recording TODO: change to values and use kubebuilder default annotation once supported")
    private Boolean adminEventsEnabled;

    public Boolean getAdminEventsEnabled() {
        return adminEventsEnabled;
    }

    public void setAdminEventsEnabled(Boolean adminEventsEnabled) {
        this.adminEventsEnabled = adminEventsEnabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("adminTheme")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Admin Console Theme")
    private String adminTheme;

    public String getAdminTheme() {
        return adminTheme;
    }

    public void setAdminTheme(String adminTheme) {
        this.adminTheme = adminTheme;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("authenticationFlows")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Authentication flows")
    private java.util.List<AuthenticationFlowsSpec> authenticationFlows;

    public java.util.List<AuthenticationFlowsSpec> getAuthenticationFlows() {
        return authenticationFlows;
    }

    public void setAuthenticationFlows(java.util.List<AuthenticationFlowsSpec> authenticationFlows) {
        this.authenticationFlows = authenticationFlows;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("authenticatorConfig")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Authenticator config")
    private java.util.List<AuthenticatorConfigSpec> authenticatorConfig;

    public java.util.List<AuthenticatorConfigSpec> getAuthenticatorConfig() {
        return authenticatorConfig;
    }

    public void setAuthenticatorConfig(java.util.List<AuthenticatorConfigSpec> authenticatorConfig) {
        this.authenticatorConfig = authenticatorConfig;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("bruteForceProtected")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Brute Force Detection")
    private Boolean bruteForceProtected;

    public Boolean getBruteForceProtected() {
        return bruteForceProtected;
    }

    public void setBruteForceProtected(Boolean bruteForceProtected) {
        this.bruteForceProtected = bruteForceProtected;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("clientScopeMappings")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client Scope Mappings")
    private java.util.Map<java.lang.String, java.util.List<ClientScopeMappingsSpec>> clientScopeMappings;

    public java.util.Map<java.lang.String, java.util.List<ClientScopeMappingsSpec>> getClientScopeMappings() {
        return clientScopeMappings;
    }

    public void setClientScopeMappings(java.util.Map<java.lang.String, java.util.List<ClientScopeMappingsSpec>> clientScopeMappings) {
        this.clientScopeMappings = clientScopeMappings;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("clientScopes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Client scopes")
    private java.util.List<ClientScopesSpec> clientScopes;

    public java.util.List<ClientScopesSpec> getClientScopes() {
        return clientScopes;
    }

    public void setClientScopes(java.util.List<ClientScopesSpec> clientScopes) {
        this.clientScopes = clientScopes;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("clients")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Keycloak Clients.")
    private java.util.List<ClientsSpec> clients;

    public java.util.List<ClientsSpec> getClients() {
        return clients;
    }

    public void setClients(java.util.List<ClientsSpec> clients) {
        this.clients = clients;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("defaultLocale")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Default Locale")
    private String defaultLocale;

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("defaultRole")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Default role")
    private DefaultRoleSpec defaultRole;

    public DefaultRoleSpec getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(DefaultRoleSpec defaultRole) {
        this.defaultRole = defaultRole;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("displayName")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Realm display name.")
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("displayNameHtml")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Realm HTML display name.")
    private String displayNameHtml;

    public String getDisplayNameHtml() {
        return displayNameHtml;
    }

    public void setDisplayNameHtml(String displayNameHtml) {
        this.displayNameHtml = displayNameHtml;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("duplicateEmailsAllowed")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Duplicate emails")
    private Boolean duplicateEmailsAllowed;

    public Boolean getDuplicateEmailsAllowed() {
        return duplicateEmailsAllowed;
    }

    public void setDuplicateEmailsAllowed(Boolean duplicateEmailsAllowed) {
        this.duplicateEmailsAllowed = duplicateEmailsAllowed;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("editUsernameAllowed")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Edit username")
    private Boolean editUsernameAllowed;

    public Boolean getEditUsernameAllowed() {
        return editUsernameAllowed;
    }

    public void setEditUsernameAllowed(Boolean editUsernameAllowed) {
        this.editUsernameAllowed = editUsernameAllowed;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("emailTheme")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Email Theme")
    private String emailTheme;

    public String getEmailTheme() {
        return emailTheme;
    }

    public void setEmailTheme(String emailTheme) {
        this.emailTheme = emailTheme;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("enabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Realm enabled flag.")
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("enabledEventTypes")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Enabled event types")
    private java.util.List<String> enabledEventTypes;

    public java.util.List<String> getEnabledEventTypes() {
        return enabledEventTypes;
    }

    public void setEnabledEventTypes(java.util.List<String> enabledEventTypes) {
        this.enabledEventTypes = enabledEventTypes;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("eventsEnabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Enable events recording TODO: change to values and use kubebuilder default annotation once supported")
    private Boolean eventsEnabled;

    public Boolean getEventsEnabled() {
        return eventsEnabled;
    }

    public void setEventsEnabled(Boolean eventsEnabled) {
        this.eventsEnabled = eventsEnabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("eventsListeners")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Event Listeners.")
    private java.util.List<String> eventsListeners;

    public java.util.List<String> getEventsListeners() {
        return eventsListeners;
    }

    public void setEventsListeners(java.util.List<String> eventsListeners) {
        this.eventsListeners = eventsListeners;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("failureFactor")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Max Login Failures")
    private Integer failureFactor;

    public Integer getFailureFactor() {
        return failureFactor;
    }

    public void setFailureFactor(Integer failureFactor) {
        this.failureFactor = failureFactor;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("identityProviders")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Identity Providers.")
    private java.util.List<IdentityProvidersSpec> identityProviders;

    public java.util.List<IdentityProvidersSpec> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(java.util.List<IdentityProvidersSpec> identityProviders) {
        this.identityProviders = identityProviders;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("internationalizationEnabled")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Internationalization Enabled")
    private Boolean internationalizationEnabled;

    public Boolean getInternationalizationEnabled() {
        return internationalizationEnabled;
    }

    public void setInternationalizationEnabled(Boolean internationalizationEnabled) {
        this.internationalizationEnabled = internationalizationEnabled;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("loginTheme")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Login Theme")
    private String loginTheme;

    public String getLoginTheme() {
        return loginTheme;
    }

    public void setLoginTheme(String loginTheme) {
        this.loginTheme = loginTheme;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("loginWithEmailAllowed")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Login with email")
    private Boolean loginWithEmailAllowed;

    public Boolean getLoginWithEmailAllowed() {
        return loginWithEmailAllowed;
    }

    public void setLoginWithEmailAllowed(Boolean loginWithEmailAllowed) {
        this.loginWithEmailAllowed = loginWithEmailAllowed;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("maxDeltaTimeSeconds")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Failure Reset Time")
    private Integer maxDeltaTimeSeconds;

    public Integer getMaxDeltaTimeSeconds() {
        return maxDeltaTimeSeconds;
    }

    public void setMaxDeltaTimeSeconds(Integer maxDeltaTimeSeconds) {
        this.maxDeltaTimeSeconds = maxDeltaTimeSeconds;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("maxFailureWaitSeconds")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Max Wait")
    private Integer maxFailureWaitSeconds;

    public Integer getMaxFailureWaitSeconds() {
        return maxFailureWaitSeconds;
    }

    public void setMaxFailureWaitSeconds(Integer maxFailureWaitSeconds) {
        this.maxFailureWaitSeconds = maxFailureWaitSeconds;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("minimumQuickLoginWaitSeconds")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Minimum Quick Login Wait")
    private Integer minimumQuickLoginWaitSeconds;

    public Integer getMinimumQuickLoginWaitSeconds() {
        return minimumQuickLoginWaitSeconds;
    }

    public void setMinimumQuickLoginWaitSeconds(Integer minimumQuickLoginWaitSeconds) {
        this.minimumQuickLoginWaitSeconds = minimumQuickLoginWaitSeconds;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("passwordPolicy")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Realm Password Policy")
    private String passwordPolicy;

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("permanentLockout")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Permanent Lockout")
    private Boolean permanentLockout;

    public Boolean getPermanentLockout() {
        return permanentLockout;
    }

    public void setPermanentLockout(Boolean permanentLockout) {
        this.permanentLockout = permanentLockout;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("quickLoginCheckMilliSeconds")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Quick Login Check Milli Seconds")
    private Long quickLoginCheckMilliSeconds;

    public Long getQuickLoginCheckMilliSeconds() {
        return quickLoginCheckMilliSeconds;
    }

    public void setQuickLoginCheckMilliSeconds(Long quickLoginCheckMilliSeconds) {
        this.quickLoginCheckMilliSeconds = quickLoginCheckMilliSeconds;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("realm")
    @javax.validation.constraints.NotNull()
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Realm name.")
    private String realm;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("registrationAllowed")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("User registration")
    private Boolean registrationAllowed;

    public Boolean getRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(Boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("registrationEmailAsUsername")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Email as username")
    private Boolean registrationEmailAsUsername;

    public Boolean getRegistrationEmailAsUsername() {
        return registrationEmailAsUsername;
    }

    public void setRegistrationEmailAsUsername(Boolean registrationEmailAsUsername) {
        this.registrationEmailAsUsername = registrationEmailAsUsername;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("rememberMe")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Remember me")
    private Boolean rememberMe;

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("resetPasswordAllowed")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Forgot password")
    private Boolean resetPasswordAllowed;

    public Boolean getResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(Boolean resetPasswordAllowed) {
        this.resetPasswordAllowed = resetPasswordAllowed;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("roles")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Roles")
    private RolesSpec roles;

    public RolesSpec getRoles() {
        return roles;
    }

    public void setRoles(RolesSpec roles) {
        this.roles = roles;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("scopeMappings")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Scope Mappings")
    private java.util.List<ScopeMappingsSpec> scopeMappings;

    public java.util.List<ScopeMappingsSpec> getScopeMappings() {
        return scopeMappings;
    }

    public void setScopeMappings(java.util.List<ScopeMappingsSpec> scopeMappings) {
        this.scopeMappings = scopeMappings;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("smtpServer")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Email")
    private java.util.Map<java.lang.String, String> smtpServer;

    public java.util.Map<java.lang.String, String> getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(java.util.Map<java.lang.String, String> smtpServer) {
        this.smtpServer = smtpServer;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("sslRequired")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Require SSL")
    private String sslRequired;

    public String getSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(String sslRequired) {
        this.sslRequired = sslRequired;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("supportedLocales")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Supported Locales")
    private java.util.List<String> supportedLocales;

    public java.util.List<String> getSupportedLocales() {
        return supportedLocales;
    }

    public void setSupportedLocales(java.util.List<String> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("userFederationMappers")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("User federation mappers are extension points triggered by the user federation at various points.")
    private java.util.List<UserFederationMappersSpec> userFederationMappers;

    public java.util.List<UserFederationMappersSpec> getUserFederationMappers() {
        return userFederationMappers;
    }

    public void setUserFederationMappers(java.util.List<UserFederationMappersSpec> userFederationMappers) {
        this.userFederationMappers = userFederationMappers;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("userFederationProviders")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Point keycloak to an external user provider to validate credentials or pull in identity information.")
    private java.util.List<UserFederationProvidersSpec> userFederationProviders;

    public java.util.List<UserFederationProvidersSpec> getUserFederationProviders() {
        return userFederationProviders;
    }

    public void setUserFederationProviders(java.util.List<UserFederationProvidersSpec> userFederationProviders) {
        this.userFederationProviders = userFederationProviders;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("userManagedAccessAllowed")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("User Managed Access Allowed")
    private Boolean userManagedAccessAllowed;

    public Boolean getUserManagedAccessAllowed() {
        return userManagedAccessAllowed;
    }

    public void setUserManagedAccessAllowed(Boolean userManagedAccessAllowed) {
        this.userManagedAccessAllowed = userManagedAccessAllowed;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("users")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("A set of Keycloak Users.")
    private java.util.List<UsersSpec> users;

    public java.util.List<UsersSpec> getUsers() {
        return users;
    }

    public void setUsers(java.util.List<UsersSpec> users) {
        this.users = users;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("verifyEmail")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Verify email")
    private Boolean verifyEmail;

    public Boolean getVerifyEmail() {
        return verifyEmail;
    }

    public void setVerifyEmail(Boolean verifyEmail) {
        this.verifyEmail = verifyEmail;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("waitIncrementSeconds")
    @com.fasterxml.jackson.annotation.JsonPropertyDescription("Wait Increment")
    private Integer waitIncrementSeconds;

    public Integer getWaitIncrementSeconds() {
        return waitIncrementSeconds;
    }

    public void setWaitIncrementSeconds(Integer waitIncrementSeconds) {
        this.waitIncrementSeconds = waitIncrementSeconds;
    }
}
