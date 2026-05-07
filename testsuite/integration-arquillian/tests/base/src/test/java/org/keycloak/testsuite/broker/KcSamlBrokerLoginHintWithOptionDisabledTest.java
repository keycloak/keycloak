package org.keycloak.testsuite.broker;

public class KcSamlBrokerLoginHintWithOptionDisabledTest extends AbstractSamlLoginHintTest {
    @Override
    boolean isLoginHintOptionEnabled() {
        return false;
    }

    @Override
    boolean isLoginQueryHintOptionEnabled() {
        return false;
    }
}
