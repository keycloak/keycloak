package org.keycloak.testframework.remote.timeoffset;

import org.keycloak.common.util.Time;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.testsuite.AbstractKeycloakTest;

public class TimeOffSet {

    private final AbstractKeycloakTest test;

    public TimeOffSet(AbstractKeycloakTest test) {
        this.test = test;
    }

    public void set(int offset) {
        test.shouldResetTimeOffset(offset != 0);

        // adminClient depends on Time.offset for auto-refreshing tokens
        Time.setOffset(offset);
        test.getTestingClient().server().run(
                session -> {
                    Time.setOffset(offset);

                    // Time offset was restarted
                    if (offset == 0) {
                        session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
                    }
                }
        );

        // force getting new token after time offset has changed
        test.getAdminClient().tokenManager().grantToken();
    }

}
