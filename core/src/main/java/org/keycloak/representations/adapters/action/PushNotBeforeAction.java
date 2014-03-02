package org.keycloak.representations.adapters.action;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PushNotBeforeAction extends AdminAction {

    protected int notBefore;

    public PushNotBeforeAction() {
    }

    public PushNotBeforeAction(String id, int expiration, String resource, int notBefore) {
        super(id, expiration, resource);
        this.notBefore = notBefore;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }
}
