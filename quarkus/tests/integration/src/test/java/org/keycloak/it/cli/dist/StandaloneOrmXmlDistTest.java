package org.keycloak.it.cli.dist;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;

import com.acme.provider.standalone.StandaloneOrmXmlTestProvider;
import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SMOKE)
@TestProvider(StandaloneOrmXmlTestProvider.class)
public class StandaloneOrmXmlDistTest {

    @Test
    @Launch({"start-dev", "--db=dev-file", "--log-level=org.hibernate.orm.jpa:debug,org.keycloak.quarkus.deployment.KeycloakProcessor:debug"})
    void testStandaloneOrmXmlAddedToDefaultPU(CLIResult cliResult) {
        String output = cliResult.getOutput();
        
        String defaultPuBlock = extractPersistenceUnitBlock(output, "<default>");
        assertNotNull(defaultPuBlock, "'<default>' PU info block should be present");
        
        assertTrue(defaultPuBlock.contains("com.acme.provider.standalone.StandaloneEntity"),
                "StandaloneEntity (from standalone orm.xml) must be loaded into '<default>' PU");

        cliResult.assertMessage("Contributing to default persistence unit.");
        cliResult.assertStartedDevMode();
    }

    private static String extractPersistenceUnitBlock(String output, String puName) {
        String marker = "HHH008541: PersistenceUnitInfo [";
        String nameToken = "name: " + puName;
        int idx = 0;
        while ((idx = output.indexOf(marker, idx)) != -1) {
            int nextBlock = output.indexOf(marker, idx + marker.length());
            String block = nextBlock == -1 ? output.substring(idx) : output.substring(idx, nextBlock);
            if (block.contains(nameToken)) {
                return block;
            }
            idx += marker.length();
        }
        return null;
    }
}
