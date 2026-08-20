package org.keycloak.tests.broker;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public class KcSamlBrokerLoginHintWithOptionDisabledTest extends AbstractSamlLoginHintTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @Override
    boolean isLoginHintOptionEnabled() {
        return false;
    }
}
