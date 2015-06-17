package org.keycloak.models.cache.entities;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.RealmCache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedClient implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String clientId;
    private String name;
    private String realm;
    private Set<String> redirectUris = new HashSet<String>();
    private boolean enabled;
    private String secret;
    private String protocol;
    private Map<String, String> attributes = new HashMap<String, String>();
    private boolean publicClient;
    private boolean fullScopeAllowed;
    private boolean directGrantsOnly;
    private boolean frontchannelLogout;
    private int notBefore;
    private Set<String> scope = new HashSet<String>();
    private Set<String> webOrigins = new HashSet<String>();
    private Set<ProtocolMapperModel> protocolMappers = new HashSet<ProtocolMapperModel>();
    private boolean surrogateAuthRequired;
    private String managementUrl;
    private String baseUrl;
    private List<String> defaultRoles = new LinkedList<String>();
    private boolean bearerOnly;
    private boolean consentRequired;
    private Map<String, String> roles = new HashMap<String, String>();
    private int nodeReRegistrationTimeout;
    private Map<String, Integer> registeredNodes;

    public CachedClient(RealmCache cache, RealmProvider delegate, RealmModel realm, ClientModel model) {
        id = model.getId();
        secret = model.getSecret();
        clientId = model.getClientId();
        name = model.getName();
        this.realm = realm.getId();
        enabled = model.isEnabled();
        protocol = model.getProtocol();
        attributes.putAll(model.getAttributes());
        notBefore = model.getNotBefore();
        directGrantsOnly = model.isDirectGrantsOnly();
        frontchannelLogout = model.isFrontchannelLogout();
        publicClient = model.isPublicClient();
        fullScopeAllowed = model.isFullScopeAllowed();
        redirectUris.addAll(model.getRedirectUris());
        webOrigins.addAll(model.getWebOrigins());
        for (RoleModel role : model.getScopeMappings())  {
            scope.add(role.getId());
        }
        for (ProtocolMapperModel mapper : model.getProtocolMappers()) {
            this.protocolMappers.add(mapper);
        }
        surrogateAuthRequired = model.isSurrogateAuthRequired();
        managementUrl = model.getManagementUrl();
        baseUrl = model.getBaseUrl();
        defaultRoles.addAll(model.getDefaultRoles());
        bearerOnly = model.isBearerOnly();
        consentRequired = model.isConsentRequired();
        for (RoleModel role : model.getRoles()) {
            roles.put(role.getName(), role.getId());
            cache.addCachedRole(new CachedClientRole(id, role, realm));
        }

        nodeReRegistrationTimeout = model.getNodeReRegistrationTimeout();
        registeredNodes = new TreeMap<String, Integer>(model.getRegisteredNodes());
    }
    public String getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public String getRealm() {
        return realm;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSecret() {
        return secret;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public boolean isDirectGrantsOnly() {
        return directGrantsOnly;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public Set<String> getScope() {
        return scope;
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public String getProtocol() {
        return protocol;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public Set<ProtocolMapperModel> getProtocolMappers() {
        return protocolMappers;
    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public Map<String, String> getRoles() {
        return roles;
    }

    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public Map<String, Integer> getRegisteredNodes() {
        return registeredNodes;
    }
}
