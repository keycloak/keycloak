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

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.hibernate.annotations.Nationalized;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="deleteRealmAttributesByRealm", query="delete from RealmAttributeEntity attr where attr.realm = :realm"),
        @NamedQuery(name="selectRealmAttributesNotEmptyByName", query="select ra from RealmAttributeEntity ra WHERE ra.name = :name and length(ra.value) > 0")
})
@Table(name="REALM_ATTRIBUTE")
@Entity
@IdClass(RealmAttributeEntity.Key.class)
public class RealmAttributeEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Id
    @Column(name = "NAME")
    protected String name;
    @Nationalized
    @Column(name = "VALUE")
    protected String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public static class Key implements Serializable {

        protected RealmEntity realm;

        protected String name;

        public Key() {
        }

        public Key(RealmEntity user, String name) {
            this.realm = user;
            this.name = name;
        }

        public RealmEntity getRealm() {
            return realm;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (name != null ? !name.equals(key.name) : key.name != null) return false;
            if (realm != null ? !realm.getId().equals(key.realm != null ? key.realm.getId() : null) : key.realm != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = realm != null ? realm.getId().hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof RealmAttributeEntity)) return false;

        RealmAttributeEntity key = (RealmAttributeEntity) o;

        if (name != null ? !name.equals(key.name) : key.name != null) return false;
        if (realm != null ? !realm.getId().equals(key.realm != null ? key.realm.getId() : null) : key.realm != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = realm != null ? realm.getId().hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }


}
