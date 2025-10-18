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
public class FipsNonStrictTestSuite {

    private static final String ROOT_DIR = "tests/base/target/kc-tests/../conf/";

    @BeforeSuite
    public static void beforeSuite() {
        SuiteSupport.startSuite()
                .registerServerConfig(FipsNonStrictServerConfig.class);
    }

    @AfterSuite
    public static void afterSuite() {
        SuiteSupport.stopSuite();
    }

    public static class FipsNonStrictServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.FIPS)
                .option("-Dserver.fips.mode", "non-strict")
                .option("-Dserver.supported.keystore.types", "PKCS12")
                .option("-Dserver.keystore", ROOT_DIR + "keycloak-fips.keystore.pkcs12")
                .option("-Dserver.keystore.password", "passwordpassword")
                .option("-Dserver.truststore", ROOT_DIR + "keycloak-fips.truststore.pkcs12")
                .option("-Dserver.truststore.password", "passwordpassword")
                .option("-Dserver.java.security.file", ROOT_DIR +  "kc.java.security")
                .option("-Dserver.kerberos.supported", "false")
                .configFile("/org/keycloak/tests/suites/fips/kc.java.security")
                .configFile("/org/keycloak/tests/suites/fips/kc.keystore-create.java.security")
                .configFile("/org/keycloak/tests/suites/fips/keycloak-fips.keystore.pkcs12")
                .configFile("/org/keycloak/tests/suites/fips/keycloak-fips.truststore.pkcs12")
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
