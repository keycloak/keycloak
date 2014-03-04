package org.keycloak.representations.adapters.action;

/**
 * Query session stats.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserStatsAction extends AdminAction {

    public static final String USER_STATS = "USER_STATS";
    protected String user;

    public UserStatsAction() {
    }

    public UserStatsAction(String id, int expiration, String resource, String user) {
        super(id, expiration, resource, USER_STATS);
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    @Override
    public boolean validate() {
        return USER_STATS.equals(action);
    }

}
