package org.keycloak.testframework.server;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KeycloakServerConfigBuilderTest {

    @Test
    public void addOptionCombinesListValues() {
        KeycloakServerConfigBuilder builder = KeycloakServerConfigBuilder.startDev()
                .option("truststore-paths", "custom.pem")
                .addOption("truststore-paths", "managed.pem");

        List<String> args = builder.toArgs();
        Assertions.assertEquals(1, args.stream().filter(a -> a.startsWith("--truststore-paths=")).count());
        Assertions.assertTrue(args.contains("--truststore-paths=custom.pem,managed.pem"));
    }

    @Test
    public void quoteWindowsBatchArgument() {
        Assertions.assertEquals("\"--truststore-paths=C:\\custom.pem,C:\\managed.pem\"",
                DistributionKeycloakServer.quoteWindowsBatchArgument("--truststore-paths=C:\\custom.pem,C:\\managed.pem"));
        Assertions.assertEquals("--http-enabled=true", DistributionKeycloakServer.quoteWindowsBatchArgument("--http-enabled=true"));
    }
}
