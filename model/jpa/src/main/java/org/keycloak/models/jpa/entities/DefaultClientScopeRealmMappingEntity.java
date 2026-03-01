/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

/**
 * Binding between realm and default clientScope
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="defaultClientScopeRealmMappingIdsByRealm", query="select m.clientScopeId from DefaultClientScopeRealmMappingEntity m where m.realm = :realm and m.defaultScope = :defaultScope"),
        @NamedQuery(name="deleteDefaultClientScopeRealmMapping", query="delete from DefaultClientScopeRealmMappingEntity where realm = :realm and clientScopeId = :clientScopeId"),
        @NamedQuery(name="deleteDefaultClientScopeRealmMappingByRealm", query="delete from DefaultClientScopeRealmMappingEntity where realm = :realm")
})
@Entity
@Table(name="DEFAULT_CLIENT_SCOPE")
@IdClass(DefaultClientScopeRealmMappingEntity.Key.class)
public class DefaultClientScopeRealmMappingEntity {

    @Id
    @Column(name = "SCOPE_ID")
    protected String clientScopeId;

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="REALM_ID")
    protected RealmEntity realm;

    @Column(name="DEFAULT_SCOPE")
    protected boolean defaultScope;

    public String getClientScopeId() {
        return clientScopeId;
    }

    public void setClientScopeId(String clientScopeId) {
        this.clientScopeId = clientScopeId;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public boolean isDefaultScope() {
        return defaultScope;
    }

    public void setDefaultScope(boolean defaultScope) {
        this.defaultScope = defaultScope;
    }

    public static class Key implements Serializable {

        protected String clientScopeId;

        protected RealmEntity realm;

        public Key() {
        }

        public Key(String clientScopeId, RealmEntity realm) {
            this.clientScopeId = clientScopeId;
            this.realm = realm;
        }

        public String getClientScopeId() {
            return clientScopeId;
        }

        public RealmEntity getRealm() {
            return realm;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DefaultClientScopeRealmMappingEntity.Key key = (DefaultClientScopeRealmMappingEntity.Key) o;

            if (clientScopeId != null ? !clientScopeId.equals(key.getClientScopeId() != null ? key.getClientScopeId() : null) : key.getClientScopeId() != null) return false;
            if (realm != null ? !realm.getId().equals(key.realm != null ? key.realm.getId() : null) : key.realm != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = clientScopeId != null ? clientScopeId.hashCode() : 0;
            result = 31 * result + (realm != null ? realm.getId().hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!(o instanceof DefaultClientScopeRealmMappingEntity)) return false;

        DefaultClientScopeRealmMappingEntity key = (DefaultClientScopeRealmMappingEntity) o;

        if (clientScopeId != null ? !clientScopeId.equals(key.getClientScopeId() != null ? key.getClientScopeId() : null) : key.getClientScopeId() != null) return false;
        if (realm != null ? !realm.getId().equals(key.realm != null ? key.realm.getId() : null) : key.realm != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = clientScopeId != null ? clientScopeId.hashCode() : 0;
        result = 31 * result + (realm != null ? realm.getId().hashCode() : 0);
        return result;
    }
}
