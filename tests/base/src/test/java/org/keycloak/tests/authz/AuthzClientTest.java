package org.keycloak.tests.authz;

import java.io.ByteArrayInputStream;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AuthzClientTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @Test
    public void testCreateWithEnvVars() throws Throwable {
        EnvironmentVariables envVars = new EnvironmentVariables()
                .set("KEYCLOAK_REALM", "test")
                .set("KEYCLOAK_AUTH_SERVER", "http://test");

        Statement statement = envVars.apply(new Statement() {
            @Override
            public void evaluate() {
                RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class, () -> {
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
                });

                MatcherAssert.assertThat(runtimeException.getMessage(), Matchers.containsString("Could not obtain configuration from server"));
            }
        }, Description.createTestDescription(AuthzClientTest.class, "testCreateWithEnvVars"));

        statement.evaluate();
    }
}
