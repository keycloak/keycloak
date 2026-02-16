package org.keycloak.tests.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
        "org.keycloak.tests.client",
        "org.keycloak.tests.common",
        "org.keycloak.tests.cors",
        "org.keycloak.tests.db",
        "org.keycloak.tests.forms",
        "org.keycloak.tests.i18n",
        "org.keycloak.tests.infinispan",
        "org.keycloak.tests.keys",
        "org.keycloak.tests.model",
        "org.keycloak.tests.oauth",
        "org.keycloak.tests.tracing",
        "org.keycloak.tests.transactions",
        "org.keycloak.tests.welcomepage",
        "org.keycloak.tests.workflow"
})
public class Base2TestSuite {
}
