package org.keycloak.representations.adapters.action;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LogoutAction extends AdminAction {
    public static final String LOGOUT = "LOGOUT";
    protected String user;
    protected int notBefore;

    public LogoutAction() {
    }

    public LogoutAction(String id, int expiration, String resource, String user, int notBefore) {
        super(id, expiration, resource, LOGOUT);
        this.user = user;
        this.notBefore = notBefore;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    @Override
    public boolean validate() {
        return LOGOUT.equals(action);
    }
}
