import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
package org.keycloak.tests.broker;

@KeycloakIntegrationTest
public class KcSamlBrokerLoginHintWithOptionDisabledTest extends AbstractSamlLoginHintTest {

    @InjectRealm
    ManagedRealm managedRealm;
    @Override
    boolean isLoginHintOptionEnabled() {
        return false;
    }
}
