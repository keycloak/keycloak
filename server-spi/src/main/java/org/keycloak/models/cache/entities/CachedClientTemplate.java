package org.keycloak.models.cache.entities;

import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.RealmCache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedClientTemplate implements Serializable {

    private String id;
    private String name;
    private String description;
    private String realm;
    private String protocol;
    private boolean fullScopeAllowed;
    private boolean publicClient;
    private boolean frontchannelLogout;
    private boolean bearerOnly;
    private boolean consentRequired;
    private boolean standardFlowEnabled;
    private boolean implicitFlowEnabled;
    private boolean directAccessGrantsEnabled;
    private boolean serviceAccountsEnabled;
    private Set<String> scope = new HashSet<String>();
    private Set<ProtocolMapperModel> protocolMappers = new HashSet<ProtocolMapperModel>();
    private Map<String, String> attributes = new HashMap<String, String>();

    public CachedClientTemplate(RealmCache cache, RealmProvider delegate, RealmModel realm, ClientTemplateModel model) {
        id = model.getId();
        name = model.getName();
        description = model.getDescription();
        this.realm = realm.getId();
        protocol = model.getProtocol();
        fullScopeAllowed = model.isFullScopeAllowed();
        for (ProtocolMapperModel mapper : model.getProtocolMappers()) {
            this.protocolMappers.add(mapper);
        }
        for (RoleModel role : model.getScopeMappings())  {
            scope.add(role.getId());
        }
        attributes.putAll(model.getAttributes());
        frontchannelLogout = model.isFrontchannelLogout();
        publicClient = model.isPublicClient();
        bearerOnly = model.isBearerOnly();
        consentRequired = model.isConsentRequired();
        standardFlowEnabled = model.isStandardFlowEnabled();
        implicitFlowEnabled = model.isImplicitFlowEnabled();
        directAccessGrantsEnabled = model.isDirectAccessGrantsEnabled();
        serviceAccountsEnabled = model.isServiceAccountsEnabled();
    }
    public String getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getRealm() {
        return realm;
    }
    public Set<ProtocolMapperModel> getProtocolMappers() {
        return protocolMappers;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public Set<String> getScope() {
        return scope;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public boolean isStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public boolean isImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public boolean isDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
