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
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Binding between client and clientScope
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="clientScopeClientMappingIdsByClient", query="select m.clientScopeId from ClientScopeClientMappingEntity m where m.clientId = :clientId and m.defaultScope = :defaultScope"),
        @NamedQuery(name="deleteClientScopeClientMapping", query="delete from ClientScopeClientMappingEntity where clientId = :clientId and clientScopeId = :clientScopeId"),
        @NamedQuery(name="deleteClientScopeClientMappingByClient", query="delete from ClientScopeClientMappingEntity where clientId = :clientId"),
        @NamedQuery(name="deleteClientScopeClientMappingByClientScope", query="delete from ClientScopeClientMappingEntity where clientScopeId = :clientScopeId"),
        @NamedQuery(name="addClientScopeToAllClients", query="insert into ClientScopeClientMappingEntity (clientScopeId, defaultScope, clientId) select :clientScopeId, :defaultScope, client.id from ClientEntity client where client.realmId = :realmId and client.bearerOnly <> true and client.protocol = :clientProtocol")
})
@Entity
@Table(name="CLIENT_SCOPE_CLIENT")
@IdClass(ClientScopeClientMappingEntity.Key.class)
public class ClientScopeClientMappingEntity {

    @Id
    @Column(name = "SCOPE_ID")
    protected String clientScopeId;

    @Id
    @Column(name="CLIENT_ID")
    protected String clientId;

    @Column(name="DEFAULT_SCOPE")
    protected boolean defaultScope;

    public String getClientScopeId() {
        return clientScopeId;
    }

    public void setClientScopeId(String clientScopeId) {
        this.clientScopeId = clientScopeId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isDefaultScope() {
        return defaultScope;
    }

    public void setDefaultScope(boolean defaultScope) {
        this.defaultScope = defaultScope;
    }

    public static class Key implements Serializable {

        protected String clientScopeId;

        protected String clientId;

        public Key() {
        }

        public Key(String clientScopeId, String clientId) {
            this.clientScopeId = clientScopeId;
            this.clientId = clientId;
        }

        public String getClientScopeId() {
            return clientScopeId;
        }

        public String getClientId() {
            return clientId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClientScopeClientMappingEntity.Key key = (ClientScopeClientMappingEntity.Key) o;

            if (clientScopeId != null ? !clientScopeId.equals(key.getClientScopeId() != null ? key.getClientScopeId() : null) : key.getClientScopeId() != null) return false;
            if (clientId != null ? !clientId.equals(key.getClientId() != null ? key.getClientId() : null) : key.getClientId() != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = clientScopeId != null ? clientScopeId.hashCode() : 0;
            result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!(o instanceof ClientScopeClientMappingEntity)) return false;

        ClientScopeClientMappingEntity key = (ClientScopeClientMappingEntity) o;

        if (clientScopeId != null ? !clientScopeId.equals(key.getClientScopeId() != null ? key.getClientScopeId() : null) : key.getClientScopeId() != null) return false;
        if (clientId != null ? !clientId.equals(key.getClientId() != null ? key.getClientId() : null) : key.getClientId() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = clientScopeId != null ? clientScopeId.hashCode() : 0;
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        return result;
    }
}
