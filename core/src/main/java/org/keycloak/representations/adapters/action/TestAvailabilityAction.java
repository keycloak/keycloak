package org.keycloak.representations.adapters.action;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestAvailabilityAction extends AdminAction {

    public static final String TEST_AVAILABILITY = "TEST_AVAILABILITY";

    public TestAvailabilityAction() {
    }

    public TestAvailabilityAction(String id, int expiration, String resource) {
        super(id, expiration, resource, TEST_AVAILABILITY);
    }

    @Override
    public boolean validate() {
        return TEST_AVAILABILITY.equals(action);
    }

}
