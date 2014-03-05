package org.keycloak.representations.adapters.action;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LogoutAction extends AdminAction {
    public static final String LOGOUT = "LOGOUT";
    protected String user;

    public LogoutAction() {
    }

    public LogoutAction(String id, int expiration, String resource, String user) {
        super(id, expiration, resource, LOGOUT);
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public boolean validate() {
        return LOGOUT.equals(action);
    }
}
