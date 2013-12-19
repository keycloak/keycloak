package org.keycloak.representations.adapters.action;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LogoutAction extends AdminAction {
    protected String user;

    public LogoutAction() {
    }

    public LogoutAction(String id, long expiration, String resource, String user) {
        super(id, expiration, resource);
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
