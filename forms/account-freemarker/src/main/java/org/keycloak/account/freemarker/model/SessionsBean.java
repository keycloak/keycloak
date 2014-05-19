package org.keycloak.account.freemarker.model;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.util.Time;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SessionsBean {

    private List<UserSessionBean> events;
    private RealmModel realm;

    public SessionsBean(RealmModel realm, List<UserSessionModel> sessions) {
        this.events = new LinkedList<UserSessionBean>();
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

        public String getIpAddress() {
            return session.getIpAddress();
        }

        public Date getStarted() {
            return Time.toDate(session.getStarted());
        }

        public Date getExpires() {
            int max = session.getStarted() + realm.getSsoSessionMaxLifespan();
            return Time.toDate(max);
        }

        public List<String> getApplications() {
            List<String> apps = new ArrayList<String>();
            for (ClientModel client : session.getClientAssociations()) {
                if (client instanceof ApplicationModel) apps.add(client.getClientId());
            }
            return apps;
        }
        public List<String> getClients() {
            List<String> apps = new ArrayList<String>();
            for (ClientModel client : session.getClientAssociations()) {
                if (client instanceof OAuthClientModel) apps.add(client.getClientId());
            }
            return apps;
        }

    }

}
