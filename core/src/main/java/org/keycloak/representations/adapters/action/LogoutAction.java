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
    protected List<String> keycloakSessionIds;

    public LogoutAction() {
    }

    public LogoutAction(String id, int expiration, String resource, List<String> adapterSessionIds, int notBefore, List<String> keycloakSessionIds) {
        super(id, expiration, resource, LOGOUT);
        this.adapterSessionIds = adapterSessionIds;
        this.notBefore = notBefore;
        this.keycloakSessionIds = keycloakSessionIds;
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

    public List<String> getKeycloakSessionIds() {
        return keycloakSessionIds;
    }

    public void setKeycloakSessionIds(List<String> keycloakSessionIds) {
        this.keycloakSessionIds = keycloakSessionIds;
    }

    @Override
    public boolean validate() {
        return LOGOUT.equals(action);
    }
}
