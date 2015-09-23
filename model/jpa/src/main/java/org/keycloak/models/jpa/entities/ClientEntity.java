package org.keycloak.models.jpa.entities;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Table(name="CLIENT", uniqueConstraints = {@UniqueConstraint(columnNames = {"REALM_ID", "CLIENT_ID"})})
public class ClientEntity {

    @Id
    @Column(name="ID", length = 36)
    private String id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "CLIENT_ID")
    private String clientId;
    @Column(name="ENABLED")
    private boolean enabled;
    @Column(name="SECRET")
    private String secret;
    @Column(name="CLIENT_AUTHENTICATOR_TYPE")
    private String clientAuthenticatorType;
    @Column(name="NOT_BEFORE")
    private int notBefore;
    @Column(name="PUBLIC_CLIENT")
    private boolean publicClient;
    @Column(name="PROTOCOL")
    private String protocol;
    @Column(name="FRONTCHANNEL_LOGOUT")
    private boolean frontchannelLogout;
    @Column(name="FULL_SCOPE_ALLOWED")
    private boolean fullScopeAllowed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @ElementCollection
    @Column(name="VALUE")
    @CollectionTable(name = "WEB_ORIGINS", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    protected Set<String> webOrigins = new HashSet<String>();

    @ElementCollection
    @Column(name="VALUE")
    @CollectionTable(name = "REDIRECT_URIS", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    protected Set<String> redirectUris = new HashSet<String>();

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", length = 2048)
    @CollectionTable(name="CLIENT_ATTRIBUTES", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    protected Map<String, String> attributes = new HashMap<String, String>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "client", cascade = CascadeType.REMOVE)
    Collection<ClientIdentityProviderMappingEntity> identityProviders = new ArrayList<ClientIdentityProviderMappingEntity>();

    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "client")
    Collection<ProtocolMapperEntity> protocolMappers = new ArrayList<ProtocolMapperEntity>();

    @Column(name="SURROGATE_AUTH_REQUIRED")
    private boolean surrogateAuthRequired;

    @Column(name="BASE_URL")
    private String baseUrl;

    @Column(name="MANAGEMENT_URL")
    private String managementUrl;

    @Column(name="DIRECT_GRANTS_ONLY")
    protected boolean directGrantsOnly;

    @Column(name="BEARER_ONLY")
    private boolean bearerOnly;

    @Column(name="CONSENT_REQUIRED")
    private boolean consentRequired;

    @Column(name="SERVICE_ACCOUNTS_ENABLED")
    private boolean serviceAccountsEnabled;

    @Column(name="NODE_REREG_TIMEOUT")
    private int nodeReRegistrationTimeout;

    @OneToMany(fetch = FetchType.EAGER, cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "client")
    Collection<RoleEntity> roles = new ArrayList<RoleEntity>();

    @OneToMany(fetch = FetchType.LAZY, cascade ={CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name="CLIENT_DEFAULT_ROLES", joinColumns = { @JoinColumn(name="CLIENT_ID")}, inverseJoinColumns = { @JoinColumn(name="ROLE_ID")})
    Collection<RoleEntity> defaultRoles = new ArrayList<RoleEntity>();

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE")
    @CollectionTable(name="CLIENT_NODE_REGISTRATIONS", joinColumns={ @JoinColumn(name="CLIENT_ID") })
    Map<String, Integer> registeredNodes = new HashMap<String, Integer>();

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(Set<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getClientAuthenticatorType() {
        return clientAuthenticatorType;
    }

    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        this.clientAuthenticatorType = clientAuthenticatorType;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(boolean fullScopeAllowed) {
        this.fullScopeAllowed = fullScopeAllowed;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public void setFrontchannelLogout(boolean frontchannelLogout) {
        this.frontchannelLogout = frontchannelLogout;
    }

    public Collection<ClientIdentityProviderMappingEntity> getIdentityProviders() {
        return this.identityProviders;
    }

    public void setIdentityProviders(Collection<ClientIdentityProviderMappingEntity> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public Collection<ProtocolMapperEntity> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(Collection<ProtocolMapperEntity> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl = managementUrl;
    }

    public Collection<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Collection<RoleEntity> roles) {
        this.roles = roles;
    }

    public Collection<RoleEntity> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(Collection<RoleEntity> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    public boolean isDirectGrantsOnly() {
        return directGrantsOnly;
    }

    public void setDirectGrantsOnly(boolean directGrantsOnly) {
        this.directGrantsOnly = directGrantsOnly;
    }

    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public void setNodeReRegistrationTimeout(int nodeReRegistrationTimeout) {
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
    }

    public Map<String, Integer> getRegisteredNodes() {
        return registeredNodes;
    }

    public void setRegisteredNodes(Map<String, Integer> registeredNodes) {
        this.registeredNodes = registeredNodes;
    }
}
