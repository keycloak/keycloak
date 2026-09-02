package org.keycloak.testframework.events;

import org.keycloak.events.Event;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventParserTest {

    String nonEvent = "<14>1 2024-08-21T08:14:33.591+02:00 fedora keycloak 17377 org.keycloak.category - \uFEFF2024-08-21 08:14:33,591 INFO  [org.keycloak.events] (executor-thread-12) type=\"LOGIN\"";
    String loginEvent = "<14>1 2024-08-21T08:14:33.591+02:00 fedora keycloak 17377 org.keycloak.events - \uFEFF2024-08-21 08:14:33,591 INFO  [org.keycloak.events] (executor-thread-12) type=\"LOGIN\", realmId=\"c4730c14-e66f-4372-89df-9910e769f3b9\", realmName=\"master\", clientId=\"security-admin-console\", userId=\"c89c95c1-633a-4116-b68c-1b78aa27c556\", sessionId=\"3186fe6a-5e85-4d0d-b517-2835369db2b9\", ipAddress=\"0:0:0:0:0:0:0:1\", auth_method=\"openid-connect\", auth_type=\"code\", response_type=\"code\", redirect_uri=\"http://localhost:8080/admin/master/console/#/master/users/c89c95c1-633a-4116-b68c-1b78aa27c556/settings\", consent=\"no_consent_required\", code_id=\"3186fe6a-5e85-4d0d-b517-2835369db2b9\", response_mode=\"query\", username=\"admin\", authSessionParentId=\"3186fe6a-5e85-4d0d-b517-2835369db2b9\", authSessionTabId=\"iL5xM2eOOcc\"";
    String adminEvent = "<14>1 2024-08-21T08:15:13.688+02:00 fedora keycloak 17377 org.keycloak.events - \uFEFF2024-08-21 08:15:13,688 INFO  [org.keycloak.events] (executor-thread-18) operationType=\"UPDATE\", realmId=\"c4730c14-e66f-4372-89df-9910e769f3b9\", realmName=\"master\", clientId=\"0a7da22d-b13e-4696-9526-3e2cde55a64c\", userId=\"c89c95c1-633a-4116-b68c-1b78aa27c556\", ipAddress=\"0:0:0:0:0:0:0:1\", resourceType=\"USER\", resourcePath=\"users/c89c95c1-633a-4116-b68c-1b78aa27c556\"";

    @Test
    public void testParseLoginEvent() {
        Event event = EventParser.parse(SysLog.parse(loginEvent));
        Assertions.assertEquals("security-admin-console", event.getClientId());
    }

    @Test
    public void testParseNonEvent() {
        Assertions.assertNull(EventParser.parse(SysLog.parse(nonEvent)));
    }

    @Test
    public void testParseAdminEvent() {
        Assertions.assertNull(EventParser.parse(SysLog.parse(adminEvent)));
    }

}
