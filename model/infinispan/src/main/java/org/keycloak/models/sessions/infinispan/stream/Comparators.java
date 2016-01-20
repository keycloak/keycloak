package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.UserSessionTimestamp;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Comparators {

    public static Comparator<UserSessionTimestamp> userSessionTimestamp() {
        return new UserSessionTimestampComparator();
    }

    private static class UserSessionTimestampComparator implements Comparator<UserSessionTimestamp>, Serializable {
        @Override
        public int compare(UserSessionTimestamp u1, UserSessionTimestamp u2) {
            return u1.getClientSessionTimestamp() - u2.getClientSessionTimestamp();
        }
    }

}
