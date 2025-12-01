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

package org.keycloak.models.jpa.session;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteClientSessionsByRealm", query="delete from PersistentClientSessionEntity sess where sess.userSessionId IN (select u.userSessionId from PersistentUserSessionEntity u where u.realmId = :realmId)"),
        @NamedQuery(name="deleteClientSessionsByClient", query="delete from PersistentClientSessionEntity sess where sess.clientId = :clientId and sess.clientId != 'external'"),
        @NamedQuery(name="deleteClientSessionsByExternalClient", query="delete from PersistentClientSessionEntity sess where sess.clientStorageProvider = :clientStorageProvider and sess.externalClientId = :externalClientId and sess.clientStorageProvider != 'internal'"),
        @NamedQuery(name="deleteClientSessionsByClientStorageProvider", query="delete from PersistentClientSessionEntity sess where sess.clientStorageProvider = :clientStorageProvider"),
        @NamedQuery(name="deleteClientSessionsByUser", query="delete from PersistentClientSessionEntity sess where sess.userSessionId IN (select u.userSessionId from PersistentUserSessionEntity u where u.userId = :userId)"),
        @NamedQuery(name="deleteClientSessionsByUserSession", query="delete from PersistentClientSessionEntity sess where sess.userSessionId = :userSessionId and sess.offline = :offline"),
        @NamedQuery(name="deleteClientSessionsByUserSessions", query="delete from PersistentClientSessionEntity sess where sess.userSessionId in (:userSessionId) and sess.offline = :offline"),
        // The query "deleteExpiredClientSessions" is deprecated (since 26.5) and may be removed in the future.
        @NamedQuery(name="deleteExpiredClientSessions", query="delete from PersistentClientSessionEntity sess where sess.offline = :offline AND sess.userSessionId IN (select u.userSessionId from PersistentUserSessionEntity u where u.realmId = :realmId AND u.offline = :offline AND u.lastSessionRefresh < :lastSessionRefresh)"),
        @NamedQuery(name="deleteClientSessionsByRealmSessionType", query="delete from PersistentClientSessionEntity sess where sess.offline = :offline AND sess.userSessionId IN (select u.userSessionId from PersistentUserSessionEntity u where u.realmId = :realmId and u.offline = :offline)"),
        @NamedQuery(name="findClientSessionsByUserSession", query="select sess from PersistentClientSessionEntity sess where sess.userSessionId=:userSessionId and sess.offline = :offline"),
        @NamedQuery(name="findClientSessionsOrderedByIdInterval", query="select sess from PersistentClientSessionEntity sess where sess.offline = :offline and sess.userSessionId >= :fromSessionId and sess.userSessionId <= :toSessionId order by sess.userSessionId"),
        @NamedQuery(name="findClientSessionsOrderedByIdExact", query="select sess from PersistentClientSessionEntity sess where sess.offline = :offline and sess.userSessionId IN (:userSessionIds)"),
        @NamedQuery(name="findClientSessionsCountByClient", query="select count(sess) from PersistentClientSessionEntity sess where sess.offline = :offline and sess.clientId = :clientId and sess.clientId != 'external'"),
        @NamedQuery(name="findClientSessionsCountByExternalClient", query="select count(sess) from PersistentClientSessionEntity sess where sess.offline = :offline and sess.clientStorageProvider = :clientStorageProvider and sess.externalClientId = :externalClientId and sess.clientStorageProvider != 'internal'"),
        @NamedQuery(name="findClientSessionsByUserSessionAndClient", query="select sess from PersistentClientSessionEntity sess where sess.userSessionId=:userSessionId and sess.offline = :offline and sess.clientId=:clientId and sess.clientId != 'external'"),
        @NamedQuery(name="findClientSessionsByUserSessionAndExternalClient", query="select sess from PersistentClientSessionEntity sess where sess.userSessionId=:userSessionId and sess.offline = :offline and sess.clientStorageProvider = :clientStorageProvider and sess.externalClientId = :externalClientId and sess.clientStorageProvider != 'internal'")
})
@Table(name="OFFLINE_CLIENT_SESSION")
@Entity
@DynamicUpdate
@IdClass(PersistentClientSessionEntity.Key.class)
public class PersistentClientSessionEntity {

    public static final String LOCAL = "local";
    public static final String EXTERNAL = "external";
    @Id
    @Column(name = "USER_SESSION_ID", length = 36)
    protected String userSessionId;

    @Id
    @Column(name="CLIENT_ID", length = 36)
    protected String clientId;

    @Id
    @Column(name="CLIENT_STORAGE_PROVIDER", length = 36)
    protected String clientStorageProvider;

    @Id
    @Column(name="EXTERNAL_CLIENT_ID", length = 255)
    protected String externalClientId;

    @Column(name="TIMESTAMP")
    protected int timestamp;

    @Version
    @Column(name="VERSION")
    private int version;

    @Id
    @Column(name = "OFFLINE_FLAG")
    protected String offline;

    @Column(name="DATA")
    protected String data;

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientStorageProvider() {
        return clientStorageProvider;
    }

    public void setClientStorageProvider(String clientStorageProvider) {
        this.clientStorageProvider = clientStorageProvider;
    }

    public String getExternalClientId() {
        return externalClientId;
    }

    public void setExternalClientId(String externalClientId) {
        this.externalClientId = externalClientId;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getOffline() {
        return offline;
    }

    public void setOffline(String offline) {
        this.offline = offline;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static class Key implements Serializable {

        protected String userSessionId;

        protected String clientId;
        protected String clientStorageProvider;
        protected String externalClientId;

        protected String offline;

        public Key() {
        }

        public Key(String userSessionId, String clientId, String clientStorageProvider, String externalClientId, String offline) {
            this.userSessionId = userSessionId;
            this.clientId = clientId;
            this.externalClientId = externalClientId;
            this.clientStorageProvider = clientStorageProvider;
            this.offline = offline;
        }

        public String getUserSessionId() {
            return userSessionId;
        }

        public String getClientId() {
            return clientId;
        }

        public String getOffline() {
            return offline;
        }

        public String getClientStorageProvider() {
            return clientStorageProvider;
        }

        public String getExternalClientId() {
            return externalClientId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            return Objects.equals(this.userSessionId, key.userSessionId) &&
                    Objects.equals(this.clientId, key.clientId) &&
                    Objects.equals(this.externalClientId, key.externalClientId) &&
                    Objects.equals(this.clientStorageProvider, key.clientStorageProvider) &&
                    Objects.equals(this.offline, key.offline);
        }

        @Override
        public int hashCode() {
            int result = this.userSessionId != null ? this.userSessionId.hashCode() : 0;
            result = 37 * result + (this.clientId != null ? this.clientId.hashCode() : 0);
            result = 37 * result + (this.externalClientId != null ? this.externalClientId.hashCode() : 0);
            result = 37 * result + (this.clientStorageProvider != null ? this.clientStorageProvider.hashCode() : 0);
            result = 31 * result + (this.offline != null ? this.offline.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "PersistentClientSessionEntity$Key[" +
                   "userSessionId='" + userSessionId + '\'' +
                   ", clientId='" + clientId + '\'' +
                   ", clientStorageProvider='" + clientStorageProvider + '\'' +
                   ", externalClientId='" + externalClientId + '\'' +
                   ", offline='" + offline + '\'' +
                   ']';
        }
    }
}
