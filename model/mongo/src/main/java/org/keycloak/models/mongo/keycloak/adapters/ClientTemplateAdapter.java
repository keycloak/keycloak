/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.entities.ProtocolMapperEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoClientTemplateEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.mongo.utils.MongoModelUtils;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class ClientTemplateAdapter extends AbstractMongoAdapter<MongoClientTemplateEntity> implements ClientTemplateModel {

    protected final MongoClientTemplateEntity clientTemplateEntity;
    private final RealmModel realm;
    protected  KeycloakSession session;

    public ClientTemplateAdapter(KeycloakSession session, RealmModel realm, MongoClientTemplateEntity clientEntity, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.session = session;
        this.realm = realm;
        this.clientTemplateEntity    = clientEntity;
    }

    @Override
    public MongoClientTemplateEntity getMongoEntity() {
        return clientTemplateEntity;
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }


    @Override
    public String getId() {
        return getMongoEntity().getId();
    }

    @Override
    public String getName() {
        return getMongoEntity().getName();
    }

    @Override
    public void setName(String name) {
        getMongoEntity().setName(name);
        updateMongoEntity();
    }

    @Override
    public String getDescription() { return getMongoEntity().getDescription(); }

    @Override
    public String getProtocol() {
        return getMongoEntity().getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        getMongoEntity().setProtocol(protocol);
        updateMongoEntity();

    }


    @Override
    public void setDescription(String description) {
        getMongoEntity().setDescription(description);
        updateMongoEntity();
    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        Set<ProtocolMapperModel> result = new HashSet<ProtocolMapperModel>();
        for (ProtocolMapperEntity entity : getMongoEntity().getProtocolMappers()) {
            ProtocolMapperModel mapping = new ProtocolMapperModel();
            mapping.setId(entity.getId());
            mapping.setName(entity.getName());
            mapping.setProtocol(entity.getProtocol());
            mapping.setProtocolMapper(entity.getProtocolMapper());
            mapping.setConsentRequired(entity.isConsentRequired());
            mapping.setConsentText(entity.getConsentText());
            Map<String, String> config = new HashMap<String, String>();
            if (entity.getConfig() != null) {
                config.putAll(entity.getConfig());
            }
            mapping.setConfig(config);
            result.add(mapping);
        }
        return result;
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        if (getProtocolMapperByName(model.getProtocol(), model.getName()) != null) {
            throw new ModelDuplicateException("Protocol mapper name must be unique per protocol");
        }
        ProtocolMapperEntity entity = new ProtocolMapperEntity();
        String id = model.getId() != null ? model.getId() : KeycloakModelUtils.generateId();
        entity.setId(id);
        entity.setProtocol(model.getProtocol());
        entity.setName(model.getName());
        entity.setProtocolMapper(model.getProtocolMapper());
        entity.setConfig(model.getConfig());
        entity.setConsentRequired(model.isConsentRequired());
        entity.setConsentText(model.getConsentText());
        getMongoEntity().getProtocolMappers().add(entity);
        updateMongoEntity();
        return entityToModel(entity);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        for (ProtocolMapperEntity entity : getMongoEntity().getProtocolMappers()) {
            if (entity.getId().equals(mapping.getId())) {
                session.users().preRemove(mapping);

                getMongoEntity().getProtocolMappers().remove(entity);
                updateMongoEntity();
                break;
            }
        }

    }

    protected ProtocolMapperEntity getProtocolMapperyEntityById(String id) {
        for (ProtocolMapperEntity entity : getMongoEntity().getProtocolMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }
    protected ProtocolMapperEntity getProtocolMapperEntityByName(String protocol, String name) {
        for (ProtocolMapperEntity entity : getMongoEntity().getProtocolMappers()) {
            if (entity.getProtocol().equals(protocol) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }


    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        ProtocolMapperEntity entity = getProtocolMapperyEntityById(mapping.getId());
        entity.setProtocolMapper(mapping.getProtocolMapper());
        entity.setConsentRequired(mapping.isConsentRequired());
        entity.setConsentText(mapping.getConsentText());
        if (entity.getConfig() != null) {
            entity.getConfig().clear();
            entity.getConfig().putAll(mapping.getConfig());
        } else {
            entity.setConfig(mapping.getConfig());
        }
        updateMongoEntity();

    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        ProtocolMapperEntity entity = getProtocolMapperyEntityById(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        ProtocolMapperEntity entity = getProtocolMapperEntityByName(protocol, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected ProtocolMapperModel entityToModel(ProtocolMapperEntity entity) {
        ProtocolMapperModel mapping = new ProtocolMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setProtocol(entity.getProtocol());
        mapping.setProtocolMapper(entity.getProtocolMapper());
        mapping.setConsentRequired(entity.isConsentRequired());
        mapping.setConsentText(entity.getConsentText());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }

    @Override
    public boolean isFullScopeAllowed() {
        return getMongoEntity().isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        getMongoEntity().setFullScopeAllowed(value);
        updateMongoEntity();

    }
    @Override
    public Set<RoleModel> getScopeMappings() {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<MongoRoleEntity> roles = MongoModelUtils.getAllScopesOfTemplate(this, invocationContext);

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
    public void addScopeMapping(RoleModel role) {
        getMongoStore().pushItemToList(this.getMongoEntity(), "scopeIds", role.getId(), true, invocationContext);
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        getMongoStore().pullItemFromList(this.getMongoEntity(), "scopeIds", role.getId(), invocationContext);
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
    public boolean isPublicClient() {
        return getMongoEntity().isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        getMongoEntity().setPublicClient(flag);
        updateMongoEntity();
    }


    @Override
    public boolean isFrontchannelLogout() {
        return getMongoEntity().isFrontchannelLogout();
    }

    @Override
    public void setFrontchannelLogout(boolean flag) {
        getMongoEntity().setFrontchannelLogout(flag);
        updateMongoEntity();
    }

    @Override
    public void setAttribute(String name, String value) {
        getMongoEntity().getAttributes().put(name, value);
        updateMongoEntity();

    }

    @Override
    public void removeAttribute(String name) {
        getMongoEntity().getAttributes().remove(name);
        updateMongoEntity();
    }

    @Override
    public String getAttribute(String name) {
        return getMongoEntity().getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> copy = new HashMap<String, String>();
        copy.putAll(getMongoEntity().getAttributes());
        return copy;
    }

    @Override
    public boolean isBearerOnly() {
        return getMongoEntity().isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        getMongoEntity().setBearerOnly(only);
        updateMongoEntity();
    }

    @Override
    public boolean isConsentRequired() {
        return getMongoEntity().isConsentRequired();
    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        getMongoEntity().setConsentRequired(consentRequired);
        updateMongoEntity();
    }

    @Override
    public boolean isStandardFlowEnabled() {
        return getMongoEntity().isStandardFlowEnabled();
    }

    @Override
    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        getMongoEntity().setStandardFlowEnabled(standardFlowEnabled);
        updateMongoEntity();
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        return getMongoEntity().isImplicitFlowEnabled();
    }

    @Override
    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        getMongoEntity().setImplicitFlowEnabled(implicitFlowEnabled);
        updateMongoEntity();
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        return getMongoEntity().isDirectAccessGrantsEnabled();
    }

    @Override
    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        getMongoEntity().setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
        updateMongoEntity();
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        return getMongoEntity().isServiceAccountsEnabled();
    }

    @Override
    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        getMongoEntity().setServiceAccountsEnabled(serviceAccountsEnabled);
        updateMongoEntity();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ClientTemplateModel)) return false;

        ClientTemplateModel that = (ClientTemplateModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }


}
