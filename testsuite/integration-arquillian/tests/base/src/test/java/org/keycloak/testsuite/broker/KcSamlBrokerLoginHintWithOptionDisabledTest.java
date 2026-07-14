package org.keycloak.testsuite.broker;

import org.junit.Test;

public class KcSamlBrokerLoginHintWithOptionDisabledTest extends AbstractSamlLoginHintTest {
    @Test
    public void testLoginHintSubjectDisabledQueryEnabled() {
        updateLoginHintOptions(false, true);
        assertLoginHintQueryParam(true);
    }

    @Override
    boolean isLoginHintOptionEnabled() {
        return false;
    }

    @Override
    boolean isLoginQueryHintOptionEnabled() {
        return false;
    }
}
