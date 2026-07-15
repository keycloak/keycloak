package org.keycloak.services.client;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

public class SimpleClientModel implements ClientModel {

    // ── Identity ──────────────────────────────────────────────────────────────
    private String id;
    private String clientId;
    private String name;
    private String description;
    private RealmModel realm;

    // ── Flags ─────────────────────────────────────────────────────────────────
    private boolean enabled;
    private boolean alwaysDisplayInConsole;
    private boolean surrogateAuthRequired;
    private boolean bearerOnly;
    private boolean frontchannelLogout;
    private boolean fullScopeAllowed;
    private boolean publicClient;
    private boolean consentRequired;
    private boolean standardFlowEnabled;
    private boolean implicitFlowEnabled;
    private boolean directAccessGrantsEnabled;
    private boolean serviceAccountsEnabled;

    // ── URLs ──────────────────────────────────────────────────────────────────
    private Set<String> webOrigins = new LinkedHashSet<>();
    private Set<String> redirectUris = new LinkedHashSet<>();
    private String managementUrl;
    private String rootUrl;
    private String baseUrl;

    // ── Auth ──────────────────────────────────────────────────────────────────
    private String clientAuthenticatorType;
    private String secret;
    private String registrationToken;
    private String protocol;
    private int nodeReRegistrationTimeout;
    private int notBefore;

    // ── Attributes & flow overrides ───────────────────────────────────────────
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final Map<String, String> authFlowBindingOverrides = new LinkedHashMap<>();

    // ── Scopes ────────────────────────────────────────────────────────────────
    /** key = scope name, value = scope model; true = default, false = optional */
    private final Map<String, ClientScopeModel> defaultClientScopes = new LinkedHashMap<>();
    private final Map<String, ClientScopeModel> optionalClientScopes = new LinkedHashMap<>();

    // ── Protocol mappers ──────────────────────────────────────────────────────
    private final Map<String, ProtocolMapperModel> protocolMappers = new LinkedHashMap<>();

    // ── Registered nodes ──────────────────────────────────────────────────────
    private final Map<String, Integer> registeredNodes = new LinkedHashMap<>();

    // ── Timestamps ────────────────────────────────────────────────────────────
    private Long createdTimestamp;
    private Long lastModifiedTimestamp;

    // =========================================================================
    // ClientModel
    // =========================================================================
    
    public SimpleClientModel(String id, RealmModel realm) {
        this.id = id;
        this.realm = realm;
    }

    @Override public void updateClient() {
        throw new UnsupportedOperationException();
    }

    @Override public String getId() { return id; }

    @Override public String getClientId() { return clientId; }
    @Override public void setClientId(String clientId) { this.clientId = clientId; }

    @Override public String getName() { return name; }
    @Override public void setName(String name) { this.name = name; }

    @Override public String getDescription() { return description; }
    @Override public void setDescription(String description) { this.description = description; }

    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override public boolean isAlwaysDisplayInConsole() { return alwaysDisplayInConsole; }
    @Override public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) { this.alwaysDisplayInConsole = alwaysDisplayInConsole; }

    @Override public boolean isSurrogateAuthRequired() { return surrogateAuthRequired; }
    @Override public void setSurrogateAuthRequired(boolean surrogateAuthRequired) { this.surrogateAuthRequired = surrogateAuthRequired; }

    @Override public Set<String> getWebOrigins() { return webOrigins; }
    @Override public void setWebOrigins(Set<String> webOrigins) { this.webOrigins = webOrigins; }
    @Override public void addWebOrigin(String webOrigin) { webOrigins.add(webOrigin); }
    @Override public void removeWebOrigin(String webOrigin) { webOrigins.remove(webOrigin); }

    @Override public Set<String> getRedirectUris() { return redirectUris; }
    @Override public void setRedirectUris(Set<String> redirectUris) { this.redirectUris = redirectUris; }
    @Override public void addRedirectUri(String redirectUri) { redirectUris.add(redirectUri); }
    @Override public void removeRedirectUri(String redirectUri) { redirectUris.remove(redirectUri); }

    @Override public String getManagementUrl() { return managementUrl; }
    @Override public void setManagementUrl(String url) { this.managementUrl = url; }

    @Override public String getRootUrl() { return rootUrl; }
    @Override public void setRootUrl(String url) { this.rootUrl = url; }

    @Override public String getBaseUrl() { return baseUrl; }
    @Override public void setBaseUrl(String url) { this.baseUrl = url; }

    @Override public boolean isBearerOnly() { return bearerOnly; }
    @Override public void setBearerOnly(boolean only) { this.bearerOnly = only; }

    @Override public int getNodeReRegistrationTimeout() { return nodeReRegistrationTimeout; }
    @Override public void setNodeReRegistrationTimeout(int timeout) { this.nodeReRegistrationTimeout = timeout; }

    @Override public String getClientAuthenticatorType() { return clientAuthenticatorType; }
    @Override public void setClientAuthenticatorType(String clientAuthenticatorType) { this.clientAuthenticatorType = clientAuthenticatorType; }

    @Override public boolean validateSecret(String secret) { return Objects.equals(this.secret, secret); }
    @Override public String getSecret() { return secret; }
    @Override public void setSecret(String secret) { this.secret = secret; }

    @Override public String getRegistrationToken() { return registrationToken; }
    @Override public void setRegistrationToken(String registrationToken) { this.registrationToken = registrationToken; }

    @Override public String getProtocol() { return protocol; }
    @Override public void setProtocol(String protocol) { this.protocol = protocol; }

    @Override public void setAttribute(String name, String value) { attributes.put(name, value); }
    @Override public void removeAttribute(String name) { attributes.remove(name); }
    @Override public String getAttribute(String name) { return attributes.get(name); }
    @Override public Map<String, String> getAttributes() { return attributes; }

    @Override public String getAuthenticationFlowBindingOverride(String binding) { return authFlowBindingOverrides.get(binding); }
    @Override public Map<String, String> getAuthenticationFlowBindingOverrides() { return authFlowBindingOverrides; }
    @Override public void removeAuthenticationFlowBindingOverride(String binding) { authFlowBindingOverrides.remove(binding); }
    @Override public void setAuthenticationFlowBindingOverride(String binding, String flowId) { authFlowBindingOverrides.put(binding, flowId); }

    @Override public boolean isFrontchannelLogout() { return frontchannelLogout; }
    @Override public void setFrontchannelLogout(boolean flag) { this.frontchannelLogout = flag; }

    @Override public boolean isFullScopeAllowed() { return fullScopeAllowed; }
    @Override public void setFullScopeAllowed(boolean value) { this.fullScopeAllowed = value; }

    @Override public boolean isPublicClient() { return publicClient; }
    @Override public void setPublicClient(boolean flag) { this.publicClient = flag; }

    @Override public boolean isConsentRequired() { return consentRequired; }
    @Override public void setConsentRequired(boolean consentRequired) { this.consentRequired = consentRequired; }

    @Override public boolean isStandardFlowEnabled() { return standardFlowEnabled; }
    @Override public void setStandardFlowEnabled(boolean standardFlowEnabled) { this.standardFlowEnabled = standardFlowEnabled; }

    @Override public boolean isImplicitFlowEnabled() { return implicitFlowEnabled; }
    @Override public void setImplicitFlowEnabled(boolean implicitFlowEnabled) { this.implicitFlowEnabled = implicitFlowEnabled; }

    @Override public boolean isDirectAccessGrantsEnabled() { return directAccessGrantsEnabled; }
    @Override public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) { this.directAccessGrantsEnabled = directAccessGrantsEnabled; }

    @Override public boolean isServiceAccountsEnabled() { return serviceAccountsEnabled; }
    @Override public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) { this.serviceAccountsEnabled = serviceAccountsEnabled; }

    @Override public RealmModel getRealm() { return realm; }

    @Override
    public void addClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        if (defaultScope) defaultClientScopes.put(clientScope.getName(), clientScope);
        else optionalClientScopes.put(clientScope.getName(), clientScope);
    }

    @Override
    public void addClientScopes(Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        clientScopes.forEach(cs -> addClientScope(cs, defaultScope));
    }

    @Override
    public void removeClientScope(ClientScopeModel clientScope) {
        defaultClientScopes.remove(clientScope.getName());
        optionalClientScopes.remove(clientScope.getName());
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(boolean defaultScope) {
        return defaultScope ? defaultClientScopes : optionalClientScopes;
    }

    @Override public int getNotBefore() { return notBefore; }
    @Override public void setNotBefore(int notBefore) { this.notBefore = notBefore; }

    @Override public Map<String, Integer> getRegisteredNodes() { return registeredNodes; }

    @Override
    public void registerNode(String nodeHost, int registrationTime) {
        registeredNodes.put(nodeHost, registrationTime);
    }

    @Override
    public void unregisterNode(String nodeHost) {
        registeredNodes.remove(nodeHost);
    }

    @Override public Long getCreatedTimestamp() { return createdTimestamp; }

    @Override public Long getLastModifiedTimestamp() { return lastModifiedTimestamp; }

    // =========================================================================
    // RoleContainerModel
    // =========================================================================

    @Override
    public RoleModel getRole(String name) { throw new UnsupportedOperationException(); }

    @Override
    public RoleModel addRole(String name) { throw new UnsupportedOperationException(); }

    @Override
    public RoleModel addRole(String id, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeRole(RoleModel role) { throw new UnsupportedOperationException(); }

    @Override
    public Stream<RoleModel> getRolesStream() { throw new UnsupportedOperationException(); }

    @Override
    public Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        throw new UnsupportedOperationException();
    }

    // =========================================================================
    // ProtocolMapperContainerModel
    // =========================================================================

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        return protocolMappers.values().stream();
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        protocolMappers.put(model.getId(), model);
        return model;
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        protocolMappers.remove(mapping.getId());
    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        protocolMappers.put(mapping.getId(), mapping);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        return protocolMappers.get(id);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        return protocolMappers.values().stream()
                .filter(m -> Objects.equals(m.getProtocolMapper(), protocol) && Objects.equals(m.getName(), name))
                .findFirst().orElse(null);
    }

    // =========================================================================
    // ScopeContainerModel
    // =========================================================================

    @Override
    public Stream<RoleModel> getScopeMappingsStream() { throw new UnsupportedOperationException(); }

    @Override
    public Stream<RoleModel> getRealmScopeMappingsStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addScopeMapping(RoleModel role) { throw new UnsupportedOperationException(); }

    @Override
    public void deleteScopeMapping(RoleModel role) { throw new UnsupportedOperationException(); }

    @Override
    public boolean hasScope(RoleModel role) {
        throw new UnsupportedOperationException();
    }
}
