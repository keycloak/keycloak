package org.keycloak.representations.adapters.action;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PushNotBeforeAction extends AdminAction {

    public static final String PUSH_NOT_BEFORE = "PUSH_NOT_BEFORE";
    protected int notBefore;

    public PushNotBeforeAction() {
    }

    public PushNotBeforeAction(String id, int expiration, String resource, int notBefore) {
        super(id, expiration, resource, PUSH_NOT_BEFORE);
        this.notBefore = notBefore;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    @Override
    public boolean validate() {
        return PUSH_NOT_BEFORE.equals(action);
    }

}
