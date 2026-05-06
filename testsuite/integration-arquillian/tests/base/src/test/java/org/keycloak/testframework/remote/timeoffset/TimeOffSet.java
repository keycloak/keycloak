package org.keycloak.testframework.remote.timeoffset;

import java.util.Collections;

import org.keycloak.common.util.Time;
import org.keycloak.testsuite.AbstractKeycloakTest;

public class TimeOffSet {

    private final AbstractKeycloakTest test;

    public TimeOffSet(AbstractKeycloakTest test) {
        this.test = test;
    }

    public void set(int offset) {
        // adminClient depends on Time.offset for auto-refreshing tokens
        Time.setOffset(offset);
        test.getTestingClient().testing().setTimeOffset(Collections.singletonMap("offset", String.valueOf(offset)));

        // force getting new token after time offset has changed
        test.getAdminClient().tokenManager().grantToken();
    }

}
