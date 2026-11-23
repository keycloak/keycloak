package org.keycloak.it.cli.dist;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.utils.RawKeycloakDistribution;

import com.acme.provider.singlenamed.SingleNamedPuTestProvider;
import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SMOKE)
public class SingleNamedPuOrmXmlDistTest {

    @Test
    @TestProvider(SingleNamedPuTestProvider.class)
    @Launch({"start-dev", "--db=dev-file", "--db-kind-single-named-store=dev-mem", "--log-level=org.hibernate.orm.jpa:debug,org.keycloak.quarkus.deployment.KeycloakProcessor:debug"})
    void testSingleNamedPuWithOrmXmlLoaded(CLIResult cliResult) {
        String output = cliResult.getOutput();

        String singlePuBlock = extractPersistenceUnitBlock(output, "single-named-store");
        assertNotNull(singlePuBlock, "'single-named-store' PU info block should be present");

        assertTrue(singlePuBlock.contains("com.acme.provider.singlenamed.SingleNamedPuEntity"),
                "SingleNamedPuEntity (from implicit orm.xml in single-PU archive) must be loaded into 'single-named-store' PU");
        assertTrue(singlePuBlock.contains("hibernate.cache.use_second_level_cache: false"));
        assertTrue(singlePuBlock.contains("hibernate.cache.use_query_cache: false"));

        String defaultPuBlock = extractPersistenceUnitBlock(output, "<default>");
        if (defaultPuBlock != null) {
            assertFalse(defaultPuBlock.contains("com.acme.provider.singlenamed.SingleNamedPuEntity"),
                    "SingleNamedPuEntity must NOT leak into '<default>' PU");
        }

        cliResult.assertStartedDevMode();
    }

    @Test
    @TestProvider(SingleNamedPuTestProvider.CacheEnabled.class)
    void cachingCanBeEnabledInCustomPersistenceUnit(KeycloakRunner runner) {
        var distribution = runner.getDistribution(RawKeycloakDistribution.class);
        installCachingDependencies(distribution);

        var result = runner.run("start-dev", "--db=dev-file", "--db-kind-single-named-store=dev-mem",
                "--log-level=org.hibernate.orm.jpa:debug,org.hibernate.orm.cache:debug");
        assertCachingEnabled(result);
    }

    @Test
    @TestProvider(SingleNamedPuTestProvider.class)
    void customPersistenceUnitInheritsCachingConfiguration(KeycloakRunner runner) {
        var distribution = runner.getDistribution(RawKeycloakDistribution.class);
        installCachingDependencies(distribution);
        distribution.setQuarkusProperty("quarkus.hibernate-orm.second-level-caching-enabled", "true");

        var result = runner.run("start-dev", "--db=dev-file", "--db-kind-single-named-store=dev-mem",
                "--log-level=org.hibernate.orm.jpa:debug,org.hibernate.orm.cache:debug");
        assertCachingEnabled(result);
    }

    @Test
    @TestProvider(SingleNamedPuTestProvider.CacheDisabled.class)
    void cachingCanBeDisabledInCustomPersistenceUnit(KeycloakRunner runner) {
        var distribution = runner.getDistribution(RawKeycloakDistribution.class);
        installCachingDependencies(distribution);
        distribution.setQuarkusProperty("quarkus.hibernate-orm.second-level-caching-enabled", "true");

        var result = runner.run("start-dev", "--db=dev-file", "--db-kind-single-named-store=dev-mem",
                "--log-level=org.hibernate.orm.jpa:debug");
        result.assertStartedDevMode();

        String singlePuBlock = extractPersistenceUnitBlock(result.getOutput(), "single-named-store");
        assertNotNull(singlePuBlock, "'single-named-store' PU info block should be present");
        assertTrue(singlePuBlock.contains("hibernate.cache.use_second_level_cache: false"));
        assertTrue(singlePuBlock.contains("hibernate.cache.use_query_cache: false"));
    }

    private static void installCachingDependencies(RawKeycloakDistribution distribution) {
        distribution.copyProvider("org.hibernate.orm", "hibernate-jcache");
        distribution.copyProvider("com.github.ben-manes.caffeine", "jcache");
        distribution.copyProvider("javax.cache", "cache-api");
        distribution.copyProvider("com.typesafe", "config");
    }

    private static void assertCachingEnabled(CLIResult result) {
        result.assertStartedDevMode();

        String singlePuBlock = extractPersistenceUnitBlock(result.getOutput(), "single-named-store");
        assertNotNull(singlePuBlock, "'single-named-store' PU info block should be present");
        assertTrue(singlePuBlock.contains("hibernate.cache.use_second_level_cache: true"));
        assertTrue(singlePuBlock.contains("hibernate.cache.use_query_cache: true"));
        result.assertMessage("Missing cache region [default-update-timestamps-region]");
        result.assertMessage("Missing cache region [default-query-results-region]");
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
