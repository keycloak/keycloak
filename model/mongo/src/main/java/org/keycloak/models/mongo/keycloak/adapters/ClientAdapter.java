package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.entities.ClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.utils.MongoModelUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class ClientAdapter<T extends MongoIdentifiableEntity> extends AbstractMongoAdapter<T> implements ClientModel {

    protected final T clientEntity;
    private final RealmModel realm;
    protected  KeycloakSession session;
    private final RealmProvider model;

    public ClientAdapter(KeycloakSession session, RealmModel realm, T clientEntity, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.clientEntity = clientEntity;
        this.realm = realm;
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public T getMongoEntity() {
        return clientEntity;
    }

    // ClientEntity doesn't extend MongoIdentifiableEntity
    public ClientEntity getMongoEntityAsClient() {
        return (ClientEntity)getMongoEntity();
    }

    @Override
    public String getId() {
        return getMongoEntity().getId();
    }

    @Override
    public String getClientId() {
        return getMongoEntityAsClient().getName();
    }

    @Override
    public long getAllowedClaimsMask() {
        return getMongoEntityAsClient().getAllowedClaimsMask();
    }

    @Override
    public void setAllowedClaimsMask(long mask) {
        getMongoEntityAsClient().setAllowedClaimsMask(mask);
        updateMongoEntity();
    }

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        if (getMongoEntityAsClient().getWebOrigins() != null) {
            result.addAll(getMongoEntityAsClient().getWebOrigins());
        }
        return result;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        List<String> result = new ArrayList<String>();
        result.addAll(webOrigins);
        getMongoEntityAsClient().setWebOrigins(result);
        updateMongoEntity();
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        getMongoStore().pushItemToList(clientEntity, "webOrigins", webOrigin, true, invocationContext);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        getMongoStore().pullItemFromList(clientEntity, "webOrigins", webOrigin, invocationContext);
    }

    @Override
    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<String>();
        if (getMongoEntityAsClient().getRedirectUris() != null) {
            result.addAll(getMongoEntityAsClient().getRedirectUris());
        }
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        List<String> result = new ArrayList<String>();
        result.addAll(redirectUris);
        getMongoEntityAsClient().setRedirectUris(result);
        updateMongoEntity();
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        getMongoStore().pushItemToList(clientEntity, "redirectUris", redirectUri, true, invocationContext);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        getMongoStore().pullItemFromList(clientEntity, "redirectUris", redirectUri, invocationContext);
    }

    @Override
    public boolean isEnabled() {
        return getMongoEntityAsClient().isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getMongoEntityAsClient().setEnabled(enabled);
        updateMongoEntity();
    }

    @Override
    public boolean validateSecret(String secret) {
        return secret.equals(getMongoEntityAsClient().getSecret());
    }

    @Override
    public String getSecret() {
        return getMongoEntityAsClient().getSecret();
    }

    @Override
    public void setSecret(String secret) {
        getMongoEntityAsClient().setSecret(secret);
        updateMongoEntity();
    }

    @Override
    public boolean isPublicClient() {
        return getMongoEntityAsClient().isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        getMongoEntityAsClient().setPublicClient(flag);
        updateMongoEntity();
    }

    @Override
    public boolean isFullScopeAllowed() {
        return getMongoEntityAsClient().isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        getMongoEntityAsClient().setFullScopeAllowed(value);
        updateMongoEntity();

    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public int getNotBefore() {
        return getMongoEntityAsClient().getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        getMongoEntityAsClient().setNotBefore(notBefore);
        updateMongoEntity();
    }

    @Override
    public Set<RoleModel> getScopeMappings() {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<MongoRoleEntity> roles = MongoModelUtils.getAllScopesOfClient(this, invocationContext);

        for (MongoRoleEntity role : roles) {
            if (realm.getId().equals(role.getRealmId())) {
                result.add(new RoleAdapter(session, realm, role, realm, invocationContext));
            } else {
                // Likely applicationRole, but we don't have this application yet
                result.add(new RoleAdapter(session, realm, role, invocationContext));
            }
        }
        return result;
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings() {
        Set<RoleModel> allScopes = getScopeMappings();

        // Filter to retrieve just realm roles TODO: Maybe improve to avoid filter programmatically... Maybe have separate fields for realmRoles and appRoles on user?
        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : allScopes) {
            MongoRoleEntity roleEntity = ((RoleAdapter) role).getRole();

            if (realm.getId().equals(roleEntity.getRealmId())) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (isFullScopeAllowed()) return true;
        Set<RoleModel> roles = getScopeMappings();
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }


    @Override
    public void addScopeMapping(RoleModel role) {
        getMongoStore().pushItemToList(this.getMongoEntity(), "scopeIds", role.getId(), true, invocationContext);
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        getMongoStore().pullItemFromList(this.getMongoEntity(), "scopeIds", role.getId(), invocationContext);
    }

    @Override
    public String getProtocol() {
        return getMongoEntityAsClient().getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        getMongoEntityAsClient().setProtocol(protocol);
        updateMongoEntity();

    }

    @Override
    public void setAttribute(String name, String value) {
        getMongoEntityAsClient().getAttributes().put(name, value);
        updateMongoEntity();

    }

    @Override
    public void removeAttribute(String name) {
        getMongoEntityAsClient().getAttributes().remove(name);
        updateMongoEntity();
    }

    @Override
    public String getAttribute(String name) {
        return getMongoEntityAsClient().getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> copy = new HashMap<String, String>();
        copy.putAll(getMongoEntityAsClient().getAttributes());
        return copy;
    }


}
