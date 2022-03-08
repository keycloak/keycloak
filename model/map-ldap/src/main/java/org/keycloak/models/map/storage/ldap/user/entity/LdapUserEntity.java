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
package org.keycloak.models.map.storage.ldap.user.entity;

import org.keycloak.models.ModelException;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.common.delegate.EntityFieldDelegate;
import org.keycloak.models.map.credential.MapSubjectCredentialManagerEntity;
import org.keycloak.models.map.storage.ldap.config.LdapKerberosConfig;
import org.keycloak.models.map.storage.ldap.user.credential.LdapSingleUserCredentialManagerEntity;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.user.MapUserEntityFields;
import org.keycloak.models.map.storage.ldap.model.LdapMapDn;
import org.keycloak.models.map.storage.ldap.model.LdapMapObject;
import org.keycloak.models.map.storage.ldap.user.LdapUserMapKeycloakTransaction;
import org.keycloak.models.map.storage.ldap.user.config.LdapMapUserMapperConfig;
import org.keycloak.models.map.user.MapUserEntityImpl;

import java.util.ArrayList;
import java.util.Collections;
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

public class LdapUserEntity extends UpdatableEntity.Impl implements EntityFieldDelegate<MapUserEntity> {

    private final LdapMapObject ldapMapObject;
    private final LdapMapUserMapperConfig userMapperConfig;
    private final LdapUserMapKeycloakTransaction transaction;

    private static final EnumMap<MapUserEntityFields, BiConsumer<LdapUserEntity, Object>> SETTERS = new EnumMap<>(MapUserEntityFields.class);

    static {
        SETTERS.put(MapUserEntityFields.USERNAME, (e, v) -> e.setName((String) v));
        SETTERS.put(MapUserEntityFields.LAST_NAME, (e, v) -> e.setLdapAttribute("sn", (String) v));
        SETTERS.put(MapUserEntityFields.FIRST_NAME, (e, v) -> e.setLdapAttribute("cn", (String) v));
        SETTERS.put(MapUserEntityFields.EMAIL, (e, v) -> e.setLdapAttribute("mail", (String) v));
        SETTERS.put(MapUserEntityFields.ID, (e, v) -> e.setId((String) v));
        SETTERS.put(MapUserEntityFields.REALM_ID, (e, v) -> e.setRealmId((String) v));
        //noinspection unchecked
        SETTERS.put(MapUserEntityFields.ATTRIBUTES, (e, v) -> e.setAttributes((Map<String, List<String>>) v));
    }

    private static final EnumMap<MapUserEntityFields, Function<LdapUserEntity, Object>> GETTERS = new EnumMap<>(MapUserEntityFields.class);
    static {
        GETTERS.put(MapUserEntityFields.USERNAME, LdapUserEntity::getName);
        GETTERS.put(MapUserEntityFields.LAST_NAME, (e) -> e.getLdapAttribute("sn"));
        GETTERS.put(MapUserEntityFields.FIRST_NAME, (e) -> e.getLdapAttribute("cn"));
        GETTERS.put(MapUserEntityFields.EMAIL, (e) -> e.getLdapAttribute("mail"));
        GETTERS.put(MapUserEntityFields.ID, LdapUserEntity::getId);
        GETTERS.put(MapUserEntityFields.REALM_ID, LdapUserEntity::getRealmId);
        GETTERS.put(MapUserEntityFields.ATTRIBUTES, LdapUserEntity::getAttributes);
        GETTERS.put(MapUserEntityFields.ENABLED, LdapUserEntity::isEnabled);
    }

    // https://social.technet.microsoft.com/wiki/contents/articles/5392.active-directory-ldap-syntax-filters.aspx
    // All enabled user objects
    // (!(userAccountControl:1.2.840.113556.1.4.803:=2)))
    // http://www.selfadsi.org/ads-attributes/user-userAccountControl.htm
    private boolean isEnabled() {
        return true;
    }

    private static final EnumMap<MapUserEntityFields, BiConsumer<LdapUserEntity, Object>> ADDERS = new EnumMap<>(MapUserEntityFields.class);
    static {
    }

    private static final EnumMap<MapUserEntityFields, BiFunction<LdapUserEntity, Object, Object>> REMOVERS = new EnumMap<>(MapUserEntityFields.class);
    static {
    }

    private MapUserEntity delegate;

    public LdapUserEntity(DeepCloner cloner, LdapMapUserMapperConfig userMapperConfig, LdapUserMapKeycloakTransaction transaction) {
        ldapMapObject = new LdapMapObject();
        ldapMapObject.setObjectClasses(userMapperConfig.getLdapMapConfig().getUserObjectClasses());
        ldapMapObject.setRdnAttributeName(userMapperConfig.getUserNameLdapAttribute());
        this.delegate = new MapUserEntityImpl(cloner);
        this.userMapperConfig = userMapperConfig;
        this.transaction = transaction;
    }

    public LdapUserEntity(LdapMapObject ldapMapObject, LdapMapUserMapperConfig userMapperConfig, LdapUserMapKeycloakTransaction transaction) {
        this.ldapMapObject = ldapMapObject;
        this.userMapperConfig = userMapperConfig;
        this.transaction = transaction;
        MapUserEntity entity = transaction.getDelegate().read(ldapMapObject.getId());
        if (entity == null) {
            entity = new MapUserEntityImpl(null);
        }
        this.delegate = entity;
    }

    public String getId() {
        return ldapMapObject.getId();
    }

    public void setId(String id) {
        this.updated |= !Objects.equals(getId(), id);
        ldapMapObject.setId(id);
        delegate.setId(id);
    }

    public void createDelegate() {
        delegate.setId(ldapMapObject.getId());
        delegate = transaction.getDelegate().create(delegate);
    }

    private void setLdapAttribute(String attributeName, String attributeValue) {
        this.updated |= !Objects.equals(getLdapAttribute(attributeName), attributeValue);
        ldapMapObject.setSingleAttribute(attributeName, attributeValue);
    }

    private String getLdapAttribute(String attributeName) {
        return ldapMapObject.getAttributeAsString(attributeName);
    }

    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> result = new HashMap<>();
        for (String userAttribute : userMapperConfig.getUserAttributes()) {
            Set<String> attrs = ldapMapObject.getAttributeAsSet(userAttribute);
            if (attrs != null) {
                result.put(userAttribute, new ArrayList<>(attrs));
            }
        }

        // KERBEROS_PRINCIPAL is used by KerberosFederationProvider to figure out if the user returned by username really matches the Kerberos realm
        result.put("KERBEROS_PRINCIPAL", Collections.singletonList(getName() + "@" + new LdapKerberosConfig(userMapperConfig.getLdapMapConfig()).getKerberosRealm()));
        return result;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        // store all attributes
        if (attributes != null) {
            attributes.forEach(this::setAttribute);
        }
        // clear attributes not in the list
        for (String userAttribute : userMapperConfig.getUserAttributes()) {
            if (attributes == null || !attributes.containsKey(userAttribute)) {
                removeAttribute(userAttribute);
            }
        }
        if (delegate.getAttributes() != null) {
            for (String userAttribute : delegate.getAttributes().keySet()) {
                if (attributes == null || !attributes.containsKey(userAttribute)) {
                    removeAttribute(userAttribute);
                }
            }
        }
    }

    public List<String> getAttribute(String name) {
        if (!userMapperConfig.getUserAttributes().contains(name)) {
            return delegate.getAttribute(name);
        }
        return new ArrayList<>(ldapMapObject.getAttributeAsSet(name));
    }

    public void setAttribute(String name, List<String> value) {
        if (!userMapperConfig.getUserAttributes().contains(name)) {
            delegate.setAttribute(name, value);
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
        if (!userMapperConfig.getUserAttributes().contains(name)) {
            delegate.removeAttribute(name);
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

    public String getName() {
        return ldapMapObject.getAttributeAsString(userMapperConfig.getUserNameLdapAttribute());
    }

    public String getDescription() {
        return ldapMapObject.getAttributeAsString("description");
    }

    public void setRealmId(String realmId) {
        // we'll not store this information, as LDAP store might be used from different realms
    }

    public void setName(String name) {
        this.updated |= !Objects.equals(getName(), name);
        ldapMapObject.setSingleAttribute(userMapperConfig.getUserNameLdapAttribute(), name);
        LdapMapDn dn = LdapMapDn.fromString(userMapperConfig.getLdapMapConfig().getUsersDn());
        dn.addFirst(userMapperConfig.getUserNameLdapAttribute(), name);
        ldapMapObject.setDn(dn);
    }

    public LdapMapObject getLdapMapObject() {
        return ldapMapObject;
    }

    @Override
    public <T, EF extends Enum<? extends EntityField<MapUserEntity>> & EntityField<MapUserEntity>> void set(EF field, T value) {
        BiConsumer<LdapUserEntity, Object> consumer = SETTERS.get(field);
        if (consumer == null) {
            field.set(delegate, value);
        } else {
            consumer.accept(this, value);
        }
    }

    @Override
    public boolean isUpdated() {
        return super.isUpdated() || delegate.isUpdated();
    }

    @Override
    public <T, EF extends Enum<? extends EntityField<MapUserEntity>> & EntityField<MapUserEntity>> void collectionAdd(EF field, T value) {
        BiConsumer<LdapUserEntity, Object> consumer = ADDERS.get(field);
        if (consumer == null) {
            field.collectionAdd(delegate, value);
        } else {
            consumer.accept(this, value);
        }
    }

    @Override
    public <T, EF extends Enum<? extends EntityField<MapUserEntity>> & EntityField<MapUserEntity>> Object collectionRemove(EF field, T value) {
        BiFunction<LdapUserEntity, Object, Object> consumer = REMOVERS.get(field);
        if (consumer == null) {
            return field.collectionRemove(delegate, value);
        } else {
            return consumer.apply(this, value);
        }
    }

    @Override
    public <EF extends Enum<? extends EntityField<MapUserEntity>> & EntityField<MapUserEntity>> Object get(EF field) {
        Function<LdapUserEntity, Object> consumer = GETTERS.get(field);
        if (consumer == null) {
            return field.get(delegate);
        } else {
            return consumer.apply(this);
        }
    }

    @Override
    public <K, EF extends Enum<? extends EntityField<MapUserEntity>> & EntityField<MapUserEntity>> Object mapGet(EF field, K key) {
        if (field == MapUserEntityFields.ATTRIBUTES) {
            return getAttribute((String) key);
        } else {
            throw new ModelException("unsupported field for mapGet " + field);
        }
    }

    @Override
    public <K, T, EF extends Enum<? extends EntityField<MapUserEntity>> & EntityField<MapUserEntity>> void mapPut(EF field, K key, T value) {
        if (field == MapUserEntityFields.ATTRIBUTES) {
            //noinspection unchecked
            setAttribute((String) key, (List<String>) value);
        } else {
            throw new ModelException("unsupported field for mapGetPut " + field);
        }
    }

    @Override
    public <K, EF extends Enum<? extends EntityField<MapUserEntity>> & EntityField<MapUserEntity>> Object mapRemove(EF field, K key) {
        if (field == MapUserEntityFields.ATTRIBUTES) {
            removeAttribute((String) key);
            return null;
        } else {
            throw new ModelException("unsupported field for mapRemove " + field);
        }
    }

    public MapSubjectCredentialManagerEntity getUserCredentialManager() {
        return new LdapSingleUserCredentialManagerEntity(transaction, ldapMapObject);
    }
}
