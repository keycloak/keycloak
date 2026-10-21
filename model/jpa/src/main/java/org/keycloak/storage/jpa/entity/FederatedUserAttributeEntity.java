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

package org.keycloak.storage.jpa.entity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.keycloak.storage.jpa.JpaHashUtils;

import org.hibernate.annotations.Nationalized;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getFederatedAttributesByNameAndValue", query="select attr.userId from FederatedUserAttributeEntity attr where attr.name = :name and attr.value = :value and attr.realmId=:realmId"),
        @NamedQuery(name="getFederatedAttributesByNameAndLongValue", query="select attr.userId, attr.longValue from FederatedUserAttributeEntity attr where attr.name = :name and attr.longValueHash = :longValueHash and attr.realmId=:realmId"),
        @NamedQuery(name="getFederatedAttributesByUser", query="select attr from FederatedUserAttributeEntity attr where attr.userId = :userId and attr.realmId=:realmId"),
        @NamedQuery(name="deleteUserFederatedAttributesByUser", query="delete from  FederatedUserAttributeEntity attr where attr.userId = :userId and attr.realmId=:realmId"),
        @NamedQuery(name="deleteUserFederatedAttributesByUserAndName", query="delete from  FederatedUserAttributeEntity attr where attr.userId = :userId and attr.name=:name and attr.realmId=:realmId"),
        @NamedQuery(name="deleteUserFederatedAttributesByRealm", query="delete from  FederatedUserAttributeEntity attr where attr.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedAttributesByStorageProvider", query="delete from FederatedUserAttributeEntity e where e.storageProviderId=:storageProviderId"),
        @NamedQuery(name="deleteUserFederatedAttributesByRealmAndLink", query="delete from  FederatedUserAttributeEntity attr where attr.userId IN (select u.id from UserEntity u where u.realmId=:realmId and u.federationLink=:link)")
})
@Table(name="FED_USER_ATTRIBUTE")
@Entity
public class FederatedUserAttributeEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "USER_ID")
    protected String userId;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "STORAGE_PROVIDER_ID")
    protected String storageProviderId;

    @Column(name = "NAME")
    protected String name;
    @Column(name = "VALUE")
    protected String value;

    @Column(name = "LONG_VALUE_HASH")
    private byte[] longValueHash;
    @Column(name = "LONG_VALUE_HASH_LOWER_CASE")
    private byte[] longValueHashLowerCase;
    @Nationalized
    @Column(name = "LONG_VALUE")
    private String longValue;

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

    public String getValue() {
        if (value != null && longValue != null) {
            throw new IllegalStateException(String.format("Federated user with id %s should not have set both `value` and `longValue` for attribute %s.", userId, name));
        }
        return value != null ? value : longValue;
    }

    public void setValue(String value) {
        if (value == null) {
            this.value = null;
            this.longValue = null;
            this.longValueHash = null;
            this.longValueHashLowerCase = null;
        } else if (value.length() > 2024) { // https://github.com/keycloak/keycloak/blob/2785bbd29bcc1b39d9abe90724333dd42af34b10/model/jpa/src/main/resources/META-INF/jpa-changelog-2.1.0.xml#L58
            this.value = null;
            this.longValue = value;
            this.longValueHash = JpaHashUtils.hashForAttributeValue(value);
            this.longValueHashLowerCase = JpaHashUtils.hashForAttributeValueLowerCase(value);
        } else {
            this.value = value;
            this.longValue = null;
            this.longValueHash = null;
            this.longValueHashLowerCase = null;
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getStorageProviderId() {
        return storageProviderId;
    }

    public void setStorageProviderId(String storageProviderId) {
        this.storageProviderId = storageProviderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederatedUserAttributeEntity)) return false;

        FederatedUserAttributeEntity that = (FederatedUserAttributeEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


}
