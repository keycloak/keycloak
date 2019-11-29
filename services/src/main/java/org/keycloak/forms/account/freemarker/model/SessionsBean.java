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

package org.keycloak.forms.account.freemarker.model;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SessionsBean {

    private List<UserSessionBean> events;
    private RealmModel realm;

    public SessionsBean(RealmModel realm, List<UserSessionModel> sessions) {
        this.events = new LinkedList<>();
        for (UserSessionModel session : sessions) {
            this.events.add(new UserSessionBean(realm, session));
        }
    }

    public List<UserSessionBean> getSessions() {
        return events;
    }

    public static class UserSessionBean {

        private UserSessionModel session;
        private RealmModel realm;

        public UserSessionBean(RealmModel realm, UserSessionModel session) {
            this.realm = realm;
            this.session = session;
        }

        public String getId() {return session.getId(); }

        public String getIpAddress() {
            return session.getIpAddress();
        }

        public Date getStarted() {
            return Time.toDate(session.getStarted());
        }

        public Date getLastAccess() {
            return Time.toDate(session.getLastSessionRefresh());
        }

        public Date getExpires() {
            int maxLifespan = session.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0 ? realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan();
            int max = session.getStarted() + maxLifespan;
            return Time.toDate(max);
        }

        public Set<String> getClients() {
            Set<String> clients = new HashSet<>();
            for (String clientUUID : session.getAuthenticatedClientSessions().keySet()) {
                ClientModel client = realm.getClientById(clientUUID);
                clients.add(client.getClientId());
            }
            return clients;
        }
    }

}
