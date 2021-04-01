package org.keycloak.testsuite.authz;

import java.io.ByteArrayInputStream;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExpectedException;
import org.keycloak.authorization.client.AuthzClient;

public class AuthzClientTest {

    @Rule
    public final EnvironmentVariables envVars = new EnvironmentVariables();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testCreateWithEnvVars() {
        envVars.set("KEYCLOAK_REALM", "test");
        envVars.set("KEYCLOAK_AUTH_SERVER", "http://test");

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(Matchers.containsString("Could not obtain configuration from server"));

        AuthzClient.create(new ByteArrayInputStream(("{\n"
                + "  \"realm\": \"${env.KEYCLOAK_REALM}\",\n"
                + "  \"auth-server-url\": \"${env.KEYCLOAK_AUTH_SERVER}\",\n"
                + "  \"ssl-required\": \"external\",\n"
                + "  \"enable-cors\": true,\n"
                + "  \"resource\": \"my-server\",\n"
                + "  \"credentials\": {\n"
                + "    \"secret\": \"${env.KEYCLOAK_SECRET}\"\n"
                + "  },\n"
                + "  \"confidential-port\": 0,\n"
                + "  \"policy-enforcer\": {\n"
                + "    \"enforcement-mode\": \"ENFORCING\"\n"
                + "  }\n"
                + "}").getBytes()));
    }
}
