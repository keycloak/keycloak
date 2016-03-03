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

package org.keycloak.models.jpa;

import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.ClientTemplateEntity;
import org.keycloak.models.jpa.entities.ProtocolMapperEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.TemplateScopeMappingEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientTemplateAdapter implements ClientTemplateModel , JpaModel<ClientTemplateEntity> {

    protected KeycloakSession session;
    protected RealmModel realm;
    protected EntityManager em;
    protected ClientTemplateEntity entity;

    public ClientTemplateAdapter(RealmModel realm, EntityManager em, KeycloakSession session, ClientTemplateEntity entity) {
        this.session = session;
        this.realm = realm;
        this.em = em;
        this.entity = entity;
    }

    public ClientTemplateEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public String getDescription() { return entity.getDescription(); }

    @Override
    public void setDescription(String description) { entity.setDescription(description); }

    @Override
    public String getProtocol() {
        return entity.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        entity.setProtocol(protocol);

    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        Set<ProtocolMapperModel> mappings = new HashSet<ProtocolMapperModel>();
        for (ProtocolMapperEntity entity : this.entity.getProtocolMappers()) {
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
            mappings.add(mapping);
        }
        return mappings;
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        if (getProtocolMapperByName(model.getProtocol(), model.getName()) != null) {
            throw new ModelDuplicateException("Protocol mapper name must be unique per protocol");
        }
        String id = model.getId() != null ? model.getId() : KeycloakModelUtils.generateId();
        ProtocolMapperEntity entity = new ProtocolMapperEntity();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setProtocol(model.getProtocol());
        entity.setProtocolMapper(model.getProtocolMapper());
        entity.setClientTemplate(this.entity);
        entity.setConfig(model.getConfig());
        entity.setConsentRequired(model.isConsentRequired());
        entity.setConsentText(model.getConsentText());

        em.persist(entity);
        this.entity.getProtocolMappers().add(entity);
        return entityToModel(entity);
    }

    protected ProtocolMapperEntity getProtocolMapperEntity(String id) {
        for (ProtocolMapperEntity entity : this.entity.getProtocolMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }

    protected ProtocolMapperEntity getProtocolMapperEntityByName(String protocol, String name) {
        for (ProtocolMapperEntity entity : this.entity.getProtocolMappers()) {
            if (entity.getProtocol().equals(protocol) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        ProtocolMapperEntity toDelete = getProtocolMapperEntity(mapping.getId());
        if (toDelete != null) {
            session.users().preRemove(mapping);

            this.entity.getProtocolMappers().remove(toDelete);
            em.remove(toDelete);
        }

    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        ProtocolMapperEntity entity = getProtocolMapperEntity(mapping.getId());
        entity.setProtocolMapper(mapping.getProtocolMapper());
        entity.setConsentRequired(mapping.isConsentRequired());
        entity.setConsentText(mapping.getConsentText());
        if (entity.getConfig() == null) {
            entity.setConfig(mapping.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(mapping.getConfig());
        }
        em.flush();

    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        ProtocolMapperEntity entity = getProtocolMapperEntity(id);
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
        return entity.isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        entity.setFullScopeAllowed(value);
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings() {
        Set<RoleModel> roleMappings = getScopeMappings();

        Set<RoleModel> appRoles = new HashSet<>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                if (((RealmModel) container).getId().equals(realm.getId())) {
                    appRoles.add(role);
                }
            }
        }

        return appRoles;
    }

    @Override
    public Set<RoleModel> getScopeMappings() {
        TypedQuery<String> query = em.createNamedQuery("clientTemplateScopeMappingIds", String.class);
        query.setParameter("template", getEntity());
        List<String> ids = query.getResultList();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (String roleId : ids) {
            RoleModel role = realm.getRoleById(roleId);
            if (role == null) continue;
            roles.add(role);
        }
        return roles;
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        if (hasScope(role)) return;
        TemplateScopeMappingEntity entity = new TemplateScopeMappingEntity();
        entity.setTemplate(getEntity());
        RoleEntity roleEntity = RoleAdapter.toRoleEntity(role, em);
        entity.setRole(roleEntity);
        em.persist(entity);
        em.flush();
        em.detach(entity);
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        TypedQuery<TemplateScopeMappingEntity> query = getRealmScopeMappingQuery(role);
        List<TemplateScopeMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (TemplateScopeMappingEntity entity : results) {
            em.remove(entity);
        }
    }

    protected TypedQuery<TemplateScopeMappingEntity> getRealmScopeMappingQuery(RoleModel role) {
        TypedQuery<TemplateScopeMappingEntity> query = em.createNamedQuery("templateHasScope", TemplateScopeMappingEntity.class);
        query.setParameter("template", getEntity());
        RoleEntity roleEntity = RoleAdapter.toRoleEntity(role, em);
        query.setParameter("role", roleEntity);
        return query;
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
        return entity.isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        entity.setPublicClient(flag);
    }

    @Override
    public boolean isFrontchannelLogout() {
        return entity.isFrontchannelLogout();
    }

    @Override
    public void setFrontchannelLogout(boolean flag) {
        entity.setFrontchannelLogout(flag);
    }

    @Override
    public void setAttribute(String name, String value) {
        entity.getAttributes().put(name, value);

    }

    @Override
    public void removeAttribute(String name) {
        entity.getAttributes().remove(name);
    }

    @Override
    public String getAttribute(String name) {
        return entity.getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> copy = new HashMap<>();
        copy.putAll(entity.getAttributes());
        return copy;
    }

    @Override
    public boolean isBearerOnly() {
        return entity.isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        entity.setBearerOnly(only);
    }

    @Override
    public boolean isConsentRequired() {
        return entity.isConsentRequired();
    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        entity.setConsentRequired(consentRequired);
    }

    @Override
    public boolean isStandardFlowEnabled() {
        return entity.isStandardFlowEnabled();
    }

    @Override
    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        entity.setStandardFlowEnabled(standardFlowEnabled);
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        return entity.isImplicitFlowEnabled();
    }

    @Override
    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        entity.setImplicitFlowEnabled(implicitFlowEnabled);
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        return entity.isDirectAccessGrantsEnabled();
    }

    @Override
    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        entity.setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        return entity.isServiceAccountsEnabled();
    }

    @Override
    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        entity.setServiceAccountsEnabled(serviceAccountsEnabled);
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
