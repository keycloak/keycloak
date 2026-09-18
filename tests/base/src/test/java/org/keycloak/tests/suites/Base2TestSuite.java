package org.keycloak.tests.suites;

import org.keycloak.common.Profile;
import org.keycloak.testframework.injection.SuiteSupport;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.authz.config.DefaultAuthzServerConfig;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
        "org.keycloak.tests.account",
        "org.keycloak.tests.actions",
        "org.keycloak.tests.authz",
        "org.keycloak.tests.broker",
        "org.keycloak.tests.cli",
        "org.keycloak.tests.client",
        "org.keycloak.tests.common",
        "org.keycloak.tests.composites",
        "org.keycloak.tests.cookies",
        "org.keycloak.tests.cors",
        "org.keycloak.tests.db",
        "org.keycloak.tests.error",
        "org.keycloak.tests.exportimport",
        "org.keycloak.tests.events",
        "org.keycloak.tests.forms",
        "org.keycloak.tests.i18n",
        "org.keycloak.tests.infinispan",
        "org.keycloak.tests.keys",
        "org.keycloak.tests.login",
        "org.keycloak.tests.loginfailures",
        "org.keycloak.tests.migration"
})
public class Base2TestSuite {

    @BeforeSuite
    public static void beforeSuite() {
        SuiteSupport.startSuite()
                .registerServerConfig(Base2ServerConfig.class);
    }

    @AfterSuite
    public static void afterSuite() {
        SuiteSupport.stopSuite();
    }

    public static class Base2ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            // AUTHORIZATION is default; omit so AuthorizationDisabledInPreviewTest can disable it.
            return new DefaultAuthzServerConfig().configure(config)
                    .features(Profile.Feature.SCRIPTS)
                    .dependency("org.keycloak.tests", "keycloak-tests-custom-scripts");
        }
    }
}
