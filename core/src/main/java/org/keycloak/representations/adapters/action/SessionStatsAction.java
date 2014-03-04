package org.keycloak.representations.adapters.action;

/**
 * Query session stats.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SessionStatsAction extends AdminAction {

    public static final String SESSION_STATS = "SESSION_STATS";

    protected boolean listUsers;

    public SessionStatsAction() {
    }

    public SessionStatsAction(String id, int expiration, String resource) {
        super(id, expiration, resource, SESSION_STATS);
    }

    public boolean isListUsers() {
        return listUsers;
    }

    public void setListUsers(boolean listUsers) {
        this.listUsers = listUsers;
    }

    @Override
    public boolean validate() {
        return SESSION_STATS.equals(action);
    }

}
