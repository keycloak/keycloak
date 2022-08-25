/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.ldap.role.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang.NotImplementedException;
import org.keycloak.models.ModelException;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.common.delegate.EntityFieldDelegate;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.models.map.role.MapRoleEntityFields;
import org.keycloak.models.map.storage.ldap.model.LdapMapDn;
import org.keycloak.models.map.storage.ldap.model.LdapMapObject;
import org.keycloak.models.map.storage.ldap.role.config.LdapMapRoleMapperConfig;
import org.keycloak.models.map.storage.ldap.role.LdapRoleMapKeycloakTransaction;

public class LdapRoleEntity extends UpdatableEntity.Impl implements EntityFieldDelegate<MapRoleEntity> {

    private final LdapMapObject ldapMapObject;
    private final LdapMapRoleMapperConfig roleMapperConfig;
    private final LdapRoleMapKeycloakTransaction transaction;
    private final String clientId;

    private static final EnumMap<MapRoleEntityFields, BiConsumer<LdapRoleEntity, Object>> SETTERS = new EnumMap<>(MapRoleEntityFields.class);
    static {
        SETTERS.put(MapRoleEntityFields.DESCRIPTION, (e, v) -> e.setDescription((String) v));
        SETTERS.put(MapRoleEntityFields.ID, (e, v) -> e.setId((String) v));
        SETTERS.put(MapRoleEntityFields.REALM_ID, (e, v) -> e.setRealmId((String) v));
        SETTERS.put(MapRoleEntityFields.CLIENT_ID, (e, v) -> e.setClientId((String) v));
        SETTERS.put(MapRoleEntityFields.CLIENT_ROLE, (e, v) -> e.setClientRole((Boolean) v));
        //noinspection unchecked
        SETTERS.put(MapRoleEntityFields.ATTRIBUTES, (e, v) -> e.setAttributes((Map<String, List<String>>) v));
        //noinspection unchecked
        SETTERS.put(MapRoleEntityFields.COMPOSITE_ROLES, (e, v) -> e.setCompositeRoles((Set<String>) v));
        SETTERS.put(MapRoleEntityFields.NAME, (e, v) -> e.setName((String) v));
    }

    private static final EnumMap<MapRoleEntityFields, Function<LdapRoleEntity, Object>> GETTERS = new EnumMap<>(MapRoleEntityFields.class);
    static {
        GETTERS.put(MapRoleEntityFields.DESCRIPTION, LdapRoleEntity::getDescription);
        GETTERS.put(MapRoleEntityFields.ID, LdapRoleEntity::getId);
        GETTERS.put(MapRoleEntityFields.REALM_ID, LdapRoleEntity::getRealmId);
        GETTERS.put(MapRoleEntityFields.CLIENT_ID, LdapRoleEntity::getClientId);
        GETTERS.put(MapRoleEntityFields.CLIENT_ROLE, LdapRoleEntity::isClientRole);
        GETTERS.put(MapRoleEntityFields.ATTRIBUTES, LdapRoleEntity::getAttributes);
        GETTERS.put(MapRoleEntityFields.COMPOSITE_ROLES, LdapRoleEntity::getCompositeRoles);
        GETTERS.put(MapRoleEntityFields.NAME, LdapRoleEntity::getName);
    }

    private static final EnumMap<MapRoleEntityFields, BiConsumer<LdapRoleEntity, Object>> ADDERS = new EnumMap<>(MapRoleEntityFields.class);
    static {
        ADDERS.put(MapRoleEntityFields.COMPOSITE_ROLES, (e, v) -> e.addCompositeRole((String) v));
    }

    private static final EnumMap<MapRoleEntityFields, BiFunction<LdapRoleEntity, Object, Object>> REMOVERS = new EnumMap<>(MapRoleEntityFields.class);
    static {
        REMOVERS.put(MapRoleEntityFields.COMPOSITE_ROLES, (e, v) -> { e.removeCompositeRole((String) v); return null; });
    }

    public LdapRoleEntity(DeepCloner cloner, LdapMapRoleMapperConfig roleMapperConfig, LdapRoleMapKeycloakTransaction transaction, String clientId) {
        ldapMapObject = new LdapMapObject();
        ldapMapObject.setObjectClasses(Arrays.asList("top", "groupOfNames"));
        ldapMapObject.setRdnAttributeName(roleMapperConfig.getRoleNameLdapAttribute());
        this.roleMapperConfig = roleMapperConfig;
        this.transaction = transaction;
        this.clientId = clientId;
    }

    public LdapRoleEntity(LdapMapObject ldapMapObject, LdapMapRoleMapperConfig roleMapperConfig, LdapRoleMapKeycloakTransaction transaction, String clientId) {
        this.ldapMapObject = ldapMapObject;
        this.roleMapperConfig = roleMapperConfig;
        this.transaction = transaction;
        this.clientId = clientId;
    }

    public String getId() {
        return ldapMapObject.getId();
    }

    public void setId(String id) {
        this.updated |= !Objects.equals(getId(), id);
        ldapMapObject.setId(id);
    }


    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> result = new HashMap<>();
        for (String roleAttribute : roleMapperConfig.getRoleAttributes()) {
            Set<String> attrs = ldapMapObject.getAttributeAsSet(roleAttribute);
            if (attrs != null) {
                result.put(roleAttribute, new ArrayList<>(attrs));
            }
        }
        return result;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        // store all attributes
        if (attributes != null) {
            attributes.forEach(this::setAttribute);
        }
        // clear attributes not in the list
        for (String roleAttribute : roleMapperConfig.getRoleAttributes()) {
            if (attributes == null || !attributes.containsKey(roleAttribute)) {
                removeAttribute(roleAttribute);
            }
        }
    }

    public List<String> getAttribute(String name) {
        if (!roleMapperConfig.getRoleAttributes().contains(name)) {
            throw new ModelException("can't read attribute '" + name + "' as it is not supported");
        }
        return new ArrayList<>(ldapMapObject.getAttributeAsSet(name));
    }

    public void setAttribute(String name, List<String> value) {
        if (!roleMapperConfig.getRoleAttributes().contains(name)) {
            throw new ModelException("can't set attribute '" + name + "' as it is not supported");
        }
        if ((ldapMapObject.getAttributeAsSet(name) == null && (value == null || value.size() == 0)) ||
                Objects.equals(ldapMapObject.getAttributeAsSet(name), new HashSet<>(value))) {
            return;
        }
        if (ldapMapObject.getReadOnlyAttributeNames().contains(name)) {
            throw new ModelException("can't write attribute '" + name + "' as it is not writeable");
        }
        ldapMapObject.setAttribute(name, new HashSet<>(value));
        this.updated = true;
    }

    public void removeAttribute(String name) {
        if (!roleMapperConfig.getRoleAttributes().contains(name)) {
            throw new ModelException("can't write attribute '" + name + "' as it is not supported");
        }
        if (ldapMapObject.getAttributeAsSet(name) == null || ldapMapObject.getAttributeAsSet(name).size() == 0) {
            return;
        }
        ldapMapObject.setAttribute(name, null);
        this.updated = true;
    }

    public String getRealmId() {
        return null;
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return ldapMapObject.getAttributeAsString(roleMapperConfig.getRoleNameLdapAttribute());
    }

    public String getDescription() {
        return ldapMapObject.getAttributeAsString("description");
    }

    public void setClientRole(Boolean clientRole) {
        if (!Objects.equals(this.isClientRole(), clientRole)) {
            throw new NotImplementedException();
        }
    }

    public boolean isClientRole() {
        return clientId != null;
    }

    public void setRealmId(String realmId) {
        // we'll not store this information, as LDAP store might be used from different realms
    }

    public void setClientId(String clientId) {
        if (!Objects.equals(this.getClientId(), clientId)) {
            throw new NotImplementedException();
        }
    }

    public void setName(String name) {
        this.updated |= !Objects.equals(getName(), name);
        ldapMapObject.setSingleAttribute(roleMapperConfig.getRoleNameLdapAttribute(), name);
        LdapMapDn dn = LdapMapDn.fromString(roleMapperConfig.getRolesDn(clientId != null, clientId));
        dn.addFirst(roleMapperConfig.getRoleNameLdapAttribute(), name);
        ldapMapObject.setDn(dn);
    }

    public void setDescription(String description) {
        this.updated |= !Objects.equals(getDescription(), description);
        if (description != null) {
            ldapMapObject.setSingleAttribute("description", description);
        } else if (getDescription() != null) {
            ldapMapObject.setAttribute("description", null);
        }
    }

    public Set<String> getCompositeRoles() {
        Set<String> members = ldapMapObject.getAttributeAsSet(roleMapperConfig.getMembershipLdapAttribute());
        if (members == null) {
            members = new HashSet<>();
        }
        HashSet<String> compositeRoles = new HashSet<>();
        for (String member : members) {
            if (member.equals(ldapMapObject.getDn().toString())) {
                continue;
            }
            if (!member.startsWith(roleMapperConfig.getRoleNameLdapAttribute())) {
                // this is a real user, not a composite role, ignore
                // TODO: this will not work if users and role use the same!
                continue;
            }
            String roleId = transaction.readIdByDn(member);
            if (roleId == null) {
                throw new NotImplementedException();
            }
            compositeRoles.add(roleId);
        }
        return compositeRoles;
    }

    public void setCompositeRoles(Set<String> compositeRoles) {
        HashSet<String> translatedCompositeRoles = new HashSet<>();
        if (compositeRoles != null) {
            for (String compositeRole : compositeRoles) {
                LdapRoleEntity ldapRole = transaction.readLdap(compositeRole);
                translatedCompositeRoles.add(ldapRole.getLdapMapObject().getDn().toString());
            }
        }
        Set<String> members = ldapMapObject.getAttributeAsSet(roleMapperConfig.getMembershipLdapAttribute());
        if (members == null) {
            members = new HashSet<>();
        }
        for (String member : members) {
            if (!member.startsWith(roleMapperConfig.getRoleNameLdapAttribute())) {
                // this is a real user, not a composite role, ignore
                // TODO: this will not work if users and role use the same!
                translatedCompositeRoles.add(member);
            }
        }
        if (!translatedCompositeRoles.equals(members)) {
            ldapMapObject.setAttribute(roleMapperConfig.getMembershipLdapAttribute(), members);
            this.updated = true;
        }
    }

    public void addCompositeRole(String roleId) {
        LdapRoleEntity ldapRole = transaction.readLdap(roleId);
        Set<String> members = ldapMapObject.getAttributeAsSet(roleMapperConfig.getMembershipLdapAttribute());
        if (members == null) {
            members = new HashSet<>();
        }
        members.add(ldapRole.getLdapMapObject().getDn().toString());
        ldapMapObject.setAttribute(roleMapperConfig.getMembershipLdapAttribute(), members);
        this.updated = true;
    }

    public void removeCompositeRole(String roleId) {
        LdapRoleEntity ldapRole = transaction.readLdap(roleId);
        Set<String> members = ldapMapObject.getAttributeAsSet(roleMapperConfig.getMembershipLdapAttribute());
        if (members == null) {
            members = new HashSet<>();
        }
        members.remove(ldapRole.getLdapMapObject().getDn().toString());
        ldapMapObject.setAttribute(roleMapperConfig.getMembershipLdapAttribute(), members);
        this.updated = true;
    }

    public LdapMapObject getLdapMapObject() {
        return ldapMapObject;
    }

    @Override
    public <T, EF extends Enum<? extends EntityField<MapRoleEntity>> & EntityField<MapRoleEntity>> void set(EF field, T value) {
        BiConsumer<LdapRoleEntity, Object> consumer = SETTERS.get(field);
        if (consumer == null) {
            throw new ModelException("unsupported field for setters " + field);
        }
        consumer.accept(this, value);
    }

    @Override
    public <T, EF extends Enum<? extends EntityField<MapRoleEntity>> & EntityField<MapRoleEntity>> void collectionAdd(EF field, T value) {
        BiConsumer<LdapRoleEntity, Object> consumer = ADDERS.get(field);
        if (consumer == null) {
            throw new ModelException("unsupported field for setters " + field);
        }
        consumer.accept(this, value);
    }

    @Override
    public <T, EF extends Enum<? extends EntityField<MapRoleEntity>> & EntityField<MapRoleEntity>> Object collectionRemove(EF field, T value) {
        BiFunction<LdapRoleEntity, Object, Object> consumer = REMOVERS.get(field);
        if (consumer == null) {
            throw new ModelException("unsupported field for setters " + field);
        }
        return consumer.apply(this, value);
    }

    @Override
    public <EF extends Enum<? extends EntityField<MapRoleEntity>> & EntityField<MapRoleEntity>> Object get(EF field) {
        Function<LdapRoleEntity, Object> consumer = GETTERS.get(field);
        if (consumer == null) {
            throw new ModelException("unsupported field for getters " + field);
        }
        return consumer.apply(this);
    }

}
