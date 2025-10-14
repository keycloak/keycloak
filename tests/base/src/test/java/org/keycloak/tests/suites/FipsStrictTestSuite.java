package org.keycloak.tests.suites;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.keycloak.common.Profile;
import org.keycloak.testframework.injection.SuiteSupport;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.admin.AdminHeadersTest;

@Suite
@SelectClasses(AdminHeadersTest.class)
public class FipsStrictTestSuite {

    private static final String ROOT_DIR = "tests/base/kc-tests/../conf/";

    @BeforeSuite
    public static void beforeSuite() {
        SuiteSupport.startSuite()
                .registerServerConfig(FipsStrictServerConfig.class);
    }

    @AfterSuite
    public static void afterSuite() {
        SuiteSupport.stopSuite();
    }

    public static class FipsStrictServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.FIPS)
                .option("-Dserver.fips.mode", "strict")
                .option("-Dserver.supported.keystore.types", "BCFKS")
                .option("-Dserver.keystore", ROOT_DIR + "keycloak-fips.keystore.bcfks")
                .option("-Dserver.keystore.password", "passwordpassword")
                .option("-Dserver.truststore", ROOT_DIR + "keycloak-fips.truststore.bcfks")
                .option("-Dserver.truststore.password", "passwordpassword")
                .option("-Dserver.java.security.file", ROOT_DIR + "kc.java.security")
                .option("-Dserver.supported.rsa.key.sizes", "2048,3072,4096")
                .option("-Dserver.kerberos.supported", "false")
                .configFile("/org/keycloak/tests/suites/fips/kc.java.security")
                .configFile("/org/keycloak/tests/suites/fips/kc.keystore-create.java.security")
                .configFile("/org/keycloak/tests/suites/fips/keycloak-fips.keystore.bcfks")
                .configFile("/org/keycloak/tests/suites/fips/keycloak-fips.truststore.bcfks")
                .dependency("org.bouncycastle", "bc-fips")
                .dependency("org.bouncycastle", "bctls-fips")
                .dependency("org.bouncycastle", "bcpkix-fips")
                .dependency("org.bouncycastle", "bcutil-fips")
                .clientDependency("org.bouncycastle", "bc-fips")
                .clientDependency("org.bouncycastle", "bctls-fips")
                .clientDependency("org.bouncycastle", "bcutil-fips");
        }
    }
}
