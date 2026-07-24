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

package org.keycloak.models.jpa.entities;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        @NamedQuery(name="deleteUserAttributesByRealm", query="delete from  UserAttributeEntity attr where attr.user IN (select u from UserEntity u where u.realmId=:realmId)"),
        @NamedQuery(name="deleteUserAttributesByNameAndUser", query="delete from  UserAttributeEntity attr where attr.user.id = :userId and attr.name = :name"),
        @NamedQuery(name="deleteUserAttributesByNameAndUserOtherThan", query="delete from  UserAttributeEntity attr where attr.user.id = :userId and attr.name = :name and attr.id <> :attrId"),
        @NamedQuery(name="deleteUserAttributesByRealmAndLink", query="delete from  UserAttributeEntity attr where attr.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)")
})
@Table(name="USER_ATTRIBUTE")
@Entity
public class UserAttributeEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    protected UserEntity user;

    @Column(name = "NAME")
    protected String name;
    @Nationalized
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
            throw new IllegalStateException(String.format("User with id %s should not have set both `value` and `longValue` for attribute %s.", user.getId(), name));
        }
        return value != null ? value : longValue;
    }

    public void setValue(String value) {
        if (value == null) {
            this.value = null;
            this.longValue = null;
            this.longValueHash = null;
            this.longValueHashLowerCase = null;
        } else if (value.length() > 255) {
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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof UserAttributeEntity)) return false;

        UserAttributeEntity that = (UserAttributeEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


}
