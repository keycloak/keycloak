package org.keycloak.tests.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
        "org.keycloak.tests.theme",
        "org.keycloak.tests.tracing",
        "org.keycloak.tests.login",
        "org.keycloak.tests.loginfailures",
        "org.keycloak.tests.migration",
        "org.keycloak.tests.i18n",
        "org.keycloak.tests.infinispan",
        "org.keycloak.tests.keys",
        "org.keycloak.tests.transactions",
        "org.keycloak.tests.user",
        "org.keycloak.tests.url",
        "org.keycloak.tests.vault",
        "org.keycloak.tests.welcomepage",
        "org.keycloak.tests.workflow",
        "org.keycloak.tests.x509"
})
public class Base4TestSuite {
}
