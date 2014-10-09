package org.keycloak.representations.adapters.action;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LogoutAction extends AdminAction {
    public static final String LOGOUT = "LOGOUT";
    protected List<String> adapterSessionIds;
    protected int notBefore;

    public LogoutAction() {
    }

    public LogoutAction(String id, int expiration, String resource, List<String> adapterSessionIds, int notBefore) {
        super(id, expiration, resource, LOGOUT);
        this.adapterSessionIds = adapterSessionIds;
        this.notBefore = notBefore;
    }


    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    public List<String> getAdapterSessionIds() {
        return adapterSessionIds;
    }

    @Override
    public boolean validate() {
        return LOGOUT.equals(action);
    }
}
