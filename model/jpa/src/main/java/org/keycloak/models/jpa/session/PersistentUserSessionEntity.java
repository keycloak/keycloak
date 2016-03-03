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
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.keycloak.models.jpa.entities.UserEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteUserSessionsByRealm", query="delete from PersistentUserSessionEntity sess where sess.realmId = :realmId"),
        @NamedQuery(name="deleteUserSessionsByUser", query="delete from PersistentUserSessionEntity sess where sess.userId = :userId"),
        @NamedQuery(name="deleteDetachedUserSessions", query="delete from PersistentUserSessionEntity sess where sess.userSessionId NOT IN (select c.userSessionId from PersistentClientSessionEntity c)"),
        @NamedQuery(name="findUserSessionsCount", query="select count(sess) from PersistentUserSessionEntity sess where sess.offline = :offline"),
        @NamedQuery(name="findUserSessions", query="select sess from PersistentUserSessionEntity sess where sess.offline = :offline order by sess.userSessionId"),
        @NamedQuery(name="updateUserSessionsTimestamps", query="update PersistentUserSessionEntity c set lastSessionRefresh = :lastSessionRefresh"),

})
@Table(name="OFFLINE_USER_SESSION")
@Entity
@IdClass(PersistentUserSessionEntity.Key.class)
public class PersistentUserSessionEntity {

    @Id
    @Column(name="USER_SESSION_ID", length = 36)
    protected String userSessionId;

    @Column(name = "REALM_ID", length = 36)
    protected String realmId;

    @Column(name="USER_ID", length = 36)
    protected String userId;

    @Column(name = "LAST_SESSION_REFRESH")
    protected int lastSessionRefresh;

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

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        this.lastSessionRefresh = lastSessionRefresh;
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

        protected String offline;

        public Key() {
        }

        public Key(String userSessionId, String offline) {
            this.userSessionId = userSessionId;
            this.offline = offline;
        }

        public String getUserSessionId() {
            return userSessionId;
        }

        public String getOffline() {
            return offline;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (this.userSessionId != null ? !this.userSessionId.equals(key.userSessionId) : key.userSessionId != null) return false;
            if (this.offline != null ? !this.offline.equals(key.offline) : key.offline != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = this.userSessionId != null ? this.userSessionId.hashCode() : 0;
            result = 31 * result + (this.offline != null ? this.offline.hashCode() : 0);
            return result;
        }
    }
}
