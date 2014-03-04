package org.keycloak.representations.adapters.action;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserStats {
    protected boolean loggedIn;
    protected long whenLoggedIn;

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public long getWhenLoggedIn() {
        return whenLoggedIn;
    }

    public void setWhenLoggedIn(long whenLoggedIn) {
        this.whenLoggedIn = whenLoggedIn;
    }
}
