package org.keycloak.tests.suites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
        "org.keycloak.tests.model",
        "org.keycloak.tests.oauth",
        "org.keycloak.tests.organization",
        "org.keycloak.tests.oid4vc",
        "org.keycloak.tests.policy",
        "org.keycloak.tests.saml",
        "org.keycloak.tests.securityprofile",
        "org.keycloak.tests.session",
        "org.keycloak.tests.sessionlimits",
        "org.keycloak.tests.ssl",
        "org.keycloak.tests.tracing",
        "org.keycloak.tests.transactions",
        "org.keycloak.tests.url",
        "org.keycloak.tests.vault",
        "org.keycloak.tests.welcomepage",
        "org.keycloak.tests.workflow"
})
public class Base3TestSuite {
}
