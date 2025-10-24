package org.keycloak.tests.suites;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.CryptoProviderOption;
import org.keycloak.testframework.injection.SuiteSupport;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.admin.ServerInfoTest;

@Suite
@SelectClasses(ServerInfoTest.class)
public class FipsStrictTestSuite {

    @BeforeSuite
    public static void beforeSuite() {
        SuiteSupport.startSuite()
                .registerServerConfig(FipsStrictServerConfig.class)
                .registerSupplierConfig("certificates", FipsStrictCertificatesConfig.class);
    }

    @AfterSuite
    public static void afterSuite() {
        SuiteSupport.stopSuite();
    }

    public static class FipsStrictServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.FIPS).tlsEnabled(true)
                .option("fips-mode", "strict")
                .option("spi-password-hashing-pbkdf2-max-padding-length", "14")
                .option("spi-password-hashing-pbkdf2-sha256-max-padding-length", "14")
                .option("spi-password-hashing-pbkdf2-sha512-max-padding-length", "14")
                .dependency("org.bouncycastle", "bc-fips")
                .dependency("org.bouncycastle", "bctls-fips")
                .dependency("org.bouncycastle", "bcpkix-fips")
                .dependency("org.bouncycastle", "bcutil-fips");
        }
    }

    public static class FipsStrictCertificatesConfig implements CertificatesConfig {

        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.cryptoProviderOption(CryptoProviderOption.FIPS_STRICT)
                    .keystoreFormat(KeystoreUtil.KeystoreFormat.BCFKS);
        }
    }
}
