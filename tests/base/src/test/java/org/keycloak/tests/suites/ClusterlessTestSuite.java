package org.keycloak.tests.suites;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.keycloak.common.Profile;
import org.keycloak.testframework.cache.InfinispanServer;
import org.keycloak.testframework.injection.SuiteSupport;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.admin.concurrency.ConcurrencyTest;

@Suite
@SelectClasses(ConcurrencyTest.class)
public class ClusterlessTestSuite {

    @BeforeSuite
    public static void beforeSuite() {
        SuiteSupport.startSuite()
                .registerServerConfig(ClusterlessServerConfig.class);
    }

    @AfterSuite
    public static void afterSuite() {
        SuiteSupport.stopSuite();
    }

    public static class ClusterlessServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLUSTERLESS)
                    .featuresDisabled(Profile.Feature.PERSISTENT_USER_SESSIONS)
                    .enableExternalCache(true)
                    .option("cache-remote-host", InfinispanServer.HOST)
                    .option("cache-remote-username", InfinispanServer.USER)
                    .option("cache-remote-password", InfinispanServer.PASSWORD)
                    .option("cache-remote-tls-enabled", "false")
                    .option("spi-cache-embedded-default-site-name", "ISPN")
                    .option("spi-load-balancer-check-remote-poll-interval", "500");
        }
    }
}
