package org.keycloak.models.cache.entities;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.RealmCache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedClient {
    protected String id;
    protected String name;
    protected String realm;
    protected long allowedClaimsMask;
    protected Set<String> redirectUris = new HashSet<String>();
    protected boolean enabled;
    protected String secret;
    protected String protocol;
    protected Map<String, String> attributes = new HashMap<String, String>();
    protected boolean publicClient;
    protected boolean fullScopeAllowed;
    protected boolean directGrantsOnly;
    protected int notBefore;
    protected Set<String> scope = new HashSet<String>();
    protected Set<String> webOrigins = new HashSet<String>();

    public CachedClient(RealmCache cache, RealmProvider delegate, RealmModel realm, ClientModel model) {
        id = model.getId();
        secret = model.getSecret();
        name = model.getClientId();
        this.realm = realm.getId();
        enabled = model.isEnabled();
        protocol = model.getProtocol();
        attributes.putAll(model.getAttributes());
        notBefore = model.getNotBefore();
        directGrantsOnly = model.isDirectGrantsOnly();
        publicClient = model.isPublicClient();
        allowedClaimsMask = model.getAllowedClaimsMask();
        fullScopeAllowed = model.isFullScopeAllowed();
        redirectUris.addAll(model.getRedirectUris());
        webOrigins.addAll(model.getWebOrigins());
        for (RoleModel role : model.getScopeMappings())  {
            scope.add(role.getId());
        }

    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRealm() {
        return realm;
    }

    public long getAllowedClaimsMask() {
        return allowedClaimsMask;
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
}
