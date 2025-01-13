package org.keycloak.testframework.events;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SysLogTest {

    String logEntry = "<14>1 2024-08-21T08:14:33.591+02:00 fedora keycloak 17377 org.keycloak.category - \uFEFFSome log message";

    @Test
    public void testParseLog() {
        SysLog sysLog = SysLog.parse(logEntry);

        Assertions.assertNotNull(sysLog.getTimestamp());
        Assertions.assertEquals("fedora", sysLog.getHostname());
        Assertions.assertEquals("keycloak", sysLog.getAppName());
        Assertions.assertEquals("org.keycloak.category", sysLog.getCategory());
        Assertions.assertEquals("Some log message", sysLog.getMessage());
    }

}
