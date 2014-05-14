package org.keycloak.account.freemarker.model;

import org.keycloak.models.UserSessionModel;
import org.keycloak.util.Time;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SessionsBean {

    private List<UserSessionBean> events;

    public SessionsBean(List<UserSessionModel> sessions) {
        this.events = new LinkedList<UserSessionBean>();
        for (UserSessionModel session : sessions) {
            this.events.add(new UserSessionBean(session));
        }
    }

    public List<UserSessionBean> getSessions() {
        return events;
    }

    public static class UserSessionBean {

        private UserSessionModel session;

        public UserSessionBean(UserSessionModel session) {
            this.session = session;
        }

        public String getIpAddress() {
            return session.getIpAddress();
        }

        public Date getStarted() {
            return Time.toDate(session.getStarted());
        }

        public Date getExpires() {
            return Time.toDate(session.getExpires());
        }

    }

}
