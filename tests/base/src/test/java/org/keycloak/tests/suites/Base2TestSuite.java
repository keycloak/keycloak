package org.keycloak.tests.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
        "org.keycloak.tests.account",
        "org.keycloak.tests.actions",
        "org.keycloak.tests.authz",
        "org.keycloak.tests.broker",
        "org.keycloak.tests.client",
        "org.keycloak.tests.common",
        "org.keycloak.tests.cors",
        "org.keycloak.tests.db",
        "org.keycloak.tests.error",
        "org.keycloak.tests.exportimport",
        "org.keycloak.tests.events",
        "org.keycloak.tests.forms",
        "org.keycloak.tests.i18n",
        "org.keycloak.tests.infinispan",
        "org.keycloak.tests.keys",
        "org.keycloak.tests.login"
})
public class Base2TestSuite {
}
